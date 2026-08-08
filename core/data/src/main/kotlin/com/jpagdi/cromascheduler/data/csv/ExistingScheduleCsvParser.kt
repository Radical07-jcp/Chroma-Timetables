package com.jpagdi.cromascheduler.data.csv

import com.jpagdi.cromascheduler.data.entity.ScheduleAssignmentEntity

/**
 * assignments.csv — the file the standalone Repair feature accepts: an ALREADY-TIMETABLED schedule
 * (session id + when + where it currently runs), possibly with conflicts baked in, that a school
 * wants Chroma to check and fix rather than generate from scratch.
 *
 * Columns: sessionId, dayOfWeek (1=Monday..7=Sunday), periodIndex (0-based), roomId (optional — a
 * session with no fixed room can omit it).
 *
 * sessionId must already exist in the `sessions` table (imported earlier via Import Data for that
 * schedule type) — this file supplies WHERE an existing session currently sits, not the session
 * itself, same relationship sessions.csv has to teachers.csv/subjects.csv/rooms.csv.
 */
fun parseExistingAssignmentsCsv(text: String, fileName: String = "assignments.csv"): ParsedFile<ScheduleAssignmentEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<ScheduleAssignmentEntity>()
    val seenSessionIds = mutableSetOf<String>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val sessionId = CsvValidation.requireField(row, "sessionId", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (!seenSessionIds.add(sessionId)) {
            errors.add(CsvValidationError(fileName, rowNumber, "Duplicate sessionId \"$sessionId\" — each session can only be scheduled once in this file"))
            return@forEachIndexed
        }
        val dayOfWeek = CsvValidation.requireInt(row, "dayOfWeek", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (dayOfWeek !in 1..7) {
            errors.add(CsvValidationError(fileName, rowNumber, "dayOfWeek must be 1 (Monday) through 7 (Sunday) — got $dayOfWeek"))
            return@forEachIndexed
        }
        val periodIndex = CsvValidation.requireInt(row, "periodIndex", fileName, rowNumber, errors) ?: return@forEachIndexed
        val roomId = row["roomId"]?.trim().takeUnless { it.isNullOrEmpty() }

        // scheduleRunId is filled in by the repository once the run itself exists — this parser
        // only knows about the file, not which run it's being imported into.
        records.add(ScheduleAssignmentEntity(scheduleRunId = "", sessionId = sessionId, dayOfWeek = dayOfWeek, periodIndex = periodIndex, roomId = roomId))
    }

    return ParsedFile(records, errors)
}
