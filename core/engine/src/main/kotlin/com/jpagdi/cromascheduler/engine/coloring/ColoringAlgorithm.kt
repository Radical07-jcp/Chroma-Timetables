package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.Timeslot

/**
 * Strategy interface every graph-coloring algorithm implements. [availableTimeslots]
 * is the full ordered pool of colors to choose from — availability constraints (a
 * teacher/room blocked on a given day+period) are applied by the caller filtering
 * this list per-session before invoking color(), not inside the algorithm itself,
 * so GreedyColoring/WelshPowellColoring/DsaturColoring stay pure graph algorithms
 * with no knowledge of teachers, rooms, or availability data.
 *
 * Implementations are added in Phase 4. New algorithms in the future just implement
 * this interface and register in ColoringAlgorithmRegistry — no changes needed to
 * callers or to ConflictGraph.
 */
interface ColoringAlgorithm {
    val name: String

    fun color(
        graph: ConflictGraph,
        availableTimeslots: List<Timeslot>,
    ): Map<String, Timeslot> // sessionId -> assigned timeslot
}

/** Central place callers look up an algorithm by name; DSATUR is the scheduling default. */
object ColoringAlgorithmRegistry {
    // Populated in Phase 4 once Greedy / Welsh-Powell / DSATUR are implemented:
    // val algorithms: Map<String, ColoringAlgorithm> = mapOf(
    //     "greedy" to GreedyColoring,
    //     "welsh_powell" to WelshPowellColoring,
    //     "dsatur" to DsaturColoring,
    // )
    // val default: ColoringAlgorithm = DsaturColoring
}
