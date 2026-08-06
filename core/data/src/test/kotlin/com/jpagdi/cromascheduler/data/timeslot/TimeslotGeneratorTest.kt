package com.jpagdi.cromascheduler.data.timeslot

import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.data.entity.PeriodConfigEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeslotGeneratorTest {

    @Test
    fun `single block generates the right number of slots for each active day`() {
        val config = PeriodConfigEntity(
            activeDays = "1,2,3,4,5",
            blocksEncoded = PeriodBlock.encodeList(listOf(PeriodBlock("Day", 7 * 60, 45, 6))),
        )
        val slots = TimeslotGenerator.generate(config)
        assertEquals(6 * 5, slots.size)
        assertEquals(6, slots.count { it.dayOfWeek == 1 })
    }

    @Test
    fun `AM-PM split with a lunch gap matches a real school day`() {
        // Exactly the case that broke the old single-block model: AM 6:30-12:00
        // (five 66-minute periods, arbitrary), PM 12:00-5:30 with a 15-min break.
        val am = PeriodBlock(label = "AM", startMinutesSinceMidnight = 6 * 60 + 30, periodDurationMinutes = 66, periodCount = 5)
        val pm = PeriodBlock(label = "PM", startMinutesSinceMidnight = 12 * 60, periodDurationMinutes = 60, periodCount = 5, breakAfterPeriod = 2, breakDurationMinutes = 15)
        val config = PeriodConfigEntity(activeDays = "1", blocksEncoded = PeriodBlock.encodeList(listOf(am, pm)))

        val slots = TimeslotGenerator.generate(config).sortedBy { it.periodIndex }
        assertEquals(10, slots.size) // 5 AM + 5 PM, periodIndex contiguous 0..9

        // AM block starts exactly at 6:30 and periods run back-to-back within it.
        assertEquals("06:30", slots[0].startTime)
        assertEquals("07:36", slots[0].endTime)
        assertEquals("07:36", slots[1].startTime)

        // PM block restarts its own clock at 12:00 regardless of where AM left off
        // — the gap between AM's last period ending and 12:00 is the lunch break,
        // and it required no special "break" field because PM just starts fresh.
        assertEquals("12:00", slots[5].startTime)

        // The within-PM-block break (after PM's 3rd period, i.e. periodIndex 7) pushes
        // every later PM period back by 15 minutes.
        assertEquals("14:00", slots[7].startTime) // 12:00 + 3*60
        assertEquals("15:15", slots[8].startTime) // 12:00 + 3*60 + 60 + 15 break
    }

    @Test
    fun `computedEndMinutes reflects an internal break`() {
        val block = PeriodBlock(label = "PM", startMinutesSinceMidnight = 12 * 60, periodDurationMinutes = 60, periodCount = 5, breakAfterPeriod = 2, breakDurationMinutes = 15)
        // 5 periods * 60 = 300 minutes + 15 break = 315 minutes after 12:00 -> 17:15
        assertEquals(12 * 60 + 315, block.computedEndMinutes())
    }

    @Test
    fun `encodeList and decodeList round-trip a list of blocks`() {
        val original = listOf(
            PeriodBlock("AM", 390, 66, 5),
            PeriodBlock("PM", 720, 60, 5, breakAfterPeriod = 2, breakDurationMinutes = 15),
        )
        val roundTripped = PeriodBlock.decodeList(PeriodBlock.encodeList(original))
        assertEquals(original, roundTripped)
    }
}
