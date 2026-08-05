package com.jpagdi.cromascheduler.engine.graph

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.EngineSession

/**
 * Builds the conflict graph for a batch of sessions: two sessions get an edge if they
 * can genuinely never share a timeslot — meaning they share a non-null teacherId or a
 * non-null sectionId. (A null teacherId/sectionId never causes a conflict on that axis;
 * see EngineSession's doc comment on why those fields are nullable.)
 *
 * Deliberately O(n) per session using an index-by-resource approach rather than the
 * naive O(n^2) all-pairs comparison — with "hundreds to thousands of sessions" as a
 * stated non-functional requirement, all-pairs would mean millions of comparisons for
 * a single school's full session list. Grouping sessions by teacherId/sectionId first
 * means we only ever compare sessions that could possibly conflict.
 *
 * Explicit user-defined conflict links (mentioned in the spec as a possibility beyond
 * shared teacher/section) are NOT handled here yet — there's no data model field for
 * them as of Phase 1/2. Deferred until that's actually needed; addLinkedConflicts()
 * below is the seam to extend this without changing buildConflictGraph's signature.
 */
object GraphBuilder {

    fun buildConflictGraph(sessions: List<EngineSession>): ConflictGraph {
        val graph = ConflictGraph(sessions)

        addEdgesForSharedResource(graph, sessions) { it.teacherId }
        addEdgesForSharedResource(graph, sessions) { it.sectionId }

        return graph
    }

    private fun addEdgesForSharedResource(
        graph: ConflictGraph,
        sessions: List<EngineSession>,
        resourceIdOf: (EngineSession) -> String?,
    ) {
        val sessionsByResource: Map<String, List<EngineSession>> = sessions
            .filter { resourceIdOf(it) != null }
            .groupBy { resourceIdOf(it)!! }

        sessionsByResource.values.forEach { group ->
            for (i in group.indices) {
                for (j in i + 1 until group.size) {
                    graph.addEdge(group[i].id, group[j].id)
                }
            }
        }
    }

    /**
     * Extension point for explicit user-defined conflict pairs (e.g. "Session A and
     * Session B must never overlap even though they share no teacher/section") once
     * that data model exists. Left as a documented no-op seam rather than built
     * speculatively ahead of the actual data model.
     */
    fun addLinkedConflicts(graph: ConflictGraph, linkedPairs: List<Pair<String, String>>) {
        linkedPairs.forEach { (a, b) -> graph.addEdge(a, b) }
    }
}
