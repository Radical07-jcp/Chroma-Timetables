package com.jpagdi.cromascheduler.data.csv

import com.jpagdi.cromascheduler.data.entity.AvailabilityBlockEntity
import com.jpagdi.cromascheduler.data.entity.AvailabilityEntityType
import com.jpagdi.cromascheduler.data.entity.SessionEntity
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity

/**
 * sessions.csv: id, type, subjectId, teacherId (optional), sectionId (optional),
 *   roomTypeRequired (optional), durationPeriods (optional, default 1)
 *
 * teacherId/sectionId are optional per EngineSession's own doc comment (a faculty
 * meeting may have no section, etc.) — subjectId and type are the only fields every
 * session type genuinely needs.
 */
fun parseSessionsCsv(text: String, fileName: String = "sessions.csv"): ParsedFile<SessionEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<SessionEntity>()
    val seenIds = mutableSetOf<String>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val id = CsvValidation.requireField(row, "id", fileName, rowNumber, errors) ?: return@forEachIndexed
        val typeRaw = CsvValidation.requireField(row, "type", fileName, rowNumber, errors) ?: return@forEachIndexed
        val subjectId = CsvValidation.requireField(row, "subjectId", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (!seenIds.add(id)) {
            errors.add(CsvValidationError(fileName, rowNumber, "Duplicate session id \"$id\""))
            return@forEachIndexed
        }
        val type = runCatching { SessionTypeEntity.valueOf(typeRaw.trim().uppercase()) }.getOrNull()
        if (type == null) {
            errors.add(
                CsvValidationError(
                    fileName, rowNumber,
                    "Column \"type\" must be one of CLASS, EXAM, LAB, MEETING, SEMINAR — got \"$typeRaw\"",
                ),
            )
            return@forEachIndexed
        }
        val teacherId = row["teacherId"]?.trim().takeUnless { it.isNullOrEmpty() }
        val sectionId = row["sectionId"]?.trim().takeUnless { it.isNullOrEmpty() }
        val roomTypeRequired = row["roomTypeRequired"]?.trim().takeUnless { it.isNullOrEmpty() }
        val durationPeriods = CsvValidation.optionalInt(row, "durationPeriods", fileName, rowNumber, errors) ?: 1
        if (durationPeriods < 1) {
            errors.add(CsvValidationError(fileName, rowNumber, "Column \"durationPeriods\" must be at least 1, got $durationPeriods"))
            return@forEachIndexed
        }

        records.add(
            SessionEntity(
                id = id,
                type = type,
                subjectId = subjectId,
                teacherId = teacherId,
                sectionId = sectionId,
                roomTypeRequired = roomTypeRequired,
                durationPeriods = durationPeriods,
            ),
        )
    }
    return ParsedFile(records, errors)
}

/**
 * availability.csv: entityType (TEACHER or ROOM), entityId, dayOfWeek (1-7), periodIndex
 *
 * Each row represents a BLOCKED slot, matching the AvailabilityBlockEntity design from
 * Phase 1 — the table only stores exceptions, not every open slot, so a mostly-available
 * teacher/room needs only a handful of rows instead of one per open period.
 */
fun parseAvailabilityCsv(text: String, fileName: String = "availability.csv"): ParsedFile<AvailabilityBlockEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<AvailabilityBlockEntity>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val entityTypeRaw = CsvValidation.requireField(row, "entityType", fileName, rowNumber, errors) ?: return@forEachIndexed
        val entityId = CsvValidation.requireField(row, "entityId", fileName, rowNumber, errors) ?: return@forEachIndexed
        val dayOfWeek = CsvValidation.requireInt(row, "dayOfWeek", fileName, rowNumber, errors) ?: return@forEachIndexed
        val periodIndex = CsvValidation.requireInt(row, "periodIndex", fileName, rowNumber, errors) ?: return@forEachIndexed

        val entityType = runCatching { AvailabilityEntityType.valueOf(entityTypeRaw.trim().uppercase()) }.getOrNull()
        if (entityType == null) {
            errors.add(CsvValidationError(fileName, rowNumber, "Column \"entityType\" must be TEACHER or ROOM — got \"$entityTypeRaw\""))
            return@forEachIndexed
        }
        if (dayOfWeek !in 1..7) {
            errors.add(CsvValidationError(fileName, rowNumber, "Column \"dayOfWeek\" must be 1-7, got $dayOfWeek"))
            return@forEachIndexed
        }
        if (periodIndex < 0) {
            errors.add(CsvValidationError(fileName, rowNumber, "Column \"periodIndex\" must be 0 or greater, got $periodIndex"))
            return@forEachIndexed
        }

        records.add(AvailabilityBlockEntity(entityType, entityId, dayOfWeek, periodIndex))
    }
    return ParsedFile(records, errors)
}
