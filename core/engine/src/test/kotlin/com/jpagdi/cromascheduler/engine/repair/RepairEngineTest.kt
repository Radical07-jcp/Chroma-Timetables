package com.jpagdi.cromascheduler.engine.repair

import com.jpagdi.cromascheduler.engine.SchedulingInput
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.SessionType
import com.jpagdi.cromascheduler.engine.model.Timeslot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepairEngineTest {

    private val weekPool = (1..5).flatMap { day -> (0..7).map { period -> Timeslot(day, period) } }

    private fun session(id: String, teacherId: String? = null) =
        EngineSession(id, SessionType.CLASS, "SUBJ", teacherId, null)

    @Test
    fun `preserves a session with no conflict and only recolors the conflicting pair`() {
        val sessions = listOf(
            session("VALID", teacherId = "T3"),
            session("CONFLICT_A", teacherId = "T1"),
            session("CONFLICT_B", teacherId = "T1"),
        )
        val input = SchedulingInput(
            sessions = sessions,
            availableTimeslotsBySession = sessions.associate { it.id to weekPool },
            rooms = emptyList(),
            sectionStudentCounts = emptyMap(),
            blockedTeacherSlots = emptyMap(),
            blockedRoomSlots = emptyMap(),
            definedPeriodsByDay = mapOf(1 to (0..7).toSet()),
        )
        // CONFLICT_A and CONFLICT_B illegally share the exact same slot; VALID sits elsewhere.
        val existingAssignments = mapOf(
            "VALID" to Timeslot(1, 5),
            "CONFLICT_A" to Timeslot(1, 0),
            "CONFLICT_B" to Timeslot(1, 0),
        )

        val result = RepairEngine.repair(input, existingAssignments, emptyMap())

        assertEquals(Timeslot(1, 5), result.assignments["VALID"], "The untouched valid session's timeslot must not change")
        assertTrue("VALID" in result.preservedSessionIds)
        assertTrue("CONFLICT_A" in result.recoloredSessionIds)
        assertTrue("CONFLICT_B" in result.recoloredSessionIds)
        assertTrue(result.assignments.getValue("CONFLICT_A") != result.assignments.getValue("CONFLICT_B"), "Repair must resolve the double-booking")
        assertTrue(result.remainingViolations.isEmpty())
    }
    @Test
    fun `keeps explicit manual placement fixed while repairing its conflicting neighbor`() {
        val sessions = listOf(
            session("MANUAL", teacherId = "T1"),
            session("NEIGHBOR", teacherId = "T1"),
        )
        val input = SchedulingInput(
            sessions = sessions,
            availableTimeslotsBySession = sessions.associate { it.id to weekPool },
            rooms = emptyList(),
            sectionStudentCounts = emptyMap(),
            blockedTeacherSlots = emptyMap(),
            blockedRoomSlots = emptyMap(),
            definedPeriodsByDay = mapOf(1 to (0..7).toSet()),
        )
        val manualPlacement = Timeslot(1, 0)
        val existingAssignments = mapOf(
            "MANUAL" to manualPlacement,
            "NEIGHBOR" to manualPlacement,
        )

        val result = RepairEngine.repair(
            input = input,
            existingAssignments = existingAssignments,
            existingRoomBySession = emptyMap(),
            fixedSessionIds = setOf("MANUAL"),
        )

        assertEquals(manualPlacement, result.assignments["MANUAL"], "Explicit user placement must survive repair")
        assertTrue(result.assignments.getValue("NEIGHBOR") != manualPlacement, "Repair should move the conflicting neighbor")
        assertTrue(result.remainingViolations.isEmpty())
    }

    @Test
    fun `scoped repair can move only selected sessions while frozen timetable remains unchanged`() {
        val sessions = listOf(
            session("SELECTED_A", teacherId = "T1"),
            session("SELECTED_B", teacherId = "T1"),
            session("FROZEN", teacherId = "T2"),
        )
        val input = SchedulingInput(
            sessions = sessions,
            availableTimeslotsBySession = sessions.associate { it.id to weekPool },
            rooms = emptyList(),
            sectionStudentCounts = emptyMap(),
            blockedTeacherSlots = emptyMap(),
            blockedRoomSlots = emptyMap(),
            definedPeriodsByDay = mapOf(1 to (0..7).toSet()),
        )
        val frozenPlacement = Timeslot(1, 5)
        val existingAssignments = mapOf(
            "SELECTED_A" to Timeslot(1, 0),
            "SELECTED_B" to Timeslot(1, 0),
            "FROZEN" to frozenPlacement,
        )

        val result = RepairEngine.repair(
            input = input,
            existingAssignments = existingAssignments,
            existingRoomBySession = emptyMap(),
            selectedSessionIds = setOf("SELECTED_A", "SELECTED_B"),
            fixedSessionIds = setOf("FROZEN"),
        )

        assertEquals(frozenPlacement, result.assignments["FROZEN"], "A frozen outside-scope session must never move")
        assertTrue(result.assignments.getValue("SELECTED_A") != result.assignments.getValue("SELECTED_B"), "Selected scope must be repairable")
        assertTrue(result.remainingViolations.isEmpty())
    }

}
