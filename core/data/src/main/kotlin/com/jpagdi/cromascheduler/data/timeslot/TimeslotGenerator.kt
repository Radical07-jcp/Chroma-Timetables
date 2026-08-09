package com.jpagdi.cromascheduler.data.timeslot

import com.jpagdi.cromascheduler.data.entity.PeriodBlock

/** Plain in-memory DTO — no longer a Room @Entity (see MIGRATION_5_6 in CromaDatabase.kt for why the old persisted `timeslots` table was dropped: it was global and one run's period config could silently apply to a different run's display). */
data class TimeslotInfo(
    val dayOfWeek: Int,
    val periodIndex: Int,
    val startTime: String, // "HH:mm"
    val endTime: String,
)

/**
 * Turns a set of period blocks + active days into the actual list of timeslots. Kept as a
 * pure function (no DB access) — every caller in ScheduleRepository computes this fresh, on
 * demand, from whichever run's own [PeriodBlock] list is relevant (a run's `generate()` call
 * uses the blocks just chosen in the creation wizard; validate/repair/optimize/export all
 * decode the SAME run's already-stored blocks) rather than one shared global table that every
 * run used to read regardless of which period config it was actually created with.
 *
 * periodIndex runs contiguously across ALL of a day's blocks (block 1's periods, then block
 * 2's, etc.) — the rest of the engine (ConflictGraph, ColoringSupport, ConstraintValidator)
 * only ever knows "day + periodIndex", never "which block", so this is the one place block
 * structure gets flattened into that simpler shape. A gap between blocks (e.g. AM ends 11:45,
 * PM starts 12:00) naturally becomes an unscheduled span of the clock — no explicit "lunch"
 * timeslot needs to exist for that gap to be respected, since nothing ever asks for a slot in it.
 */
object TimeslotGenerator {
    fun generate(blocks: List<PeriodBlock>, activeDays: List<Int>): List<TimeslotInfo> {
        val slots = mutableListOf<TimeslotInfo>()

        activeDays.forEach { day ->
            var periodIndex = 0
            blocks.forEach { block ->
                var cursor = block.startMinutesSinceMidnight
                for (p in 0 until block.periodCount) {
                    val start = cursor
                    val end = start + block.periodDurationMinutes
                    slots += TimeslotInfo(
                        dayOfWeek = day,
                        periodIndex = periodIndex,
                        startTime = formatMinutes(start),
                        endTime = formatMinutes(end),
                    )
                    cursor = end
                    if (block.breakAfterPeriod == p && block.breakDurationMinutes > 0) {
                        cursor += block.breakDurationMinutes
                    }
                    if (block.lunchAfterPeriod == p && block.lunchDurationMinutes > 0) {
                        cursor += block.lunchDurationMinutes
                    }
                    periodIndex++
                }
            }
        }
        return slots
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val wrapped = totalMinutes % (24 * 60) // defensive: a very long day config shouldn't crash formatting, just wrap
        val hours = wrapped / 60
        val minutes = wrapped % 60
        return "%02d:%02d".format(hours, minutes)
    }
}
