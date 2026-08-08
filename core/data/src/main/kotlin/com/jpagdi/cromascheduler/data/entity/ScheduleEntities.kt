package com.jpagdi.cromascheduler.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors engine.model.SessionType but kept as a separate String-backed enum here —
 * the data layer must never import from :core:engine's model package. Mapping between
 * the two happens once, in the repository layer that hands sessions to the engine.
 */
enum class SessionTypeEntity { CLASS, EXAM, LAB }

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val type: SessionTypeEntity,
    val subjectId: String?,
    val teacherId: String?,
    val sectionId: String?,
    val roomTypeRequired: String?,
    val durationPeriods: Int = 1,
)

enum class AvailabilityEntityType { TEACHER, ROOM }

/**
 * One row per (entity, day, period) combination that's explicitly BLOCKED. Absence of
 * a row means available — this keeps the table small for the common case (most
 * teachers/rooms are available most of the time) instead of needing a row for every
 * open slot too.
 */
@Entity(
    tableName = "availability_blocks",
    primaryKeys = ["entityType", "entityId", "dayOfWeek", "periodIndex"],
)
data class AvailabilityBlockEntity(
    val entityType: AvailabilityEntityType,
    val entityId: String,
    val dayOfWeek: Int,
    val periodIndex: Int,
)

@Entity(
    tableName = "timeslots",
    primaryKeys = ["dayOfWeek", "periodIndex"],
)
data class TimeslotEntity(
    val dayOfWeek: Int,
    val periodIndex: Int,
    val startTime: String, // "HH:mm", stored as text — no timezone concerns for a same-device local schedule
    val endTime: String,
)

/**
 * One contiguous chunk of a school day — "Morning" 6:30-12:00, "Afternoon"
 * 12:00-5:30, or however many blocks a school actually runs. Each block owns its
 * own start time, period length, and period count, so an AM session of 60-minute
 * periods and a PM session of 50-minute periods (or any other mismatch) are both
 * representable — the earlier single-block model couldn't express that at all.
 *
 * A gap between one block's computed end and the next block's start time (e.g.
 * AM ends 11:45, PM starts 12:00) becomes a natural lunch break with no special
 * handling needed when a school splits AM/PM into two separate blocks.
 * [breakAfterPeriod]/[breakDurationMinutes] and [lunchAfterPeriod]/[lunchDurationMinutes]
 * cover the other case — a single continuous block that still needs a short recess
 * AND a lunch period inside it, e.g. one block 7:30-3:30 with a 15-min break after
 * period 2 and a 45-min lunch after period 4. They're independent fields (not a list
 * of breaks) because two is the realistic ceiling for one block; a school needing
 * more than that is almost always better modeled as separate blocks instead.
 *
 * Stored as a delimited string (see encode/decode below) rather than a separate
 * Room table — this is small, read-as-a-whole settings data that's never queried
 * by SQL, so a second table plus join logic wouldn't earn its keep. Same reasoning
 * PeriodConfigEntity.activeDays already used for the day list.
 */
data class PeriodBlock(
    val label: String,
    val startMinutesSinceMidnight: Int,
    val periodDurationMinutes: Int,
    val periodCount: Int,
    val breakAfterPeriod: Int? = null, // 0-based index WITHIN this block; null = no short break
    val breakDurationMinutes: Int = 0,
    val lunchAfterPeriod: Int? = null, // 0-based index WITHIN this block; null = no lunch break
    val lunchDurationMinutes: Int = 0,
) {
    /** When this block's last period ends — purely computed, never stored, so the UI can show it as live feedback while editing. */
    fun computedEndMinutes(): Int {
        var cursor = startMinutesSinceMidnight
        for (p in 0 until periodCount) {
            cursor += periodDurationMinutes
            if (breakAfterPeriod == p) cursor += breakDurationMinutes
            if (lunchAfterPeriod == p) cursor += lunchDurationMinutes
        }
        return cursor
    }

    companion object {
        private fun encode(b: PeriodBlock): String {
            val safeLabel = b.label.replace("|", "").replace(";", "")
            return listOf(
                safeLabel, b.startMinutesSinceMidnight, b.periodDurationMinutes, b.periodCount,
                b.breakAfterPeriod ?: -1, b.breakDurationMinutes,
                b.lunchAfterPeriod ?: -1, b.lunchDurationMinutes,
            ).joinToString("|")
        }

        private fun decode(text: String): PeriodBlock? {
            val parts = text.split("|")
            // 6 parts = pre-lunch-break format still saved on an older device; read it as
            // "no lunch break configured yet" rather than failing to load the whole block.
            if (parts.size != 6 && parts.size != 8) return null
            return PeriodBlock(
                label = parts[0],
                startMinutesSinceMidnight = parts[1].toIntOrNull() ?: return null,
                periodDurationMinutes = parts[2].toIntOrNull() ?: return null,
                periodCount = parts[3].toIntOrNull() ?: return null,
                breakAfterPeriod = parts[4].toIntOrNull()?.takeIf { it >= 0 },
                breakDurationMinutes = parts[5].toIntOrNull() ?: 0,
                lunchAfterPeriod = parts.getOrNull(6)?.toIntOrNull()?.takeIf { it >= 0 },
                lunchDurationMinutes = parts.getOrNull(7)?.toIntOrNull() ?: 0,
            )
        }

        fun encodeList(blocks: List<PeriodBlock>): String = blocks.joinToString(";") { encode(it) }

        fun decodeList(text: String): List<PeriodBlock> =
            if (text.isBlank()) emptyList() else text.split(";").mapNotNull { decode(it) }
    }
}

/**
 * Single-row settings table (id is always "default") capturing how a school defines
 * its day. Different schools genuinely differ here — some run one continuous block
 * of same-length periods, others (like an AM/PM split with a lunch gap between
 * them) need several blocks with different lengths. This is the source of truth
 * TimeslotGenerator reads to (re)build the `timeslots` table; changing it and
 * regenerating is how a school changes its schedule shape without anyone
 * hand-editing rows.
 */
@Entity(tableName = "period_config")
data class PeriodConfigEntity(
    @PrimaryKey val id: String = DEFAULT_ID,
    val activeDays: String, // comma-joined day-of-week ints, 1=Monday..7=Sunday
    val blocksEncoded: String, // see PeriodBlock.encodeList/decodeList
    // false for the auto-seeded PeriodConfigEntity.DEFAULT below, true the moment a person actually
    // saves something via Settings -> Define Periods — this is what GenerateScreen gates on, since a
    // generic 60-min/8-period default silently applying to a real school's bell schedule is worse
    // than blocking Generate with an explicit "set your periods first" message.
    val isUserConfigured: Boolean = false,
) {
    fun blocks(): List<PeriodBlock> = PeriodBlock.decodeList(blocksEncoded)

    fun activeDaysList(): List<Int> = activeDays.split(",").mapNotNull { it.trim().toIntOrNull() }

    /** Total periods across every block in one day — what the teacher-availability grid and Results tables size themselves against. */
    fun totalPeriodsPerDay(): Int = blocks().sumOf { it.periodCount }

    companion object {
        const val DEFAULT_ID = "default"

        // A generic single-block default (60-min periods, 8/day, starting 7:30 AM,
        // Monday-Friday) — NOT tailored to any specific school. Real schools are
        // expected to replace this via Settings -> Define Periods with their own
        // block(s), e.g. an AM 6:30-12:00 + PM 12:00-5:30 split.
        val DEFAULT = PeriodConfigEntity(
            activeDays = "1,2,3,4,5",
            blocksEncoded = PeriodBlock.encodeList(
                listOf(PeriodBlock(label = "Day", startMinutesSinceMidnight = 7 * 60 + 30, periodDurationMinutes = 60, periodCount = 8)),
            ),
        )
    }
}


/**
 * One row per generated schedule "run" — lets Validate/Repair/Optimize target a specific saved schedule.
 *
 * [sessionType] is the schedule TYPE this run was generated for (Class, Examination, Lab, Meeting,
 * Seminar) — added so that Home/Timetable Detail can label and filter runs, and so Generate can never
 * silently mix session types into one run. Every session actually colored into this run has this same
 * [SessionTypeEntity], enforced by ScheduleRepository.generate() ANDing the caller's sessionFilter with
 * `session.type == sessionType`, not just documented by convention.
 */
@Entity(tableName = "schedule_runs")
data class ScheduleRunEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val algorithmUsed: String,
    val mode: String, // GENERATE / GENERATE_EXAM / REPAIR / OPTIMIZE
    val executionTimeMillis: Long = 0,
    val sessionType: SessionTypeEntity = SessionTypeEntity.CLASS,
)

@Entity(
    tableName = "schedule_assignments",
    primaryKeys = ["scheduleRunId", "sessionId"],
)
data class ScheduleAssignmentEntity(
    val scheduleRunId: String,
    val sessionId: String,
    val dayOfWeek: Int,
    val periodIndex: Int,
    val roomId: String?,
)

@Entity(tableName = "conflict_records")
data class ConflictRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleRunId: String,
    val sessionAId: String,
    val sessionBId: String?, // null for a single-session constraint violation, e.g. capacity or availability
    val conflictType: String,
    val reason: String,
)
