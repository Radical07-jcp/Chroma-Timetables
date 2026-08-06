package com.jpagdi.cromascheduler.data.timeslot

import com.jpagdi.cromascheduler.data.entity.PeriodConfigEntity
import com.jpagdi.cromascheduler.data.entity.TimeslotEntity

/**
 * Turns a PeriodConfigEntity into the actual list of TimeslotEntity rows. Kept as a
 * pure function (no DB access) so it's trivially testable and so the caller
 * (ScheduleRepository) decides when to persist — regenerating is just "clear +
 * insert this function's output" with no other logic duplicated elsewhere.
 *
 * periodIndex runs contiguously across ALL of a day's blocks (block 1's periods,
 * then block 2's, etc.) — the rest of the engine (ConflictGraph, ColoringSupport,
 * ConstraintValidator) only ever knows "day + periodIndex", never "which block",
 * so this is the one place block structure gets flattened into that simpler shape.
 * A gap between blocks (e.g. AM ends 11:45, PM starts 12:00) naturally becomes an
 * unscheduled span of the clock — no explicit "lunch" timeslot needs to exist for
 * that gap to be respected, since nothing ever asks for a slot in it.
 */
object TimeslotGenerator {
    fun generate(config: PeriodConfigEntity): List<TimeslotEntity> {
        val days = config.activeDaysList()
        val blocks = config.blocks()
        val slots = mutableListOf<TimeslotEntity>()

        days.forEach { day ->
            var periodIndex = 0
            blocks.forEach { block ->
                var cursor = block.startMinutesSinceMidnight
                for (p in 0 until block.periodCount) {
                    val start = cursor
                    val end = start + block.periodDurationMinutes
                    slots += TimeslotEntity(
                        dayOfWeek = day,
                        periodIndex = periodIndex,
                        startTime = formatMinutes(start),
                        endTime = formatMinutes(end),
                    )
                    cursor = end
                    if (block.breakAfterPeriod == p && block.breakDurationMinutes > 0) {
                        cursor += block.breakDurationMinutes
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
