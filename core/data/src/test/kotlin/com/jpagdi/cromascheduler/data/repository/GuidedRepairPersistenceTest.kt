package com.jpagdi.cromascheduler.data.repository

import com.jpagdi.cromascheduler.data.entity.ScheduleAssignmentEntity
import com.jpagdi.cromascheduler.engine.model.Timeslot
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuidedRepairPersistenceTest {
    @Test
    fun `clean swap working draft must persist the exact swapped timetable`() {
        val working = mapOf("A" to Timeslot(1, 3), "B" to Timeslot(1, 1))
        val rooms = mapOf<String, String?>("A" to "ROOM_B", "B" to "ROOM_A")
        val persisted = listOf(
            ScheduleAssignmentEntity("RUN", "A", 1, 3, "ROOM_B"),
            ScheduleAssignmentEntity("RUN", "B", 1, 1, "ROOM_A"),
        )
        assertTrue(guidedRepairPersistenceMatches(persisted, working, rooms))
    }

    @Test
    fun `persistence invariant rejects the original timetable after a clean swap`() {
        val working = mapOf("A" to Timeslot(1, 3), "B" to Timeslot(1, 1))
        val rooms = mapOf<String, String?>("A" to "ROOM_B", "B" to "ROOM_A")
        val staleOriginal = listOf(
            ScheduleAssignmentEntity("RUN", "A", 1, 1, "ROOM_A"),
            ScheduleAssignmentEntity("RUN", "B", 1, 3, "ROOM_B"),
        )
        assertFalse(guidedRepairPersistenceMatches(staleOriginal, working, rooms))
    }

    @Test
    fun `persistence invariant rejects missing or extra sessions`() {
        val working = mapOf("A" to Timeslot(1, 3), "B" to Timeslot(1, 1))
        val rooms = mapOf<String, String?>("A" to "ROOM_B", "B" to "ROOM_A")
        val missing = listOf(ScheduleAssignmentEntity("RUN", "A", 1, 3, "ROOM_B"))
        val extra = listOf(
            ScheduleAssignmentEntity("RUN", "A", 1, 3, "ROOM_B"),
            ScheduleAssignmentEntity("RUN", "B", 1, 1, "ROOM_A"),
            ScheduleAssignmentEntity("RUN", "C", 1, 2, "ROOM_C"),
        )
        assertFalse(guidedRepairPersistenceMatches(missing, working, rooms))
        assertFalse(guidedRepairPersistenceMatches(extra, working, rooms))
    }
}
