package com.jpagdi.cromascheduler.data.repository

import androidx.room.withTransaction
import com.jpagdi.cromascheduler.data.db.CromaDatabase
import com.jpagdi.cromascheduler.data.entity.AvailabilityEntityType
import com.jpagdi.cromascheduler.data.entity.ConflictRecordEntity
import com.jpagdi.cromascheduler.data.entity.PeriodConfigEntity
import com.jpagdi.cromascheduler.data.entity.ScheduleAssignmentEntity
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.entity.SessionEntity
import com.jpagdi.cromascheduler.data.export.ScheduleExportRow
import com.jpagdi.cromascheduler.data.timeslot.TimeslotGenerator
import com.jpagdi.cromascheduler.engine.SchedulingEngine
import com.jpagdi.cromascheduler.engine.SchedulingInput
import com.jpagdi.cromascheduler.engine.SchedulingOutput
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithmRegistry
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.engine.optimize.OptimizationResult
import com.jpagdi.cromascheduler.engine.optimize.ScheduleOptimizer
import com.jpagdi.cromascheduler.engine.repair.RepairEngine
import com.jpagdi.cromascheduler.engine.repair.RepairResult
import com.jpagdi.cromascheduler.engine.rooms.EngineRoom
import com.jpagdi.cromascheduler.engine.validation.ConstraintValidator
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import com.jpagdi.cromascheduler.engine.validation.ValidationContext
import java.util.UUID

object ScheduleMode {
    const val GENERATE = "GENERATE"
    const val GENERATE_EXAM = "GENERATE_EXAM"
    const val REPAIR = "REPAIR"
    const val OPTIMIZE = "OPTIMIZE"
}

/**
 * The single place that assembles a SchedulingInput from whatever's currently in
 * Room and turns engine output back into persisted rows. Every "mode" the spec
 * lists (Generate / Generate Exam / Validate / Repair / Optimize) goes through the
 * SAME underlying engine calls — the only thing that differs between them is which
 * sessions get selected and which engine entry point gets called, exactly as the
 * spec's Application Modes section requires ("The same mathematical scheduling
 * engine should power every mode").
 */
class ScheduleRepository(private val database: CromaDatabase) {

    suspend fun buildSchedulingInput(sessionFilter: (SessionEntity) -> Boolean = { true }): SchedulingInput {
        val teachers = database.teacherDao().getAll()
        val rooms = database.roomDao().getAll()
        val sections = database.sectionDao().getAll()
        val sessions = database.sessionDao().getAll().filter(sessionFilter)
        val timeslots = database.timeslotDao().getAll()

        val engineSessions = sessions.toEngineSessions()
        val engineRooms = rooms.map { EngineRoom(it.id, it.capacity, it.type) }
        val sectionCounts = sections.associate { it.id to it.studentCount }

        val blockedTeacher = teachers.associate { teacher ->
            teacher.id to database.availabilityDao()
                .getBlocksFor(AvailabilityEntityType.TEACHER, teacher.id)
                .map { Timeslot(it.dayOfWeek, it.periodIndex) }
                .toSet()
        }
        val blockedRoom = rooms.associate { room ->
            room.id to database.availabilityDao()
                .getBlocksFor(AvailabilityEntityType.ROOM, room.id)
                .map { Timeslot(it.dayOfWeek, it.periodIndex) }
                .toSet()
        }

        val allTimeslots = timeslots.map { Timeslot(it.dayOfWeek, it.periodIndex) }
        val definedPeriodsByDay = timeslots.groupBy { it.dayOfWeek }.mapValues { (_, v) -> v.map { it.periodIndex }.toSet() }

        // Each session's candidate pool = every defined timeslot minus its own
        // teacher's blocked periods. Room availability is intentionally NOT
        // filtered here — see Phase 1's ConflictGraph doc comment on why room
        // conflicts are resolved in a separate pass (RoomAssigner), not baked
        // into the timeslot pool used for coloring.
        val availableBySession = engineSessions.associate { session ->
            val blocked = session.teacherId?.let { blockedTeacher[it] }.orEmpty()
            session.id to allTimeslots.filter { it !in blocked }
        }

        return SchedulingInput(
            sessions = engineSessions,
            availableTimeslotsBySession = availableBySession,
            rooms = engineRooms,
            sectionStudentCounts = sectionCounts,
            blockedTeacherSlots = blockedTeacher,
            blockedRoomSlots = blockedRoom,
            definedPeriodsByDay = definedPeriodsByDay,
        )
    }

    /** Generate New Schedule / Generate Examination Schedule — [sessionFilter] is the only difference between the two modes. */
    suspend fun generate(
        name: String,
        mode: String,
        algorithmName: String = ColoringAlgorithmRegistry.default.name,
        sessionFilter: (SessionEntity) -> Boolean = { true },
    ): String {
        val input = buildSchedulingInput(sessionFilter)
        val algorithm = ColoringAlgorithmRegistry.algorithms[algorithmName] ?: ColoringAlgorithmRegistry.default
        val startTime = System.currentTimeMillis()
        val output = SchedulingEngine.generate(input, algorithm)
        val elapsed = System.currentTimeMillis() - startTime

        val runId = UUID.randomUUID().toString()
        val run = ScheduleRunEntity(runId, name, System.currentTimeMillis(), algorithm.name, mode, elapsed)
        persistRun(run, output.assignments, output.roomBySession, output.violations)
        return runId
    }

    /** Validate Existing Schedule — re-checks a saved run's current assignments and refreshes its conflict records. */
    suspend fun validate(runId: String): List<ConstraintViolation> {
        val input = buildSchedulingInput()
        val savedAssignments = database.scheduleRunDao().getAssignmentsFor(runId)
        val assignments = savedAssignments.associate { it.sessionId to Timeslot(it.dayOfWeek, it.periodIndex) }
        val roomBySession = savedAssignments.mapNotNull { a -> a.roomId?.let { a.sessionId to it } }.toMap()

        val violations = ConstraintValidator.validate(
            ValidationContext(
                sessions = input.sessions,
                assignments = assignments,
                roomBySession = roomBySession,
                rooms = input.rooms,
                sectionStudentCounts = input.sectionStudentCounts,
                blockedTeacherSlots = input.blockedTeacherSlots,
                blockedRoomSlots = input.blockedRoomSlots,
                definedPeriodsByDay = input.definedPeriodsByDay,
            ),
        )

        database.withTransaction {
            persistConflicts(runId, violations)
        }
        return violations
    }

    /**
     * Repair Conflicting Schedule — produces a NEW schedule run (mode REPAIR)
     * rather than mutating the original, so the "before" state stays available
     * for comparison/undo. [sourceRunId] is validated fresh (not read from
     * possibly-stale conflict records) so repair always acts on the schedule's
     * true current state.
     */
    suspend fun repair(sourceRunId: String, algorithmName: String = ColoringAlgorithmRegistry.default.name): String {
        val input = buildSchedulingInput()
        val savedAssignments = database.scheduleRunDao().getAssignmentsFor(sourceRunId)
        val existingAssignments = savedAssignments.associate { it.sessionId to Timeslot(it.dayOfWeek, it.periodIndex) }
        val existingRoomBySession = savedAssignments.mapNotNull { a -> a.roomId?.let { a.sessionId to it } }.toMap()
        val algorithm = ColoringAlgorithmRegistry.algorithms[algorithmName] ?: ColoringAlgorithmRegistry.default

        val startTime = System.currentTimeMillis()
        val result: RepairResult = RepairEngine.repair(input, existingAssignments, existingRoomBySession, algorithm)
        val elapsed = System.currentTimeMillis() - startTime

        val sourceRun = database.scheduleRunDao().getAll().find { it.id == sourceRunId }
        val runId = UUID.randomUUID().toString()
        val run = ScheduleRunEntity(
            id = runId,
            name = "${sourceRun?.name ?: "Schedule"} (repaired)",
            createdAtEpochMillis = System.currentTimeMillis(),
            algorithmUsed = algorithm.name,
            mode = ScheduleMode.REPAIR,
            executionTimeMillis = elapsed,
        )
        persistRun(run, result.assignments, result.roomBySession, result.remainingViolations)
        return runId
    }

    /** Optimize Existing Schedule — also produces a new run, same reasoning as repair(). */
    suspend fun optimize(sourceRunId: String, maxPasses: Int = 3): String {
        val input = buildSchedulingInput()
        val savedAssignments = database.scheduleRunDao().getAssignmentsFor(sourceRunId)
        val assignments = savedAssignments.associate { it.sessionId to Timeslot(it.dayOfWeek, it.periodIndex) }
        val roomBySession = savedAssignments.mapNotNull { a -> a.roomId?.let { a.sessionId to it } }.toMap()
        val totalDefinedPeriods = input.definedPeriodsByDay.values.sumOf { it.size }

        val startTime = System.currentTimeMillis()
        val result: OptimizationResult = ScheduleOptimizer.optimize(
            input, assignments, roomBySession, input.rooms.size, totalDefinedPeriods, maxPasses,
        )
        val elapsed = System.currentTimeMillis() - startTime

        val violations = ConstraintValidator.validate(
            ValidationContext(
                sessions = input.sessions,
                assignments = result.assignments,
                roomBySession = result.roomBySession,
                rooms = input.rooms,
                sectionStudentCounts = input.sectionStudentCounts,
                blockedTeacherSlots = input.blockedTeacherSlots,
                blockedRoomSlots = input.blockedRoomSlots,
                definedPeriodsByDay = input.definedPeriodsByDay,
            ),
        )

        val sourceRun = database.scheduleRunDao().getAll().find { it.id == sourceRunId }
        val runId = UUID.randomUUID().toString()
        val run = ScheduleRunEntity(
            id = runId,
            name = "${sourceRun?.name ?: "Schedule"} (optimized)",
            createdAtEpochMillis = System.currentTimeMillis(),
            algorithmUsed = "optimize",
            mode = ScheduleMode.OPTIMIZE,
            executionTimeMillis = elapsed,
        )
        persistRun(run, result.assignments, result.roomBySession, violations)
        return runId
    }

    suspend fun getRuns(): List<ScheduleRunEntity> = database.scheduleRunDao().getAll()

    suspend fun getTeachers(): List<com.jpagdi.cromascheduler.data.entity.TeacherEntity> = database.teacherDao().getAll()

    data class HomeCounts(val teachers: Int, val subjects: Int, val rooms: Int, val sessions: Int)

    suspend fun getHomeCounts(): HomeCounts = HomeCounts(
        teachers = database.teacherDao().getAll().size,
        subjects = database.subjectDao().getAll().size,
        rooms = database.roomDao().getAll().size,
        sessions = database.sessionDao().getAll().size,
    )

    /** A teacher's currently blocked (unavailable) periods, for rendering the toggle grid. */
    suspend fun getTeacherBlockedSlots(teacherId: String): Set<Timeslot> =
        database.availabilityDao().getBlocksFor(AvailabilityEntityType.TEACHER, teacherId)
            .map { Timeslot(it.dayOfWeek, it.periodIndex) }
            .toSet()

    /** In-app equivalent of a row in availability.csv — lets a teacher mark/unmark a single period without a fresh CSV import. */
    suspend fun setTeacherBlocked(teacherId: String, day: Int, period: Int, blocked: Boolean) {
        if (blocked) {
            database.availabilityDao().upsert(
                com.jpagdi.cromascheduler.data.entity.AvailabilityBlockEntity(AvailabilityEntityType.TEACHER, teacherId, day, period),
            )
        } else {
            database.availabilityDao().deleteBlock(AvailabilityEntityType.TEACHER, teacherId, day, period)
        }
    }

    /** Current period configuration, or the built-in default (60-min periods, Mon-Fri, 8/day) if none has been set yet. */
    suspend fun getPeriodConfig(): PeriodConfigEntity = database.periodConfigDao().get() ?: PeriodConfigEntity.DEFAULT

    /**
     * Saves a new period configuration and regenerates the entire timeslot grid
     * from it. This intentionally REPLACES all timeslots rather than diffing —
     * changing period length invalidates every existing timeslot's meaning
     * (period 3 at 45 minutes and period 3 at 60 minutes are not the same slot),
     * so a partial merge would be actively misleading. Any schedule runs
     * generated before a regeneration keep their saved (dayOfWeek, periodIndex)
     * assignments, but re-running Validate against them afterward may surface
     * new DURATION_EXCEEDS_AVAILABLE_PERIODS violations if the new grid is
     * shorter — that's correct behavior, not a bug, and is exactly what
     * Validate mode is for.
     */
    suspend fun savePeriodConfigAndRegenerate(config: PeriodConfigEntity) {
        val timeslots = TimeslotGenerator.generate(config)
        database.withTransaction {
            database.periodConfigDao().upsert(config)
            database.timeslotDao().clear()
            database.timeslotDao().upsertAll(timeslots)
        }
    }

    suspend fun getAssignments(runId: String): List<ScheduleAssignmentEntity> = database.scheduleRunDao().getAssignmentsFor(runId)

    suspend fun getConflicts(runId: String): List<ConflictRecordEntity> = database.scheduleRunDao().getConflictsFor(runId)

    /**
     * Joins a run's raw assignments against every name lookup (subject/teacher/
     * section/room/timeslot) needed for display. Shared by the Results screen and
     * every export format — building this join once here means both stay
     * consistent instead of each screen independently deciding how to render an
     * unassigned room or a missing timeslot definition.
     */
    suspend fun buildExportRows(runId: String): List<ScheduleExportRow> {
        val assignments = database.scheduleRunDao().getAssignmentsFor(runId)
            .sortedWith(compareBy({ it.dayOfWeek }, { it.periodIndex }))
        val sessions = database.sessionDao().getAll().associateBy { it.id }
        val subjects = database.subjectDao().getAll().associateBy { it.id }
        val teachers = database.teacherDao().getAll().associateBy { it.id }
        val sections = database.sectionDao().getAll().associateBy { it.id }
        val rooms = database.roomDao().getAll().associateBy { it.id }
        val timeslots = database.timeslotDao().getAll().associateBy { it.dayOfWeek to it.periodIndex }

        return assignments.mapNotNull { a ->
            val session = sessions[a.sessionId] ?: return@mapNotNull null
            val timeslot = timeslots[a.dayOfWeek to a.periodIndex]
            ScheduleExportRow(
                sessionId = session.id,
                sessionType = session.type.name,
                subjectName = session.subjectId?.let { subjects[it]?.name } ?: "—",
                teacherName = session.teacherId?.let { teachers[it]?.name } ?: "—",
                sectionName = session.sectionId?.let { sections[it]?.name } ?: "—",
                roomName = a.roomId?.let { rooms[it]?.name } ?: "Unassigned",
                dayLabel = DAY_NAMES[a.dayOfWeek] ?: "Day ${a.dayOfWeek}",
                startTime = timeslot?.startTime ?: "Period ${a.periodIndex}",
                endTime = timeslot?.endTime.orEmpty(),
            )
        }
    }

    companion object {
        // dayOfWeek follows java.time.DayOfWeek.getValue() convention: 1=Monday .. 7=Sunday.
        private val DAY_NAMES = mapOf(
            1 to "Monday", 2 to "Tuesday", 3 to "Wednesday", 4 to "Thursday",
            5 to "Friday", 6 to "Saturday", 7 to "Sunday",
        )
    }

    private suspend fun persistRun(
        run: ScheduleRunEntity,
        assignments: Map<String, Timeslot>,
        roomBySession: Map<String, String>,
        violations: List<ConstraintViolation>,
    ) {
        database.withTransaction {
            database.scheduleRunDao().upsert(run)
            val assignmentEntities = assignments.map { (sessionId, ts) ->
                ScheduleAssignmentEntity(run.id, sessionId, ts.dayOfWeek, ts.periodIndex, roomBySession[sessionId])
            }
            database.scheduleRunDao().upsertAssignments(assignmentEntities)
            persistConflicts(run.id, violations)
        }
    }

    private suspend fun persistConflicts(runId: String, violations: List<ConstraintViolation>) {
        val entities = violations.map {
            ConflictRecordEntity(
                scheduleRunId = runId,
                sessionAId = it.sessionAId,
                sessionBId = it.sessionBId,
                conflictType = it.type.name,
                reason = it.message,
            )
        }
        database.scheduleRunDao().upsertConflicts(entities)
    }
}
