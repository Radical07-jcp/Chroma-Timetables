package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.Timeslot

/** Simplest baseline: colors vertices in whatever order the graph's vertex list provides. */
object GreedyColoring : ColoringAlgorithm {
    override val name = "greedy"

    override fun color(
        graph: ConflictGraph,
        availableTimeslotsBySession: Map<String, List<Timeslot>>,
        fixedAssignments: Map<String, Timeslot>,
    ): ColoringResult = ColoringSupport.colorInOrder(graph, graph.vertices, availableTimeslotsBySession, fixedAssignments)
}
