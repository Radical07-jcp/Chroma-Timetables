package com.jpagdi.cromascheduler.data.csv

import com.jpagdi.cromascheduler.data.entity.RoomEntity
import com.jpagdi.cromascheduler.data.entity.SectionEntity
import com.jpagdi.cromascheduler.data.entity.SubjectEntity
import com.jpagdi.cromascheduler.data.entity.TeacherEntity

/**
 * ASSUMPTION FLAGGED FOR REVIEW: the spec doesn't pin down exact CSV column names,
 * so the layouts below are my best guess at the minimum fields each entity needs.
 * These are easy to change in one place (each function's requireField/row[...] calls)
 * once you confirm what your actual export/source format looks like — nothing else
 * in the app depends on the raw column names, only on the parsed entities.
 *
 * teachers.csv:  id, name, subjectIds, maxLoadPerDay (optional)
 *   subjectIds is semicolon-separated within the field, e.g. "SUBJ1;SUBJ2" — a plain
 *   comma can't be used since comma is the CSV delimiter itself.
 */
fun parseTeachersCsv(text: String, fileName: String = "teachers.csv"): ParsedFile<TeacherEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<TeacherEntity>()
    val seenIds = mutableSetOf<String>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val id = CsvValidation.requireField(row, "id", fileName, rowNumber, errors) ?: return@forEachIndexed
        val name = CsvValidation.requireField(row, "name", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (!seenIds.add(id)) {
            errors.add(CsvValidationError(fileName, rowNumber, "Duplicate teacher id \"$id\""))
            return@forEachIndexed
        }
        val subjectIds = row["subjectIds"].orEmpty().split(";").map { it.trim() }.filter { it.isNotEmpty() }
        val maxLoad = CsvValidation.optionalInt(row, "maxLoadPerDay", fileName, rowNumber, errors)
        records.add(TeacherEntity(id = id, name = name, subjectIds = subjectIds, maxLoadPerDay = maxLoad))
    }
    return ParsedFile(records, errors)
}

/** subjects.csv: id, name, code */
fun parseSubjectsCsv(text: String, fileName: String = "subjects.csv"): ParsedFile<SubjectEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<SubjectEntity>()
    val seenIds = mutableSetOf<String>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val id = CsvValidation.requireField(row, "id", fileName, rowNumber, errors) ?: return@forEachIndexed
        val name = CsvValidation.requireField(row, "name", fileName, rowNumber, errors) ?: return@forEachIndexed
        val code = CsvValidation.requireField(row, "code", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (!seenIds.add(id)) {
            errors.add(CsvValidationError(fileName, rowNumber, "Duplicate subject id \"$id\""))
            return@forEachIndexed
        }
        records.add(SubjectEntity(id = id, name = name, code = code))
    }
    return ParsedFile(records, errors)
}

/** rooms.csv: id, name, capacity, type */
fun parseRoomsCsv(text: String, fileName: String = "rooms.csv"): ParsedFile<RoomEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<RoomEntity>()
    val seenIds = mutableSetOf<String>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val id = CsvValidation.requireField(row, "id", fileName, rowNumber, errors) ?: return@forEachIndexed
        val name = CsvValidation.requireField(row, "name", fileName, rowNumber, errors) ?: return@forEachIndexed
        val capacity = CsvValidation.requireInt(row, "capacity", fileName, rowNumber, errors) ?: return@forEachIndexed
        val type = CsvValidation.requireField(row, "type", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (!seenIds.add(id)) {
            errors.add(CsvValidationError(fileName, rowNumber, "Duplicate room id \"$id\""))
            return@forEachIndexed
        }
        records.add(RoomEntity(id = id, name = name, capacity = capacity, type = type))
    }
    return ParsedFile(records, errors)
}

/** sections.csv: id, name, studentCount */
fun parseSectionsCsv(text: String, fileName: String = "sections.csv"): ParsedFile<SectionEntity> {
    val errors = mutableListOf<CsvValidationError>()
    val records = mutableListOf<SectionEntity>()
    val seenIds = mutableSetOf<String>()

    CsvParser.parseTable(text).forEachIndexed { index, row ->
        val rowNumber = index + 1
        val id = CsvValidation.requireField(row, "id", fileName, rowNumber, errors) ?: return@forEachIndexed
        val name = CsvValidation.requireField(row, "name", fileName, rowNumber, errors) ?: return@forEachIndexed
        val studentCount = CsvValidation.requireInt(row, "studentCount", fileName, rowNumber, errors) ?: return@forEachIndexed
        if (!seenIds.add(id)) {
            errors.add(CsvValidationError(fileName, rowNumber, "Duplicate section id \"$id\""))
            return@forEachIndexed
        }
        records.add(SectionEntity(id = id, name = name, studentCount = studentCount))
    }
    return ParsedFile(records, errors)
}
