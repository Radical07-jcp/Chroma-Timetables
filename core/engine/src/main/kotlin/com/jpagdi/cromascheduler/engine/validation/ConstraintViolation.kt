package com.jpagdi.cromascheduler.engine.validation

enum class ConstraintViolationType {
    TEACHER_DOUBLE_BOOKED,
    ROOM_DOUBLE_BOOKED,
    SECTION_DOUBLE_BOOKED,
    SUBJECT_DOUBLE_BOOKED,
    TEACHER_UNAVAILABLE,
    ROOM_UNAVAILABLE,
    ROOM_CAPACITY_EXCEEDED,
    DURATION_EXCEEDS_AVAILABLE_PERIODS,
}

/**
 * [sessionBId] is null for violations that only involve one session (availability,
 * capacity, duration-fit) and set for the pairwise ones (double-booking) — RepairEngine
 * relies on collecting both ids from every violation to know which sessions to recolor.
 */
data class ConstraintViolation(
    val type: ConstraintViolationType,
    val sessionAId: String,
    val sessionBId: String? = null,
    val message: String,
)
