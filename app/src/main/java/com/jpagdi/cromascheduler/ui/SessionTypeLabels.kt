package com.jpagdi.cromascheduler.ui

import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity

/** Human-readable label for a session type — used everywhere a SessionTypeEntity is shown to a person. */
fun SessionTypeEntity.label(): String = when (this) {
    SessionTypeEntity.CLASS -> "Class Schedule"
    SessionTypeEntity.EXAM -> "Examination Schedule"
    SessionTypeEntity.LAB -> "Laboratory Schedule"
}

/** Short form for compact UI (status pills, tab labels). */
fun SessionTypeEntity.shortLabel(): String = when (this) {
    SessionTypeEntity.CLASS -> "Class"
    SessionTypeEntity.EXAM -> "Exam"
    SessionTypeEntity.LAB -> "Lab"
}
