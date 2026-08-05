package com.jpagdi.cromascheduler.data.csv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvParserTest {

    @Test
    fun `parses simple table into header-keyed maps`() {
        val text = "id,name\nT1,Ms. Cruz\nT2,Mr. Reyes\n"
        val rows = CsvParser.parseTable(text)
        assertEquals(2, rows.size)
        assertEquals("Ms. Cruz", rows[0]["name"])
        assertEquals("T2", rows[1]["id"])
    }

    @Test
    fun `handles quoted fields containing commas`() {
        val text = "id,name\nT1,\"Cruz, Maria\"\n"
        val rows = CsvParser.parseTable(text)
        assertEquals("Cruz, Maria", rows[0]["name"])
    }

    @Test
    fun `handles escaped quotes inside quoted fields`() {
        val text = "id,name\nT1,\"Ms. \"\"Mari\"\" Cruz\"\n"
        val rows = CsvParser.parseTable(text)
        assertEquals("Ms. \"Mari\" Cruz", rows[0]["name"])
    }

    @Test
    fun `parseTeachersCsv splits semicolon subjectIds and flags duplicates`() {
        val text = "id,name,subjectIds\nT1,Ms. Cruz,MATH;SCI\nT1,Duplicate,MATH\n"
        val result = parseTeachersCsv(text)
        assertEquals(1, result.records.size)
        assertEquals(listOf("MATH", "SCI"), result.records.first().subjectIds)
        assertTrue(result.errors.any { it.message.contains("Duplicate teacher id") })
    }

    @Test
    fun `parseSessionsCsv rejects unknown session type`() {
        val text = "id,type,subjectId\nS1,WORKSHOP,MATH\n"
        val result = parseSessionsCsv(text)
        assertTrue(result.records.isEmpty())
        assertTrue(result.errors.any { it.message.contains("must be one of") })
    }

    @Test
    fun `crossFileValidator flags a session referencing an unknown teacher`() {
        val subjects = parseSubjectsCsv("id,name,code\nMATH,Mathematics,MTH\n").records
        val sessions = parseSessionsCsv("id,type,subjectId,teacherId\nS1,CLASS,MATH,GHOST\n").records
        val errors = CrossFileValidator.validate(
            teachers = emptyList(), subjects = subjects, rooms = emptyList(),
            sections = emptyList(), sessions = sessions, availability = emptyList(),
        )
        assertTrue(errors.any { it.message.contains("unknown teacher") })
    }
}
