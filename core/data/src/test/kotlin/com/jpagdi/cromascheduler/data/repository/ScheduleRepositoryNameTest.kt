package com.jpagdi.cromascheduler.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleRepositoryNameTest {

    @Test
    fun `rename normalizes whitespace and blank names`() {
        assertEquals("Math Block", ScheduleRepository.sanitizeRunName("  Math Block  "))
        assertEquals("Untitled Timetable", ScheduleRepository.sanitizeRunName("   "))
        assertEquals("Biology", ScheduleRepository.sanitizeRunName("Biology"))
    }
}
