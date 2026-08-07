package com.jpagdi.cromascheduler.data.repository

import androidx.room.withTransaction
import com.jpagdi.cromascheduler.data.csv.CrossFileValidator
import com.jpagdi.cromascheduler.data.csv.CsvValidationError
import com.jpagdi.cromascheduler.data.csv.ZipCsvReader
import com.jpagdi.cromascheduler.data.csv.parseAvailabilityCsv
import com.jpagdi.cromascheduler.data.csv.parseRoomsCsv
import com.jpagdi.cromascheduler.data.csv.parseSectionsCsv
import com.jpagdi.cromascheduler.data.csv.parseSessionsCsv
import com.jpagdi.cromascheduler.data.csv.parseSubjectsCsv
import com.jpagdi.cromascheduler.data.csv.parseTeachersCsv
import com.jpagdi.cromascheduler.data.db.CromaDatabase
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import java.io.InputStream

private val EXPECTED_FILES = setOf(
    "teachers.csv", "subjects.csv", "rooms.csv", "sections.csv", "sessions.csv", "availability.csv",
)

data class ImportResult(
    val filesFound: Set<String>,
    val filesMissing: Set<String>,
    val teacherCount: Int,
    val subjectCount: Int,
    val roomCount: Int,
    val sectionCount: Int,
    val sessionCount: Int,
    val availabilityBlockCount: Int,
    val errors: List<CsvValidationError>,
) {
    /** True only if every expected file was present and nothing failed row-level or cross-file validation. */
    val isClean: Boolean get() = filesMissing.isEmpty() && errors.isEmpty()
}

/**
 * Orchestrates CSV import end to end: parse whatever files are provided (individually
 * or from a zip), run cross-file referential validation, then persist everything that
 * parsed successfully in one Room transaction.
 *
 * Design decision: rows that fail row-level validation are skipped (never persisted);
 * rows that only fail CROSS-file validation (e.g. a session pointing at a teacherId
 * that isn't in this batch) are still persisted as-is. Reasoning: a teacher row might
 * simply not be in *this* import batch yet if the user is updating files one at a time
 * across multiple imports — rejecting the whole session for that would fight the "import
 * files incrementally" workflow. All such issues are surfaced in ImportResult.errors
 * either way so nothing is silently dropped without being reported.
 */
class CsvImportService(private val database: CromaDatabase) {

    suspend fun importFromZip(input: InputStream, expectedSessionType: SessionTypeEntity?): ImportResult {
        val entries = ZipCsvReader.readCsvEntries(input)
        return importFromFiles(entries, expectedSessionType)
    }

    /**
     * [expectedSessionType] is the schedule type the user picked in the Import prompt (Class,
     * Examination, Lab, Meeting, Seminar) BEFORE choosing files — see ImportScreen's type dialog. This
     * is what prevents one import from silently mixing schedule types: any sessions.csv row whose own
     * `type` column doesn't match is rejected as a row-level error and never persisted, exactly like any
     * other invalid row. teachers/subjects/rooms/sections/availability aren't session-typed themselves
     * (a teacher or room is shared across schedule types), so only sessions.csv is filtered here.
     * Passing null (e.g. from a test or a future "import everything" path) restores the old
     * accept-anything behavior.
     */
    suspend fun importFromFiles(files: Map<String, String>, expectedSessionType: SessionTypeEntity? = null): ImportResult {
        val allErrors = mutableListOf<CsvValidationError>()

        val teachers = files["teachers.csv"]?.let { parseTeachersCsv(it) }
        val subjects = files["subjects.csv"]?.let { parseSubjectsCsv(it) }
        val rooms = files["rooms.csv"]?.let { parseRoomsCsv(it) }
        val sections = files["sections.csv"]?.let { parseSectionsCsv(it) }
        val sessionsParsed = files["sessions.csv"]?.let { parseSessionsCsv(it) }
        val availability = files["availability.csv"]?.let { parseAvailabilityCsv(it) }

        // Split sessions.csv's records into the ones that match the chosen schedule type and the
        // ones that don't, rather than rejecting the whole file — one bad row shouldn't block every
        // correct row in the same import, same philosophy as every other row-level check here.
        val sessions = if (expectedSessionType != null && sessionsParsed != null) {
            val (matching, mismatched) = sessionsParsed.records.partition { it.type == expectedSessionType }
            mismatched.forEach { rejected ->
                allErrors += CsvValidationError(
                    "sessions.csv", 0,
                    "Session \"${rejected.id}\" is type ${rejected.type} but this import was started as " +
                        "$expectedSessionType — row skipped so schedule types don't get mixed. Import it " +
                        "separately as a ${rejected.type} schedule instead.",
                )
            }
            sessionsParsed.copy(records = matching)
        } else {
            sessionsParsed
        }

        listOfNotNull(teachers, subjects, rooms, sections, sessions, availability).forEach {
            allErrors += it.errors
        }

        val crossFileErrors = CrossFileValidator.validate(
            teachers = teachers?.records.orEmpty(),
            subjects = subjects?.records.orEmpty(),
            rooms = rooms?.records.orEmpty(),
            sections = sections?.records.orEmpty(),
            sessions = sessions?.records.orEmpty(),
            availability = availability?.records.orEmpty(),
        )
        allErrors += crossFileErrors

        database.withTransaction {
            teachers?.records?.let { database.teacherDao().upsertAll(it) }
            subjects?.records?.let { database.subjectDao().upsertAll(it) }
            rooms?.records?.let { database.roomDao().upsertAll(it) }
            sections?.records?.let { database.sectionDao().upsertAll(it) }
            sessions?.records?.let { database.sessionDao().upsertAll(it) }
            availability?.records?.let { database.availabilityDao().upsertAll(it) }
        }

        val filesFound = files.keys.intersect(EXPECTED_FILES)
        return ImportResult(
            filesFound = filesFound,
            filesMissing = EXPECTED_FILES - filesFound,
            teacherCount = teachers?.records?.size ?: 0,
            subjectCount = subjects?.records?.size ?: 0,
            roomCount = rooms?.records?.size ?: 0,
            sectionCount = sections?.records?.size ?: 0,
            sessionCount = sessions?.records?.size ?: 0,
            availabilityBlockCount = availability?.records?.size ?: 0,
            errors = allErrors,
        )
    }
}
