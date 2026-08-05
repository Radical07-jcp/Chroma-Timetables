package com.jpagdi.cromascheduler.engine.graph

import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphBuilderTest {

    private fun session(
        id: String,
        teacherId: String? = null,
        sectionId: String? = null,
    ) = EngineSession(id = id, type = SessionType.CLASS, subjectId = "SUBJ", teacherId = teacherId, sectionId = sectionId)

    @Test
    fun `sessions sharing a teacher get an edge`() {
        val sessions = listOf(
            session("S1", teacherId = "T1"),
            session("S2", teacherId = "T1"),
            session("S3", teacherId = "T2"),
        )
        val graph = GraphBuilder.buildConflictGraph(sessions)

        assertTrue("S2" in graph.neighborsOf("S1"))
        assertFalse("S3" in graph.neighborsOf("S1"))
        assertEquals(1, graph.edgeCount)
    }

    @Test
    fun `sessions sharing a section get an edge`() {
        val sessions = listOf(
            session("S1", sectionId = "SEC1"),
            session("S2", sectionId = "SEC1"),
        )
        val graph = GraphBuilder.buildConflictGraph(sessions)
        assertTrue("S2" in graph.neighborsOf("S1"))
    }

    @Test
    fun `a session sharing both teacher and section with another still yields a single edge`() {
        val sessions = listOf(
            session("S1", teacherId = "T1", sectionId = "SEC1"),
            session("S2", teacherId = "T1", sectionId = "SEC1"),
        )
        val graph = GraphBuilder.buildConflictGraph(sessions)
        // ConflictGraph.addEdge uses a Set, so calling it twice for the same pair
        // (once for teacher, once for section) must not double-count the edge.
        assertEquals(1, graph.edgeCount)
    }

    @Test
    fun `sessions with no shared teacher or section and null ids never conflict`() {
        val sessions = listOf(session("S1"), session("S2"))
        val graph = GraphBuilder.buildConflictGraph(sessions)
        assertEquals(0, graph.edgeCount)
    }

    @Test
    fun `three sessions sharing one teacher form a triangle`() {
        val sessions = listOf(
            session("S1", teacherId = "T1"),
            session("S2", teacherId = "T1"),
            session("S3", teacherId = "T1"),
        )
        val graph = GraphBuilder.buildConflictGraph(sessions)
        assertEquals(3, graph.edgeCount)
        assertEquals(2, graph.degreeOf("S1"))
    }
}
