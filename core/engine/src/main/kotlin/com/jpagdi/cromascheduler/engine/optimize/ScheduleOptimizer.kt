package com.jpagdi.cromascheduler.engine.optimize

import com.jpagdi.cromascheduler.engine.SchedulingInput
import com.jpagdi.cromascheduler.engine.coloring.ColoringSupport
import com.jpagdi.cromascheduler.engine.graph.GraphBuilder
import com.jpagdi.cromascheduler.engine.model.Timeslot

data class OptimizationResult(
    val assignments: Map<String, Timeslot>,
    val roomBySession: Map<String, String>,
    val before: QualityReport,
    val after: QualityReport,
    val passesRun: Int,
)

/**
 * "Optimize Existing Schedule" mode. Takes an already-VALID schedule (no hard
 * constraint violations — run this after ConstraintValidator confirms that, not
 * instead of it) and tries to improve its soft-constraint score.
 *
 * Deliberately a bounded, deterministic hill-climb rather than a full metaheuristic
 * (simulated annealing, genetic search, etc.): for each session in a fixed order, try
 * every other candidate start in its own available pool, keep the first one that
 * both (a) doesn't collide with any neighbor's occupied periods and (b) is still
 * valid in the session's CURRENT room, and improves the total score; otherwise leave
 * it alone. This never touches room assignment — a candidate move is only accepted
 * if the existing room stays free at the new time, which is what keeps "reduce room
 * changes" naturally satisfied instead of needing separate logic for it.
 *
 * [maxPasses] bounds total work at large session counts (spec: "hundreds to
 * thousands of sessions") and keeps results reproducible — an unbounded hill-climb
 * can in principle keep finding smaller and smaller improvements forever depending
 * on floating-point score deltas. A pass that makes zero changes stops early.
 */
object ScheduleOptimizer {
    fun optimize(
        input: SchedulingInput,
        assignments: Map<String, Timeslot>,
        roomBySession: Map<String, String>,
        totalRoomCount: Int,
        totalDefinedPeriods: Int,
        maxPasses: Int = 3,
    ): OptimizationResult {
        val graph = GraphBuilder.buildConflictGraph(input.sessions)
        val sessionById = input.sessions.associateBy { it.id }
        var currentAssignments = assignments.toMutableMap()
        val orderedIds = input.sessions.map { it.id }.sorted()

        val before = ScheduleQualityScorer.score(input.sessions, currentAssignments, roomBySession, totalRoomCount, totalDefinedPeriods)

        var passesRun = 0
        for (pass in 1..maxPasses) {
            passesRun = pass
            var changedThisPass = false

            for (sessionId in orderedIds) {
                val session = sessionById[sessionId] ?: continue
                val currentStart = currentAssignments[sessionId] ?: continue
                val roomId = roomBySession[sessionId]
                val pool = input.availableTimeslotsBySession[sessionId].orEmpty()
                if (pool.size <= 1) continue // nothing to try switching to

                val neighborsOccupied = graph.neighborsOf(sessionId)
                    .mapNotNull { neighborId ->
                        val neighborSession = sessionById[neighborId] ?: return@mapNotNull null
                        val neighborStart = currentAssignments[neighborId] ?: return@mapNotNull null
                        ColoringSupport.occupiedRun(neighborSession, neighborStart)
                    }.flatten().toSet()

                val currentScore = ScheduleQualityScorer.score(input.sessions, currentAssignments, roomBySession, totalRoomCount, totalDefinedPeriods).totalScore

                var bestStart: Timeslot? = null
                var bestScore = currentScore

                for (candidateStart in pool) {
                    if (candidateStart == currentStart) continue
                    val run = ColoringSupport.occupiedRun(session, candidateStart)
                    // Same "does the run fit contiguously in the available pool" check
                    // findValidStart does internally, applied here to this one candidate.
                    val dayPool = pool.filter { it.dayOfWeek == candidateStart.dayOfWeek }.map { it.periodIndex }.toSet()
                    val fits = (candidateStart.periodIndex until candidateStart.periodIndex + session.durationPeriods).all { it in dayPool }
                    if (!fits) continue
                    if (run.any { it in neighborsOccupied }) continue
                    if (roomId != null) {
                        val roomBlocked = input.blockedRoomSlots[roomId].orEmpty()
                        if (run.any { it in roomBlocked }) continue
                        // Also make sure no OTHER session sitting in the same room occupies this run.
                        val roomConflict = input.sessions.any { other ->
                            other.id != sessionId && roomBySession[other.id] == roomId &&
                                currentAssignments[other.id]?.let { otherStart ->
                                    ColoringSupport.occupiedRun(other, otherStart).any { it in run }
                                } == true
                        }
                        if (roomConflict) continue
                    }

                    val trial = currentAssignments.toMutableMap().apply { put(sessionId, candidateStart) }
                    val trialScore = ScheduleQualityScorer.score(input.sessions, trial, roomBySession, totalRoomCount, totalDefinedPeriods).totalScore
                    if (trialScore < bestScore) {
                        bestScore = trialScore
                        bestStart = candidateStart
                    }
                }

                if (bestStart != null) {
                    currentAssignments[sessionId] = bestStart
                    changedThisPass = true
                }
            }

            if (!changedThisPass) break
        }

        val after = ScheduleQualityScorer.score(input.sessions, currentAssignments, roomBySession, totalRoomCount, totalDefinedPeriods)
        return OptimizationResult(currentAssignments, roomBySession, before, after, passesRun)
    }
}
