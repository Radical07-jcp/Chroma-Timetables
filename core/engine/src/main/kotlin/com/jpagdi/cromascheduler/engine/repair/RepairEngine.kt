package com.jpagdi.cromascheduler.engine.repair

import com.jpagdi.cromascheduler.engine.SchedulingInput
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithm
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithmRegistry
import com.jpagdi.cromascheduler.engine.coloring.ColoringSupport
import com.jpagdi.cromascheduler.engine.graph.GraphBuilder
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.engine.rooms.RoomAssigner
import com.jpagdi.cromascheduler.engine.validation.ConstraintValidator
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import com.jpagdi.cromascheduler.engine.validation.ValidationContext

data class RepairResult(
    val preservedSessionIds: Set<String>,
    val recoloredSessionIds: Set<String>,
    val assignments: Map<String, Timeslot>,
    val roomBySession: Map<String, String>,
    val unresolvedSessionIds: List<String>,
    val remainingViolations: List<ConstraintViolation>, // non-empty means repair couldn't fully resolve everything — surface to the user, don't silently ship it
)

/**
 * "Conflict Repair" mode from the spec: import an existing timetable, detect
 * conflicts, preserve valid sessions whenever possible, recalculate only the
 * conflicting ones. This is the whole reason ColoringAlgorithm grew a
 * [ColoringAlgorithm.fixedAssignments] parameter in Phase 4 — repair is just a
 * normal coloring run where "valid so far" sessions are pinned in place.
 */
object RepairEngine {
    fun repair(
        input: SchedulingInput,
        existingAssignments: Map<String, Timeslot>,
        existingRoomBySession: Map<String, String>,
        algorithm: ColoringAlgorithm = ColoringAlgorithmRegistry.default,
    ): RepairResult {
        // Step 1: find what's actually broken in the schedule as given.
        val initialViolations = ConstraintValidator.validate(
            ValidationContext(
                sessions = input.sessions,
                assignments = existingAssignments,
                roomBySession = existingRoomBySession,
                rooms = input.rooms,
                sectionStudentCounts = input.sectionStudentCounts,
                blockedTeacherSlots = input.blockedTeacherSlots,
                blockedRoomSlots = input.blockedRoomSlots,
                definedPeriodsByDay = input.definedPeriodsByDay,
            ),
        )

        val conflictingSessionIds = initialViolations
            .flatMap { listOfNotNull(it.sessionAId, it.sessionBId) }
            .toSet()
        // Anything the schedule never even assigned a timeslot to also needs (re)coloring.
        val neverAssigned = input.sessions.map { it.id }.filter { it !in existingAssignments }.toSet()
        val toRecolor = conflictingSessionIds + neverAssigned
        val preserved = existingAssignments.keys - toRecolor

        // Step 2: recolor only the broken/missing sessions, with everything else pinned.
        val fixedAssignments = existingAssignments.filterKeys { it in preserved }
        val graph = GraphBuilder.buildConflictGraph(input.sessions)
        val coloring = algorithm.color(graph, input.availableTimeslotsBySession, fixedAssignments)

        // Step 3: rooms — preserved sessions keep their existing room; only the
        // recolored subset needs a fresh room decision, seeded with preserved
        // sessions' occupied periods so it can't double-book them.
        val preOccupiedByRoom = mutableMapOf<String, MutableSet<Timeslot>>()
        preserved.forEach { sessionId ->
            val roomId = existingRoomBySession[sessionId] ?: return@forEach
            val session = input.sessions.find { it.id == sessionId } ?: return@forEach
            val start = existingAssignments[sessionId] ?: return@forEach
            preOccupiedByRoom.getOrPut(roomId) { mutableSetOf() }.addAll(ColoringSupport.occupiedRun(session, start))
        }
        val sessionsToRoom = input.sessions.filter { it.id in toRecolor }
        val roomResult = RoomAssigner.assignRooms(
            sessionsToAssign = sessionsToRoom,
            assignments = coloring.assignments,
            rooms = input.rooms,
            sectionStudentCounts = input.sectionStudentCounts,
            roomAvailability = input.blockedRoomSlots,
            preOccupiedByRoom = preOccupiedByRoom,
        )
        val finalRoomBySession = existingRoomBySession.filterKeys { it in preserved } + roomResult.roomBySession

        // Step 4: re-validate the repaired result — if something is still unresolved
        // (e.g. a genuinely unsatisfiable session), that has to be surfaced, not hidden.
        val remainingViolations = ConstraintValidator.validate(
            ValidationContext(
                sessions = input.sessions,
                assignments = coloring.assignments,
                roomBySession = finalRoomBySession,
                rooms = input.rooms,
                sectionStudentCounts = input.sectionStudentCounts,
                blockedTeacherSlots = input.blockedTeacherSlots,
                blockedRoomSlots = input.blockedRoomSlots,
                definedPeriodsByDay = input.definedPeriodsByDay,
            ),
        )

        return RepairResult(
            preservedSessionIds = preserved,
            recoloredSessionIds = toRecolor,
            assignments = coloring.assignments,
            roomBySession = finalRoomBySession,
            unresolvedSessionIds = coloring.unresolvedSessionIds,
            remainingViolations = remainingViolations,
        )
    }
}
