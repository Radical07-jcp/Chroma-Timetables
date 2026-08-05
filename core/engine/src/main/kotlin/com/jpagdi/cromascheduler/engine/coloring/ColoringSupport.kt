package com.jpagdi.cromascheduler.engine.coloring

import com.jpagdi.cromascheduler.engine.model.ConflictGraph
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot

object ColoringSupport {

    /**
     * Finds the first candidate start timeslot in [availableTimeslots] (this
     * session's own pool — already filtered for its teacher's/section's blocked
     * periods by the caller) where a contiguous run of [EngineSession.durationPeriods]
     * periods, same day, is (a) entirely present in the pool and (b) doesn't
     * overlap [occupiedByNeighbors]. Returns null if no such start exists anywhere
     * in the pool — that session is unresolved for this coloring pass.
     *
     * Candidates are tried in the order [availableTimeslots] provides them within
     * each day (grouped by day, sorted by period) — callers control priority (e.g.
     * "prefer morning") by how they order/filter the pool before calling color(),
     * not by anything in this function.
     */
    fun findValidStart(
        session: EngineSession,
        availableTimeslots: List<Timeslot>,
        occupiedByNeighbors: Set<Timeslot>,
    ): Timeslot? {
        val byDay = availableTimeslots.groupBy { it.dayOfWeek }
        for ((day, slotsForDay) in byDay) {
            val sorted = slotsForDay.sortedBy { it.periodIndex }
            val periodSet = sorted.map { it.periodIndex }.toSet()
            for (candidate in sorted) {
                val run = candidate.periodIndex until (candidate.periodIndex + session.durationPeriods)
                if (run.all { it in periodSet }) {
                    val runSlots = run.map { Timeslot(day, it) }
                    if (runSlots.none { it in occupiedByNeighbors }) {
                        return candidate
                    }
                }
            }
        }
        return null
    }

    /** The set of periods a session occupies once its start is known — always same-day, contiguous. */
    fun occupiedRun(session: EngineSession, start: Timeslot): Set<Timeslot> =
        (start.periodIndex until (start.periodIndex + session.durationPeriods))
            .map { Timeslot(start.dayOfWeek, it) }
            .toSet()

    /**
     * The core loop shared by GreedyColoring and WelshPowellColoring — they differ
     * only in vertex processing order, so both just build an order and delegate here.
     * DSATUR does NOT use this — its processing order depends on colors already
     * assigned so far, which this fixed-order loop can't express.
     */
    fun colorInOrder(
        graph: ConflictGraph,
        order: List<EngineSession>,
        availableTimeslotsBySession: Map<String, List<Timeslot>>,
        fixedAssignments: Map<String, Timeslot>,
    ): ColoringResult {
        val assignments = mutableMapOf<String, Timeslot>()
        val occupiedBySession = mutableMapOf<String, Set<Timeslot>>()
        val unresolved = mutableListOf<String>()
        val sessionById = graph.vertices.associateBy { it.id }

        // Seed fixed (already-valid, not-to-be-touched) sessions first so their
        // occupied periods count against neighbors from the very start.
        fixedAssignments.forEach { (sessionId, start) ->
            val session = sessionById[sessionId] ?: return@forEach
            assignments[sessionId] = start
            occupiedBySession[sessionId] = occupiedRun(session, start)
        }

        for (session in order) {
            if (session.id in fixedAssignments) continue // already seeded above
            val neighborsOccupied = graph.neighborsOf(session.id)
                .mapNotNull { occupiedBySession[it] }
                .flatten()
                .toSet()
            val pool = availableTimeslotsBySession[session.id].orEmpty()
            val start = findValidStart(session, pool, neighborsOccupied)
            if (start == null) {
                unresolved += session.id
            } else {
                assignments[session.id] = start
                occupiedBySession[session.id] = occupiedRun(session, start)
            }
        }
        return ColoringResult(assignments, unresolved)
    }
}
