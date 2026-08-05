package com.jpagdi.cromascheduler.engine.model

/**
 * Undirected conflict graph. An edge between two sessions means they must never be
 * assigned the same timeslot. Deliberately does NOT model rooms — room assignment is
 * a separate resource-assignment pass that runs after coloring (see engine README).
 * Baking room conflicts into this graph would corrupt DSATUR's degree/saturation
 * calculation, which is meant to reflect only genuine "can never co-occur" constraints
 * (shared teacher, shared section, or an explicit user-defined link), not a resource
 * that could simply be swapped to a different room instead.
 *
 * Built in Phase 3 from EngineSession lists; consumed by every ColoringAlgorithm.
 */
class ConflictGraph(val vertices: List<EngineSession>) {
    private val adjacency: MutableMap<String, MutableSet<String>> = vertices.associate {
        it.id to mutableSetOf<String>()
    }.toMutableMap()

    fun addEdge(sessionIdA: String, sessionIdB: String) {
        require(sessionIdA != sessionIdB) { "A session cannot conflict with itself: $sessionIdA" }
        adjacency.getValue(sessionIdA).add(sessionIdB)
        adjacency.getValue(sessionIdB).add(sessionIdA)
    }

    fun neighborsOf(sessionId: String): Set<String> = adjacency[sessionId].orEmpty()

    fun degreeOf(sessionId: String): Int = adjacency[sessionId]?.size ?: 0

    val edgeCount: Int get() = adjacency.values.sumOf { it.size } / 2
}
