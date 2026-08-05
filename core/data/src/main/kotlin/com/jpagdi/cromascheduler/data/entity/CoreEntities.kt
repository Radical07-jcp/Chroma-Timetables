package com.jpagdi.cromascheduler.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * subjectIds/id lists are stored as a comma-joined String via a Room TypeConverter
 * (see Converters.kt) rather than a separate join table — kept simple for Phase 1;
 * revisit if a many-to-many query pattern (e.g. "which teachers can teach Subject X")
 * turns out to need real SQL joins instead of an in-memory split.
 */
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subjectIds: List<String>,
    val maxLoadPerDay: Int? = null,
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val capacity: Int,
    val type: String, // "regular", "lab", etc. — free-text, validated against Subject/Session needs at import time
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val studentCount: Int,
)
