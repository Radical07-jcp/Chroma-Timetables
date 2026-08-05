package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.Timeslot

/**
 * DSATUR (Degree of Saturation) — the scheduling default per spec. At each step,
 * picks the uncolored vertex with the most DISTINCT colors already used among its
 * already-colored neighbors (saturation degree), breaking ties by raw graph degree,
 * then by session id for determinism. This adapts to the graph as it colors, which
 * is why it can't reuse ColoringSupport.colorInOrder (that loop assumes a fixed
 * order decided up front, like Greedy/Welsh-Powell use).
 *
 * "Color" for saturation-counting purposes is the assigned START timeslot — two
 * neighbors assigned different starts count as 2 distinct colors even if their
 * occupied runs happen to overlap in a period that isn't the start. This is a
 * simplification (true DSATUR assumes single-period colors); documented here rather
 * than hidden because it means saturation degree is an approximation once
 * multi-period sessions are in the mix, not the textbook-exact count. It still
 * produces a valid, conflict-free coloring — occupied-run overlap is what's
 * actually checked before committing an assignment (see ColoringSupport.findValidStart),
 * this only affects *which order* vertices get picked, not correctness.
 */
object DsaturColoring : ColoringAlgorithm {
    override val name = "dsatur"

    override fun color(
        graph: ConflictGraph,
        availableTimeslotsBySession: Map<String, List<Timeslot>>,
        fixedAssignments: Map<String, Timeslot>,
    ): ColoringResult {
        val sessionById = graph.vertices.associateBy { it.id }
        val assignments = mutableMapOf<String, Timeslot>()
        val occupiedBySession = mutableMapOf<String, Set<Timeslot>>()
        val unresolved = mutableListOf<String>()

        fixedAssignments.forEach { (sessionId, start) ->
            val session = sessionById[sessionId] ?: return@forEach
            assignments[sessionId] = start
            occupiedBySession[sessionId] = ColoringSupport.occupiedRun(session, start)
        }

        val uncolored = graph.vertices.map { it.id }.filterTo(mutableSetOf()) { it !in fixedAssignments }

        fun saturationDegree(id: String): Int =
            graph.neighborsOf(id).mapNotNull { assignments[it] }.toSet().size

        while (uncolored.isNotEmpty()) {
            // minWith + negated primary/secondary keys simulates "max saturation, then
            // max degree, then lowest id" in one deterministic pass without juggling
            // ascending/descending comparator semantics for a max-pick.
            val nextId = uncolored.minWith(
                compareBy<String> { -saturationDegree(it) }
                    .thenBy { -graph.degreeOf(it) }
                    .thenBy { it },
            )
            uncolored.remove(nextId)
            val session = sessionById.getValue(nextId)

            val neighborsOccupied = graph.neighborsOf(nextId)
                .mapNotNull { occupiedBySession[it] }
                .flatten()
                .toSet()
            val pool = availableTimeslotsBySession[nextId].orEmpty()
            val start = ColoringSupport.findValidStart(session, pool, neighborsOccupied)
            if (start == null) {
                unresolved += nextId
            } else {
                assignments[nextId] = start
                occupiedBySession[nextId] = ColoringSupport.occupiedRun(session, start)
            }
        }

        return ColoringResult(assignments, unresolved)
    }
}
