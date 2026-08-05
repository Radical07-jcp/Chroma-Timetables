package com.jpagdi.cromascheduler.data.export

/**
 * One row of a rendered schedule, with every id already resolved to a display name.
 * Every format exporter below (CSV/Excel/PDF) consumes this same shape — building
 * it is the caller's job (joining SessionEntity/TeacherEntity/RoomEntity/SectionEntity
 * by id), so the exporters themselves never need database access.
 */
data class ScheduleExportRow(
    val sessionId: String,
    val sessionType: String,
    val subjectName: String,
    val teacherName: String,
    val sectionName: String,
    val roomName: String,
    val dayLabel: String, // e.g. "Monday" — resolved by the caller, exporters never guess weekday names from an int
    val startTime: String, // "HH:mm"
    val endTime: String,
)
