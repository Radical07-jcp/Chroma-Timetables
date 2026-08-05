package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot

/**
 * Classic Welsh-Powell: sort vertices by degree descending, then run the same
 * greedy loop. Ties broken by session id ascending for a deterministic result
 * (spec requires deterministic scheduling results — relying on unspecified
 * HashMap/List iteration order for tie-breaking would violate that on some JVMs).
 */
object WelshPowellColoring : ColoringAlgorithm {
    override val name = "welsh_powell"

    override fun color(
        graph: ConflictGraph,
        availableTimeslotsBySession: Map<String, List<Timeslot>>,
        fixedAssignments: Map<String, Timeslot>,
    ): ColoringResult {
        val order = graph.vertices.sortedWith(
            compareByDescending<EngineSession> { graph.degreeOf(it.id) }.thenBy { it.id },
        )
        return ColoringSupport.colorInOrder(graph, order, availableTimeslotsBySession, fixedAssignments)
    }
}
