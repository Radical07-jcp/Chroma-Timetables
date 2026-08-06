package com.jpagdi.cromascheduler.data.timeslot

import com.jpagdi.cromascheduler.data.entity.PeriodConfigEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeslotGeneratorTest {

    @Test
    fun `generates the right number of slots for each active day`() {
        val config = PeriodConfigEntity(
            periodDurationMinutes = 45, periodsPerDay = 6,
            dayStartMinutesSinceMidnight = 7 * 60, activeDays = "1,2,3,4,5",
        )
        val slots = TimeslotGenerator.generate(config)
        assertEquals(6 * 5, slots.size)
        assertEquals(6, slots.count { it.dayOfWeek == 1 })
    }

    @Test
    fun `50-minute periods produce correctly spaced start times`() {
        val config = PeriodConfigEntity(
            periodDurationMinutes = 50, periodsPerDay = 3,
            dayStartMinutesSinceMidnight = 8 * 60, activeDays = "1",
        )
        val slots = TimeslotGenerator.generate(config).sortedBy { it.periodIndex }
        assertEquals("08:00", slots[0].startTime)
        assertEquals("08:50", slots[0].endTime)
        assertEquals("08:50", slots[1].startTime)
        assertEquals("09:40", slots[2].startTime)
    }

    @Test
    fun `a break after a period pushes every later period's start time back`() {
        val config = PeriodConfigEntity(
            periodDurationMinutes = 60, periodsPerDay = 4,
            dayStartMinutesSinceMidnight = 8 * 60, activeDays = "1",
            breakAfterPeriod = 1, breakDurationMinutes = 15,
        )
        val slots = TimeslotGenerator.generate(config).sortedBy { it.periodIndex }
        assertEquals("08:00", slots[0].startTime)
        assertEquals("09:00", slots[1].startTime)
        assertEquals("10:15", slots[2].startTime) // 09:00 + 60 (period 1) + 15 (break)
        assertEquals("11:15", slots[3].startTime)
    }
}
