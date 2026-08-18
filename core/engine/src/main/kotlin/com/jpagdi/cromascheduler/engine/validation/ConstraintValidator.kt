package com.jpagdi.cromascheduler.engine.validation

import com.jpagdi.cromascheduler.engine.coloring.ColoringSupport
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.engine.rooms.EngineRoom

/**
 * Everything needed to fully validate a schedule. Bundled into one class rather than
 * six separate parameters because ConstraintValidator, RepairEngine, and
 * ScheduleOptimizer (Phase 6) all need the same bundle — keeping it as one type means
 * they stay in sync instead of three call sites independently assembling overlapping
 * argument lists.
 */
data class ValidationContext(
    val sessions: List<EngineSession>,
    val assignments: Map<String, Timeslot>, // sessionId -> start
    val roomBySession: Map<String, String>, // sessionId -> roomId, may be a subset of sessions
    val rooms: List<EngineRoom>,
    val sectionStudentCounts: Map<String, Int>,
    val blockedTeacherSlots: Map<String, Set<Timeslot>>,
    val blockedRoomSlots: Map<String, Set<Timeslot>>,
    /** The full defined period grid per day (e.g. 1..8 for an 8-period day) — used only to check a session's duration doesn't run past the end of the day. */
    val definedPeriodsByDay: Map<Int, Set<Int>>,
)

/**
 * Validates a fully-formed schedule against every HARD constraint listed in the
 * spec's Constraint Validation section. Soft constraints (idle time, room changes,
 * compactness, morning preference, room utilization) are scored separately by
 * ScheduleOptimizer (Phase 6) — they inform quality, never a pass/fail violation here.
 *
 * Used by: Validate Existing Schedule mode directly; Repair mode to find which
 * sessions need recoloring; Generate mode as a final sanity check on its own output
 * (coloring + room assignment are constructed to avoid these by design, but validating
 * the result anyway catches any gap between "designed to avoid" and "actually does").
 */
object ConstraintValidator {

    fun validate(context: ValidationContext): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val sessionById = context.sessions.associateBy { it.id }
        val occupied = mutableMapOf<String, Set<Timeslot>>()

        for (session in context.sessions) {
            val start = context.assignments[session.id] ?: continue // unassigned sessions are reported by the coloring/repair step, not here
            val dayPeriods = context.definedPeriodsByDay[start.dayOfWeek].orEmpty()
            val run = start.periodIndex until (start.periodIndex + session.durationPeriods)
            if (!run.all { it in dayPeriods }) {
                violations += ConstraintViolation(
                    ConstraintViolationType.DURATION_EXCEEDS_AVAILABLE_PERIODS,
                    session.id,
                    message = "Session \"${session.id}\" (duration ${session.durationPeriods}) doesn't fit within day ${start.dayOfWeek}'s defined periods starting at period ${start.periodIndex}",
                )
                continue // no valid occupied run to check further constraints against
            }
            occupied[session.id] = ColoringSupport.occupiedRun(session, start)
        }

        checkSharedResourceDoubleBooking(context.sessions, occupied, ConstraintViolationType.TEACHER_DOUBLE_BOOKED, violations) { it.teacherId }
        checkSharedResourceDoubleBooking(context.sessions, occupied, ConstraintViolationType.SECTION_DOUBLE_BOOKED, violations) { it.sectionId }
        checkSharedResourceDoubleBooking(context.sessions, occupied, ConstraintViolationType.SUBJECT_DOUBLE_BOOKED, violations) { it.subjectId }
        checkRoomDoubleBooking(context, occupied, violations)
        checkAvailability(context, occupied, sessionById, violations)
        checkRoomCapacity(context, violations)

        return violations
    }

    private fun checkSharedResourceDoubleBooking(
        sessions: List<EngineSession>,
        occupied: Map<String, Set<Timeslot>>,
        type: ConstraintViolationType,
        violations: MutableList<ConstraintViolation>,
        resourceIdOf: (EngineSession) -> String?,
    ) {
        val groups = sessions.filter { resourceIdOf(it) != null }.groupBy { resourceIdOf(it)!! }
        groups.values.forEach { group ->
            for (i in group.indices) {
                for (j in i + 1 until group.size) {
                    val a = group[i]; val b = group[j]
                    val occA = occupied[a.id] ?: continue
                    val occB = occupied[b.id] ?: continue
                    if (occA.intersect(occB).isNotEmpty()) {
                        violations += ConstraintViolation(
                            type, a.id, b.id,
                            message = "Sessions \"${a.id}\" and \"${b.id}\" overlap on ${describe(type)} \"${resourceIdOf(a)}\"",
                        )
                    }
                }
            }
        }
    }

    private fun checkRoomDoubleBooking(
        context: ValidationContext,
        occupied: Map<String, Set<Timeslot>>,
        violations: MutableList<ConstraintViolation>,
    ) {
        val byRoom = context.roomBySession.entries.groupBy({ it.value }, { it.key })
        byRoom.values.forEach { sessionIds ->
            for (i in sessionIds.indices) {
                for (j in i + 1 until sessionIds.size) {
                    val idA = sessionIds[i]; val idB = sessionIds[j]
                    val occA = occupied[idA] ?: continue
                    val occB = occupied[idB] ?: continue
                    if (occA.intersect(occB).isNotEmpty()) {
                        violations += ConstraintViolation(
                            ConstraintViolationType.ROOM_DOUBLE_BOOKED, idA, idB,
                            message = "Sessions \"$idA\" and \"$idB\" are both booked in room \"${context.roomBySession[idA]}\" at an overlapping time",
                        )
                    }
                }
            }
        }
    }

    private fun checkAvailability(
        context: ValidationContext,
        occupied: Map<String, Set<Timeslot>>,
        sessionById: Map<String, EngineSession>,
        violations: MutableList<ConstraintViolation>,
    ) {
        for ((sessionId, occ) in occupied) {
            val session = sessionById[sessionId] ?: continue
            session.teacherId?.let { teacherId ->
                val blocked = context.blockedTeacherSlots[teacherId].orEmpty()
                if (occ.intersect(blocked).isNotEmpty()) {
                    violations += ConstraintViolation(
                        ConstraintViolationType.TEACHER_UNAVAILABLE, sessionId,
                        message = "Session \"$sessionId\" is scheduled during a period teacher \"$teacherId\" marked unavailable",
                    )
                }
            }
            context.roomBySession[sessionId]?.let { roomId ->
                val blocked = context.blockedRoomSlots[roomId].orEmpty()
                if (occ.intersect(blocked).isNotEmpty()) {
                    violations += ConstraintViolation(
                        ConstraintViolationType.ROOM_UNAVAILABLE, sessionId,
                        message = "Session \"$sessionId\" is scheduled during a period room \"$roomId\" is marked unavailable",
                    )
                }
            }
        }
    }

    private fun checkRoomCapacity(context: ValidationContext, violations: MutableList<ConstraintViolation>) {
        val roomsById = context.rooms.associateBy { it.id }
        for (session in context.sessions) {
            val sectionId = session.sectionId ?: continue
            val roomId = context.roomBySession[session.id] ?: continue
            val room = roomsById[roomId] ?: continue
            val studentCount = context.sectionStudentCounts[sectionId] ?: continue
            if (studentCount > room.capacity) {
                violations += ConstraintViolation(
                    ConstraintViolationType.ROOM_CAPACITY_EXCEEDED, session.id,
                    message = "Session \"${session.id}\" has $studentCount students but room \"$roomId\" capacity is ${room.capacity}",
                )
            }
        }
    }

    private fun describe(type: ConstraintViolationType) = when (type) {
        ConstraintViolationType.TEACHER_DOUBLE_BOOKED -> "teacher"
        ConstraintViolationType.SECTION_DOUBLE_BOOKED -> "section"
        else -> "resource"
    }
}
