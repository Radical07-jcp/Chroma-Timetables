package com.jpagdi.cromascheduler.data.timeslot

import com.jpagdi.cromascheduler.data.entity.PeriodConfigEntity
import com.jpagdi.cromascheduler.data.entity.TimeslotEntity

/**
 * Turns a PeriodConfigEntity into the actual list of TimeslotEntity rows. Kept as a
 * pure function (no DB access) so it's trivially testable and so the caller
 * (ScheduleRepository) decides when to persist — regenerating is just "clear +
 * insert this function's output" with no other logic duplicated elsewhere.
 */
object TimeslotGenerator {
    fun generate(config: PeriodConfigEntity): List<TimeslotEntity> {
        val days = config.activeDaysList()
        val slots = mutableListOf<TimeslotEntity>()

        days.forEach { day ->
            var cursor = config.dayStartMinutesSinceMidnight
            for (period in 0 until config.periodsPerDay) {
                val start = cursor
                val end = start + config.periodDurationMinutes
                slots += TimeslotEntity(
                    dayOfWeek = day,
                    periodIndex = period,
                    startTime = formatMinutes(start),
                    endTime = formatMinutes(end),
                )
                cursor = end
                if (config.breakAfterPeriod == period && config.breakDurationMinutes > 0) {
                    cursor += config.breakDurationMinutes
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
