package com.jpagdi.cromascheduler.engine.optimize

import com.jpagdi.cromascheduler.engine.SchedulingInput
import com.jpagdi.cromascheduler.engine.coloring.ColoringSupport
import com.jpagdi.cromascheduler.engine.graph.GraphBuilder
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.engine.validation.ConstraintValidator
import com.jpagdi.cromascheduler.engine.validation.ValidationContext

data class OptimizationChange(
    val sessionId: String,
    val from: Timeslot,
    val to: Timeslot,
    val pairedSessionId: String? = null,
)

data class OptimizationResult(
    val assignments: Map<String, Timeslot>,
    val roomBySession: Map<String, String>,
    val before: QualityReport,
    val after: QualityReport,
    val passesRun: Int,
    val changes: List<OptimizationChange> = emptyList(),
    val swaps: Int = 0,
    val violationsBefore: Int = 0,
    val violationsAfter: Int = 0,
)

/**
 * Local-change optimizer for a timetable that has already been handed in.
 *
 * The optimizer is intentionally NOT a second generator. It starts with the existing
 * assignments and searches only for small repairs/improvements:
 *   1. move one session to another legal slot;
 *   2. swap the slots of two sessions when a direct move is impossible;
 *   3. accept a change when it reduces hard violations first, then improves soft quality.
 *
 * Existing assignments are therefore "sticky". A clean schedule will only move when the
 * move actually improves its quality. An invalid schedule caused by a newly changed
 * availability/preference can be repaired without rebuilding the whole timetable.
 *
 * The result is deterministic and bounded. [maxChanges] is a change budget, not a
 * permission to regenerate the schedule.
 */
object ScheduleOptimizer {
    fun optimize(
        input: SchedulingInput,
        assignments: Map<String, Timeslot>,
        roomBySession: Map<String, String>,
        totalRoomCount: Int,
        totalDefinedPeriods: Int,
        maxPasses: Int = 3,
        maxChanges: Int = 12,
        focusSessionIds: Set<String>? = null,
    ): OptimizationResult {
        val graph = GraphBuilder.buildConflictGraph(input.sessions)
        val sessionById = input.sessions.associateBy { it.id }
        var current = assignments.toMutableMap()
        val original = assignments.toMap()
        val orderedIds = input.sessions.map { it.id }.sorted()
        val focusedIds = focusSessionIds.orEmpty()
        val moveIds = if (focusedIds.isEmpty()) orderedIds else orderedIds.filter { it in focusedIds }

        fun violationsOf(candidate: Map<String, Timeslot>): Int {
            return ConstraintValidator.validate(
                ValidationContext(
                    sessions = input.sessions,
                    assignments = candidate,
                    roomBySession = roomBySession,
                    rooms = input.rooms,
                    sectionStudentCounts = input.sectionStudentCounts,
                    blockedTeacherSlots = input.blockedTeacherSlots,
                    blockedRoomSlots = input.blockedRoomSlots,
                    definedPeriodsByDay = input.definedPeriodsByDay,
                ),
            ).size
        }

        val violationsBefore = violationsOf(current)
        val before = ScheduleQualityScorer.score(
            input.sessions, current, roomBySession, totalRoomCount, totalDefinedPeriods
        )

        data class Candidate(
            val assignments: Map<String, Timeslot>,
            val violationCount: Int,
            val quality: Double,
            val changeCount: Int,
            val change: OptimizationChange,
        )

        fun candidateScore(candidate: Map<String, Timeslot>): Pair<Int, Double> {
            val v = violationsOf(candidate)
            val q = ScheduleQualityScorer.score(
                input.sessions, candidate, roomBySession, totalRoomCount, totalDefinedPeriods
            ).totalScore
            return v to q
        }

        fun occupiedFits(session: EngineSession, start: Timeslot, pool: Set<Timeslot>): Boolean {
            val run = ColoringSupport.occupiedRun(session, start)
            return run.all { it in pool }
        }

        fun hardChangeIsPlausible(sessionId: String, start: Timeslot): Boolean {
            val session = sessionById[sessionId] ?: return false
            val pool = input.availableTimeslotsBySession[sessionId].orEmpty().toSet()
            if (!occupiedFits(session, start, pool)) return false

            val run = ColoringSupport.occupiedRun(session, start)
            val roomId = roomBySession[sessionId]
            if (roomId != null) {
                if (run.any { it in input.blockedRoomSlots[roomId].orEmpty() }) return false
                val roomConflict = input.sessions.any { other ->
                    other.id != sessionId &&
                        roomBySession[other.id] == roomId &&
                        current[other.id]?.let { otherStart ->
                            ColoringSupport.occupiedRun(other, otherStart).any { it in run }
                        } == true
                }
                if (roomConflict) return false
            }
            return true
        }

        val changes = mutableListOf<OptimizationChange>()
        var swaps = 0
        var passesRun = 0

        fun changeCount(candidate: Map<String, Timeslot>): Int =
            original.count { (id, start) -> candidate[id] != null && candidate[id] != start }

        fun better(a: Candidate, b: Candidate): Boolean {
            // Hard constraints always dominate. For a clean schedule, preserve the
            // existing timetable unless quality genuinely improves.
            if (a.violationCount != b.violationCount) return a.violationCount < b.violationCount
            if (a.quality != b.quality) return a.quality < b.quality
            return a.changeCount < b.changeCount
        }

        for (pass in 1..maxPasses) {
            passesRun = pass
            var changedThisPass = false

            // First: one-session local moves. These are the smallest possible repair.
            for (sessionId in moveIds) {
                if (changeCount(current) >= maxChanges) break
                val session = sessionById[sessionId] ?: continue
                val from = current[sessionId] ?: continue
                val pool = input.availableTimeslotsBySession[sessionId].orEmpty()
                if (pool.size <= 1) continue

                val currentEval = candidateScore(current)
                var best: Candidate? = null

                for (to in pool) {
                    if (to == from || !hardChangeIsPlausible(sessionId, to)) continue
                    val trial = current.toMutableMap().apply { put(sessionId, to) }
                    val (v, q) = candidateScore(trial)
                    val c = Candidate(trial, v, q, changeCount(trial),
                        OptimizationChange(sessionId, from, to))
                    if (best == null || better(c, best!!)) best = c
                }

                if (best != null) {
                    val b = best!!
                    val improvesHard = b.violationCount < currentEval.first
                    val improvesQuality = b.violationCount == currentEval.first && b.quality < currentEval.second
                    if (improvesHard || (currentEval.first == 0 && improvesQuality)) {
                        current = b.assignments.toMutableMap()
                        changes += b.change
                        changedThisPass = true
                    }
                }
            }

            // Second: direct swaps. This is the important school-scheduler case:
            // A's preferred/available slot is occupied by B, while B can take A's slot.
            if (changeCount(current) + 2 <= maxChanges) {
                outer@ for (i in orderedIds.indices) {
                    val aId = orderedIds[i]
                    val aFrom = current[aId] ?: continue
                    for (j in i + 1 until orderedIds.size) {
                        val bId = orderedIds[j]
                        val bFrom = current[bId] ?: continue
                        if (aFrom == bFrom) continue

                        val a = sessionById[aId] ?: continue
                        val b = sessionById[bId] ?: continue

                        // Only consider a swap if at least one side is actually interested
                        // in the other's slot. This keeps the search local and deterministic.
                        val aWantsB = bFrom in input.availableTimeslotsBySession[aId].orEmpty()
                        val bWantsA = aFrom in input.availableTimeslotsBySession[bId].orEmpty()
                        if (!aWantsB && !bWantsA) continue
                        // A swap only needs to involve a flagged session, not have BOTH sides
                        // flagged. The realistic fix for a double-booking is swapping the
                        // conflicted session with an unrelated, perfectly fine one that happens
                        // to hold the slot it needs — requiring both sides to already be
                        // conflicted ruled that out every time, since a swap partner is almost
                        // always a "clean" session by definition.
                        if (focusedIds.isNotEmpty() && aId !in focusedIds && bId !in focusedIds) continue
                        if (!occupiedFits(a, bFrom, input.availableTimeslotsBySession[aId].orEmpty().toSet())) continue
                        if (!occupiedFits(b, aFrom, input.availableTimeslotsBySession[bId].orEmpty().toSet())) continue

                        val trial = current.toMutableMap()
                        trial[aId] = bFrom
                        trial[bId] = aFrom
                        val (v, q) = candidateScore(trial)
                        val currentEval = candidateScore(current)
                        val improvesHard = v < currentEval.first
                        val improvesQuality = v == currentEval.first && q < currentEval.second
                        if (improvesHard || (currentEval.first == 0 && improvesQuality)) {
                            current = trial
                            changes += OptimizationChange(aId, aFrom, bFrom, pairedSessionId = bId)
                            changes += OptimizationChange(bId, bFrom, aFrom, pairedSessionId = aId)
                            swaps++
                            changedThisPass = true
                            break@outer
                        }
                    }
                }
            }

            if (!changedThisPass) break
        }

        val after = ScheduleQualityScorer.score(
            input.sessions, current, roomBySession, totalRoomCount, totalDefinedPeriods
        )
        val violationsAfter = violationsOf(current)
        return OptimizationResult(
            assignments = current,
            roomBySession = roomBySession,
            before = before,
            after = after,
            passesRun = passesRun,
            changes = changes,
            swaps = swaps,
            violationsBefore = violationsBefore,
            violationsAfter = violationsAfter,
        )
    }
}
