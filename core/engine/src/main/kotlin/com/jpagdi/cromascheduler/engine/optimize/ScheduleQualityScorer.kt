package com.jpagdi.cromascheduler.engine.optimize

import com.jpagdi.cromascheduler.engine.coloring.ColoringSupport
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot

/**
 * Every field is a "lower is better" raw count/measure except [roomUtilization],
 * which is "higher is better" (0.0-1.0). [totalScore] folds all five into one
 * lower-is-better number via fixed weights so ScheduleOptimizer has a single value
 * to compare candidates by. Weights are separated out as top-level constants
 * (below) specifically so they're easy to find and retune later without touching
 * the scoring logic itself.
 */
data class QualityReport(
    val teacherIdlePeriods: Int,
    val roomChangeCount: Int,
    val sectionGapPeriods: Int,
    val morningPreferenceScore: Int, // sum of period indices across all sessions — lower means more sessions sit earlier in the day
    val roomUtilization: Double,
    val totalScore: Double,
)

object ScheduleQualityScorer {
    // Tunable weights — see QualityReport doc comment. No single "correct" ratio
    // between these exists; these are a reasonable starting point (idle time and
    // room changes weighted heaviest since they're the most visible pain points
    // teachers report) meant to be adjusted once you have real schedules to compare.
    private const val IDLE_WEIGHT = 3.0
    private const val ROOM_CHANGE_WEIGHT = 2.0
    private const val GAP_WEIGHT = 1.0
    private const val MORNING_WEIGHT = 0.1
    private const val UTILIZATION_WEIGHT = 10.0 // subtracted, since higher utilization is better

    fun score(
        sessions: List<EngineSession>,
        assignments: Map<String, Timeslot>,
        roomBySession: Map<String, String>,
        totalRoomCount: Int,
        totalDefinedPeriods: Int, // across the whole week — for utilization's denominator
    ): QualityReport {
        val sessionById = sessions.associateBy { it.id }
        val occupiedById = assignments.mapNotNull { (id, start) ->
            sessionById[id]?.let { id to ColoringSupport.occupiedRun(it, start) }
        }.toMap()

        val idle = idlePeriods(sessions, occupiedById) { it.teacherId }
        val gaps = idlePeriods(sessions, occupiedById) { it.sectionId }
        val roomChanges = countRoomChanges(sessions, occupiedById, roomBySession)
        val morning = assignments.values.sumOf { it.periodIndex }
        val usedSlots = occupiedById.values.sumOf { it.size }
        val utilization = if (totalRoomCount == 0 || totalDefinedPeriods == 0) {
            0.0
        } else {
            (usedSlots.toDouble() / (totalRoomCount * totalDefinedPeriods)).coerceIn(0.0, 1.0)
        }

        val total = IDLE_WEIGHT * idle + ROOM_CHANGE_WEIGHT * roomChanges +
            GAP_WEIGHT * gaps + MORNING_WEIGHT * morning - UTILIZATION_WEIGHT * utilization

        return QualityReport(idle, roomChanges, gaps, morning, utilization, total)
    }

    /**
     * Shared by both teacher-idle-time and section-compactness scoring — same
     * underlying measure (gaps between a resource's occupied periods on a given
     * day), just grouped by a different resource id.
     */
    private fun idlePeriods(
        sessions: List<EngineSession>,
        occupiedById: Map<String, Set<Timeslot>>,
        resourceIdOf: (EngineSession) -> String?,
    ): Int {
        val byResource = sessions.filter { resourceIdOf(it) != null }.groupBy { resourceIdOf(it)!! }
        var totalIdle = 0
        byResource.values.forEach { group ->
            val allOccupied = group.flatMap { occupiedById[it.id].orEmpty() }
            val byDay = allOccupied.groupBy { it.dayOfWeek }
            byDay.values.forEach { slotsForDay ->
                val periods = slotsForDay.map { it.periodIndex }.sorted().distinct()
                if (periods.size > 1) {
                    totalIdle += (periods.last() - periods.first() + 1) - periods.size
                }
            }
        }
        return totalIdle
    }

    private fun countRoomChanges(
        sessions: List<EngineSession>,
        occupiedById: Map<String, Set<Timeslot>>,
        roomBySession: Map<String, String>,
    ): Int {
        val bySection = sessions.filter { it.sectionId != null }.groupBy { it.sectionId!! }
        var changes = 0
        bySection.values.forEach { group ->
            // Order this section's sessions chronologically by their occupied start period.
            val ordered = group
                .mapNotNull { s -> occupiedById[s.id]?.minByOrNull { it.periodIndex }?.let { start -> Triple(s, start.dayOfWeek, start.periodIndex) } }
                .sortedWith(compareBy({ it.second }, { it.third }))
            for (i in 1 until ordered.size) {
                val prevRoom = roomBySession[ordered[i - 1].first.id]
                val currRoom = roomBySession[ordered[i].first.id]
                if (prevRoom != null && currRoom != null && prevRoom != currRoom && ordered[i].second == ordered[i - 1].second) {
                    changes++ // only counts as a "change" within the same day — different days are expected to differ
                }
            }
        }
        return changes
    }
}
