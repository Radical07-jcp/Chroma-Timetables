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

        /** Fallback ONLY for a run saved before per-run period storage existed (periodBlocksEncoded/activeDaysEncoded were added together — see ScheduleRunEntity). Every run created through the normal creation wizard always has its own real blocks. */
        val FALLBACK_DEFAULT = listOf(PeriodBlock(label = "Day", startMinutesSinceMidnight = 7 * 60 + 30, periodDurationMinutes = 60, periodCount = 8))
        val FALLBACK_DEFAULT_DAYS = listOf(1, 2, 3, 4, 5)
    }
}

/**
 * One row per generated schedule "run" — lets Validate/Repair/Optimize target a specific saved schedule.
 *
 * [sessionType] is the schedule TYPE this run was generated for (Class, Examination, Lab) — added so
 * that Home and Timetable Detail can label and filter runs, and so Generate can never silently mix
 * session types into one run. Every session actually colored into this run has this same
 * [SessionTypeEntity], enforced by ScheduleRepository.generate() ANDing the caller's sessionFilter with
 * `session.type == sessionType`, not just documented by convention.
 *
 * [periodBlocksEncoded] and [activeDaysEncoded] are THIS run's own period definition — encoded the
 * same way [PeriodBlock.encodeList] always has, and [activeDaysEncoded] the same comma-joined format
 * [PeriodConfigEntity] used to use. Each run carries its own copy rather than reading one shared
 * global config, which is what actually makes "different timetables can have different time
 * periods" true: a Class Schedule generated with an AM/PM split and an Exam Schedule generated with
 * a single continuous block can coexist, each remembering exactly what it was built with. Empty on
 * either field means "predates this — fall back to a generic default" (see ScheduleRepository's
 * timeslotsFor()), which only applies to runs created before this was added.
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
    val periodBlocksEncoded: String = "",
    val activeDaysEncoded: String = "",
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
