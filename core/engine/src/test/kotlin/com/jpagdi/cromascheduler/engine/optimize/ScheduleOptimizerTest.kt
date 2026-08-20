package com.jpagdi.cromascheduler.engine.optimize

import com.jpagdi.cromascheduler.engine.SchedulingInput
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.SessionType
import com.jpagdi.cromascheduler.engine.model.Timeslot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleOptimizerTest {

    private fun session(id: String, teacherId: String, sectionId: String) =
        EngineSession(id, SessionType.CLASS, "SUBJ", teacherId, sectionId)

    /**
     * S1 and S2 share teacher T1 and are both booked into slot (1,0) — a real conflict.
     * S1 is pinned (its available-timeslots pool has only its current slot), so it can never
     * be the one that moves. S2's only alternative slot, (1,1), is already held by S3 (teacher
     * T2, but sharing S2's section) — a lone move there just trades the teacher conflict for a
     * section conflict, no net improvement. The only way to actually clear the conflict is a
     * SWAP: S2 <-> S3. S3 is never itself conflicted, so it's not in the focus set the
     * repository derives from the flagged violation (S1, S2) — this is exactly the case the
     * swap-eligibility fix covers. (Without pinning S1, the optimizer's own single-move pass
     * would just move S1 out of the way instead — a perfectly valid alternate fix, but not the
     * swap path this test means to exercise.)
     */
    @Test
    fun `resolves a double-booking by swapping the conflicted session with an unrelated clean one`() {
        val s1 = session("S1", teacherId = "T1", sectionId = "SEC1")
        val s2 = session("S2", teacherId = "T1", sectionId = "SEC2")
        val s3 = session("S3", teacherId = "T2", sectionId = "SEC2")
        val allSlots = listOf(Timeslot(1, 0), Timeslot(1, 1))
        val pinnedSlot = listOf(Timeslot(1, 0))

        val input = SchedulingInput(
            sessions = listOf(s1, s2, s3),
            availableTimeslotsBySession = mapOf("S1" to pinnedSlot, "S2" to allSlots, "S3" to allSlots),
            rooms = emptyList(),
            sectionStudentCounts = emptyMap(),
            blockedTeacherSlots = emptyMap(),
            blockedRoomSlots = emptyMap(),
            definedPeriodsByDay = mapOf(1 to setOf(0, 1)),
        )
        val assignments = mapOf("S1" to Timeslot(1, 0), "S2" to Timeslot(1, 0), "S3" to Timeslot(1, 1))

        val result = ScheduleOptimizer.optimize(
            input = input,
            assignments = assignments,
            roomBySession = emptyMap(),
            totalRoomCount = 0,
            totalDefinedPeriods = 2,
            focusSessionIds = setOf("S1", "S2"), // exactly what the repository derives from the flagged violation
        )

        assertEquals(1, result.violationsBefore)
        assertEquals(0, result.violationsAfter)
        assertTrue(result.swaps >= 1, "expected the optimizer to swap S2 with the unrelated S3, but it made no swap")
        assertEquals(Timeslot(1, 1), result.assignments["S2"])
        assertEquals(Timeslot(1, 0), result.assignments["S3"])
    }
}
