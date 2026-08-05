package com.jpagdi.cromascheduler.engine.validation

import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.SessionType
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.engine.rooms.EngineRoom
import kotlin.test.Test
import kotlin.test.assertTrue

class ConstraintValidatorTest {

    private fun session(id: String, teacherId: String? = null, sectionId: String? = null, duration: Int = 1) =
        EngineSession(id, SessionType.CLASS, "SUBJ", teacherId, sectionId, durationPeriods = duration)

    private val definedPeriods = mapOf(1 to (0..7).toSet(), 2 to (0..7).toSet())

    @Test
    fun `flags two sessions sharing a teacher at overlapping times`() {
        val sessions = listOf(session("S1", teacherId = "T1"), session("S2", teacherId = "T1"))
        val assignments = mapOf("S1" to Timeslot(1, 0), "S2" to Timeslot(1, 0))
        val violations = ConstraintValidator.validate(
            ValidationContext(sessions, assignments, emptyMap(), emptyList(), emptyMap(), emptyMap(), emptyMap(), definedPeriods),
        )
        assertTrue(violations.any { it.type == ConstraintViolationType.TEACHER_DOUBLE_BOOKED })
    }

    @Test
    fun `flags two sessions in the same room at overlapping times`() {
        val sessions = listOf(session("S1"), session("S2"))
        val assignments = mapOf("S1" to Timeslot(1, 0), "S2" to Timeslot(1, 0))
        val roomBySession = mapOf("S1" to "R1", "S2" to "R1")
        val violations = ConstraintValidator.validate(
            ValidationContext(sessions, assignments, roomBySession, emptyList(), emptyMap(), emptyMap(), emptyMap(), definedPeriods),
        )
        assertTrue(violations.any { it.type == ConstraintViolationType.ROOM_DOUBLE_BOOKED })
    }

    @Test
    fun `flags a session scheduled during a teacher's blocked period`() {
        val sessions = listOf(session("S1", teacherId = "T1"))
        val assignments = mapOf("S1" to Timeslot(1, 0))
        val blockedTeacher = mapOf("T1" to setOf(Timeslot(1, 0)))
        val violations = ConstraintValidator.validate(
            ValidationContext(sessions, assignments, emptyMap(), emptyList(), emptyMap(), blockedTeacher, emptyMap(), definedPeriods),
        )
        assertTrue(violations.any { it.type == ConstraintViolationType.TEACHER_UNAVAILABLE })
    }

    @Test
    fun `flags a section that exceeds its assigned room's capacity`() {
        val sessions = listOf(session("S1", sectionId = "SEC1"))
        val assignments = mapOf("S1" to Timeslot(1, 0))
        val roomBySession = mapOf("S1" to "R1")
        val rooms = listOf(EngineRoom("R1", capacity = 20, type = "regular"))
        val sectionCounts = mapOf("SEC1" to 35)
        val violations = ConstraintValidator.validate(
            ValidationContext(sessions, assignments, roomBySession, rooms, sectionCounts, emptyMap(), emptyMap(), definedPeriods),
        )
        assertTrue(violations.any { it.type == ConstraintViolationType.ROOM_CAPACITY_EXCEEDED })
    }

    @Test
    fun `flags a session whose duration runs past the end of the defined day`() {
        val sessions = listOf(session("S1", duration = 3))
        val assignments = mapOf("S1" to Timeslot(1, 6)) // periods 6,7,8 — day only defines 0..7
        val violations = ConstraintValidator.validate(
            ValidationContext(sessions, assignments, emptyMap(), emptyList(), emptyMap(), emptyMap(), emptyMap(), definedPeriods),
        )
        assertTrue(violations.any { it.type == ConstraintViolationType.DURATION_EXCEEDS_AVAILABLE_PERIODS })
    }

    @Test
    fun `a fully valid schedule produces no violations`() {
        val sessions = listOf(session("S1", teacherId = "T1", sectionId = "SEC1"), session("S2", teacherId = "T2", sectionId = "SEC2"))
        val assignments = mapOf("S1" to Timeslot(1, 0), "S2" to Timeslot(1, 1))
        val roomBySession = mapOf("S1" to "R1", "S2" to "R2")
        val rooms = listOf(EngineRoom("R1", 30, "regular"), EngineRoom("R2", 30, "regular"))
        val sectionCounts = mapOf("SEC1" to 25, "SEC2" to 25)
        val violations = ConstraintValidator.validate(
            ValidationContext(sessions, assignments, roomBySession, rooms, sectionCounts, emptyMap(), emptyMap(), definedPeriods),
        )
        assertTrue(violations.isEmpty())
    }
}
