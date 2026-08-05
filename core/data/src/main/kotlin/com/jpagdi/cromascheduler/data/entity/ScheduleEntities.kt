package com.jpagdi.cromascheduler.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors engine.model.SessionType but kept as a separate String-backed enum here —
 * the data layer must never import from :core:engine's model package. Mapping between
 * the two happens once, in the repository layer that hands sessions to the engine.
 */
enum class SessionTypeEntity { CLASS, EXAM, LAB, MEETING, SEMINAR }

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

/** One row per generated schedule "run" — lets Validate/Repair/Optimize target a specific saved schedule. */
@Entity(tableName = "schedule_runs")
data class ScheduleRunEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val algorithmUsed: String,
    val mode: String, // GENERATE / GENERATE_EXAM / REPAIR / OPTIMIZE
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
