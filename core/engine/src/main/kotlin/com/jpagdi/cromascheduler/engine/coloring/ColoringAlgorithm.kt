package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.Timeslot

/**
 * Strategy interface every graph-coloring algorithm implements.
 *
 * [availableTimeslotsBySession] is per-session, not global — filtering out a
 * teacher's or room's blocked periods happens once by the caller (SchedulingEngine)
 * before invoking color(), so the algorithms below stay pure graph algorithms with
 * no knowledge of teachers, rooms, or availability data. It's a Map keyed by
 * sessionId rather than one shared List because two sessions can legitimately have
 * different candidate pools (different teachers have different blocked periods).
 *
 * [fixedAssignments] carries sessions that must NOT be reassigned — this is what
 * lets Repair mode "recalculate only the conflicting sessions instead of rebuilding
 * the entire schedule" (spec, Conflict Repair section) reuse the exact same coloring
 * algorithms: fixed sessions are copied straight into the result, and their occupied
 * periods still count against their neighbors so the graph stays consistent.
 *
 * A "color" here is a start Timeslot, not a bare integer — sessions with
 * durationPeriods > 1 occupy a contiguous run of periods starting there (same day
 * only; a session is never modeled as spanning across days). See
 * ColoringSupport.findValidStart for how that run is validated.
 */
interface ColoringAlgorithm {
    val name: String

    fun color(
        graph: ConflictGraph,
        availableTimeslotsBySession: Map<String, List<Timeslot>>,
        fixedAssignments: Map<String, Timeslot> = emptyMap(),
    ): ColoringResult
}

/**
 * [unresolvedSessionIds] are sessions the algorithm could not place at all — every
 * candidate start either collided with a neighbor or didn't have a long-enough
 * contiguous run available. Callers (SchedulingEngine, RepairEngine) surface these
 * rather than silently dropping the session; a truly unsatisfiable session (e.g. a
 * teacher blocked on every period) is a data problem to report, not something the
 * algorithm should paper over.
 */
data class ColoringResult(
    val assignments: Map<String, Timeslot>, // sessionId -> start timeslot, includes fixedAssignments unchanged
    val unresolvedSessionIds: List<String>,
)

/** Central place callers look up an algorithm by name; DSATUR is the scheduling default per spec. */
object ColoringAlgorithmRegistry {
    val algorithms: Map<String, ColoringAlgorithm> = mapOf(
        "greedy" to GreedyColoring,
        "welsh_powell" to WelshPowellColoring,
        "dsatur" to DsaturColoring,
    )
    val default: ColoringAlgorithm = DsaturColoring
}
