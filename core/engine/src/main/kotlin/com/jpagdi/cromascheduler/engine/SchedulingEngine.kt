package com.jpagdi.cromascheduler.engine

import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithm
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithmRegistry
import com.jpagdi.cromascheduler.engine.graph.GraphBuilder
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.engine.rooms.EngineRoom
import com.jpagdi.cromascheduler.engine.rooms.RoomAssigner
import com.jpagdi.cromascheduler.engine.validation.ConstraintValidator
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import com.jpagdi.cromascheduler.engine.validation.ValidationContext

/**
 * Everything the engine needs to generate a schedule, gathered in one place. The
 * repository layer (:core:data) is responsible for building this from Room entities —
 * see SessionMapper.kt for the one place entity <-> engine model conversion happens.
 */
data class SchedulingInput(
    val sessions: List<EngineSession>,
    val availableTimeslotsBySession: Map<String, List<Timeslot>>,
    val rooms: List<EngineRoom>,
    val sectionStudentCounts: Map<String, Int>,
    val blockedTeacherSlots: Map<String, Set<Timeslot>>,
    val blockedRoomSlots: Map<String, Set<Timeslot>>,
    val definedPeriodsByDay: Map<Int, Set<Int>>,
)

data class SchedulingOutput(
    val assignments: Map<String, Timeslot>,
    val roomBySession: Map<String, String>,
    val unresolvedSessionIds: List<String>, // couldn't be colored at all
    val unroomedSessionIds: List<String>, // colored, but no suitable room found
    val violations: List<ConstraintViolation>, // should normally be empty — see ConstraintValidator's "final sanity check" note
)

/**
 * "Generate New Schedule" and "Generate Examination Schedule" modes both call this —
 * the only difference between them is which sessions get passed in (EXAM-type vs
 * everything else), decided by the caller, not by this function.
 */
object SchedulingEngine {
    fun generate(
        input: SchedulingInput,
        algorithm: ColoringAlgorithm = ColoringAlgorithmRegistry.default,
    ): SchedulingOutput {
        val graph = GraphBuilder.buildConflictGraph(input.sessions)
        val coloring = algorithm.color(graph, input.availableTimeslotsBySession)

        val roomResult = RoomAssigner.assignRooms(
            sessionsToAssign = input.sessions,
            assignments = coloring.assignments,
            rooms = input.rooms,
            sectionStudentCounts = input.sectionStudentCounts,
            roomAvailability = input.blockedRoomSlots,
        )

        val violations = ConstraintValidator.validate(
            ValidationContext(
                sessions = input.sessions,
                assignments = coloring.assignments,
                roomBySession = roomResult.roomBySession,
                rooms = input.rooms,
                sectionStudentCounts = input.sectionStudentCounts,
                blockedTeacherSlots = input.blockedTeacherSlots,
                blockedRoomSlots = input.blockedRoomSlots,
                definedPeriodsByDay = input.definedPeriodsByDay,
            ),
        )

        return SchedulingOutput(
            assignments = coloring.assignments,
            roomBySession = roomResult.roomBySession,
            unresolvedSessionIds = coloring.unresolvedSessionIds,
            unroomedSessionIds = roomResult.unassignedSessionIds,
            violations = violations,
        )
    }
}
