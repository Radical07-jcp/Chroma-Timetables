package com.jpagdi.cromascheduler.data.csv

import com.jpagdi.cromascheduler.data.entity.AvailabilityEntityType
import com.jpagdi.cromascheduler.data.entity.RoomEntity
import com.jpagdi.cromascheduler.data.entity.SectionEntity
import com.jpagdi.cromascheduler.data.entity.SessionEntity
import com.jpagdi.cromascheduler.data.entity.SubjectEntity
import com.jpagdi.cromascheduler.data.entity.TeacherEntity
import com.jpagdi.cromascheduler.data.entity.AvailabilityBlockEntity

/**
 * Per-file parsers (EntityCsvParsers.kt, SessionAvailabilityCsvParsers.kt) only catch
 * problems visible within a single row of a single file — a well-formed but
 * nonexistent teacherId on a session row parses fine on its own. This pass runs
 * after every file is parsed and catches dangling references across files, which
 * is where most real-world "missing or invalid records" (per the spec) show up:
 * a typo'd id, a row for a teacher that got deleted from teachers.csv, etc.
 *
 * Read-only — never mutates or drops records, only reports. The caller decides
 * whether to still persist rows with reference errors (CsvImportService currently
 * does — see its doc comment for why) or block on them.
 */
object CrossFileValidator {
    fun validate(
        teachers: List<TeacherEntity>,
        subjects: List<SubjectEntity>,
        rooms: List<RoomEntity>,
        sections: List<SectionEntity>,
        sessions: List<SessionEntity>,
        availability: List<AvailabilityBlockEntity>,
    ): List<CsvValidationError> {
        val errors = mutableListOf<CsvValidationError>()
        val subjectIds = subjects.map { it.id }.toSet()
        val teacherIds = teachers.map { it.id }.toSet()
        val sectionIds = sections.map { it.id }.toSet()
        val roomIds = rooms.map { it.id }.toSet()
        val roomTypes = rooms.map { it.type }.toSet()

        teachers.forEach { teacher ->
            teacher.subjectIds.forEach { subjectId ->
                if (subjectId !in subjectIds) {
                    errors.add(
                        CsvValidationError(
                            "teachers.csv", 0,
                            "Teacher \"${teacher.id}\" references unknown subject \"$subjectId\"",
                        ),
                    )
                }
            }
        }

        sessions.forEach { session ->
            if (session.subjectId !in subjectIds) {
                errors.add(CsvValidationError("sessions.csv", 0, "Session \"${session.id}\" references unknown subject \"${session.subjectId}\""))
            }
            session.teacherId?.let { teacherId ->
                if (teacherId !in teacherIds) {
                    errors.add(CsvValidationError("sessions.csv", 0, "Session \"${session.id}\" references unknown teacher \"$teacherId\""))
                }
            }
            session.sectionId?.let { sectionId ->
                if (sectionId !in sectionIds) {
                    errors.add(CsvValidationError("sessions.csv", 0, "Session \"${session.id}\" references unknown section \"$sectionId\""))
                }
            }
            session.roomTypeRequired?.let { requiredType ->
                if (requiredType !in roomTypes) {
                    errors.add(
                        CsvValidationError(
                            "sessions.csv", 0,
                            "Session \"${session.id}\" requires room type \"$requiredType\" but no imported room has that type",
                        ),
                    )
                }
            }
        }

        availability.forEach { block ->
            val known = when (block.entityType) {
                AvailabilityEntityType.TEACHER -> block.entityId in teacherIds
                AvailabilityEntityType.ROOM -> block.entityId in roomIds
            }
            if (!known) {
                errors.add(
                    CsvValidationError(
                        "availability.csv", 0,
                        "Availability block references unknown ${block.entityType.name.lowercase()} \"${block.entityId}\"",
                    ),
                )
            }
        }

        return errors
    }
}
