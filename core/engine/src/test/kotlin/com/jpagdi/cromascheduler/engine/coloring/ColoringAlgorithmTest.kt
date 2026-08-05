package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.graph.GraphBuilder
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.SessionType
import com.jpagdi.cromascheduler.engine.model.Timeslot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColoringAlgorithmTest {

    private val weekPool = (1..5).flatMap { day -> (0..7).map { period -> Timeslot(day, period) } }

    private fun session(id: String, teacherId: String? = null, duration: Int = 1) =
        EngineSession(id = id, type = SessionType.CLASS, subjectId = "SUBJ", teacherId = teacherId, sectionId = null, durationPeriods = duration)

    private fun assertNoOverlap(graph: com.jpagdi.cromascheduler.engine.model.ConflictGraph, sessions: List<EngineSession>, result: ColoringResult) {
        val sessionById = sessions.associateBy { it.id }
        val occupied = result.assignments.mapValues { (id, start) -> ColoringSupport.occupiedRun(sessionById.getValue(id), start) }
        for (session in sessions) {
            for (neighborId in graph.neighborsOf(session.id)) {
                val a = occupied[session.id] ?: continue
                val b = occupied[neighborId] ?: continue
                assertTrue(a.intersect(b).isEmpty(), "Sessions ${session.id} and $neighborId should not overlap but do")
            }
        }
    }

    private val algorithms = listOf(GreedyColoring, WelshPowellColoring, DsaturColoring)

    @Test
    fun `all three algorithms produce a conflict-free coloring for a simple graph`() {
        val sessions = listOf(session("S1", "T1"), session("S2", "T1"), session("S3", "T2"))
        val graph = GraphBuilder.buildConflictGraph(sessions)
        val pool = sessions.associate { it.id to weekPool }

        algorithms.forEach { algo ->
            val result = algo.color(graph, pool)
            assertTrue(result.unresolvedSessionIds.isEmpty(), "${algo.name} left sessions unresolved")
            assertNoOverlap(graph, sessions, result)
        }
    }

    @Test
    fun `multi-period session reserves a contiguous run and blocks its neighbor from that whole run`() {
        val sessions = listOf(session("S1", "T1", duration = 3), session("S2", "T1"))
        val graph = GraphBuilder.buildConflictGraph(sessions)
        val pool = sessions.associate { it.id to weekPool }

        val result = DsaturColoring.color(graph, pool)
        val start1 = result.assignments.getValue("S1")
        val start2 = result.assignments.getValue("S2")
        val run1 = ColoringSupport.occupiedRun(sessions[0], start1)
        assertEquals(3, run1.size)
        assertTrue(Timeslot(start2.dayOfWeek, start2.periodIndex) !in run1 || start2.dayOfWeek != start1.dayOfWeek)
    }

    @Test
    fun `fixedAssignments are preserved unchanged and still block their neighbors`() {
        val sessions = listOf(session("S1", "T1"), session("S2", "T1"))
        val graph = GraphBuilder.buildConflictGraph(sessions)
        val pool = sessions.associate { it.id to weekPool }
        val fixed = mapOf("S1" to Timeslot(1, 0))

        val result = GreedyColoring.color(graph, pool, fixed)
        assertEquals(Timeslot(1, 0), result.assignments["S1"])
        assertTrue(result.assignments.getValue("S2") != Timeslot(1, 0))
    }

    @Test
    fun `an impossible session is reported as unresolved, not silently dropped`() {
        val sessions = listOf(session("S1", "T1"))
        val graph = GraphBuilder.buildConflictGraph(sessions)
        val result = GreedyColoring.color(graph, mapOf("S1" to emptyList()))
        assertTrue("S1" in result.unresolvedSessionIds)
        assertTrue(result.assignments.isEmpty())
    }
}
