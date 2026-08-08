package com.jpagdi.cromascheduler.engine.model

/**
 * The unit the scheduling engine works with. CLASS, EXAM, and LAB are all just an
 * EngineSession to the graph/coloring code below — the engine never branches on
 * [type]. Type exists purely for UI grouping and constraint messages (e.g. "prefer
 * morning" only applies to some types in a soft-constraint weighting, decided in
 * the constraint layer, not here).
 */
enum class SessionType { CLASS, EXAM, LAB }

/**
 * [id] must be stable and unique across a whole scheduling run — it's used as the
 * graph vertex identity. [durationPeriods] lets a session span more than one
 * consecutive timeslot; the coloring layer is responsible for reserving a contiguous
 * run of colors for anything with durationPeriods > 1 (see ColoringAlgorithm docs).
 *
 * [teacherId] and [sectionId] are nullable because not every session type needs both
 * — a self-study exam block may have no teacher, a lab makeup session may have no section.
 * Conflict-graph construction (Phase 3) only adds an edge on a shared, non-null id.
 */
data class EngineSession(
    val id: String,
    val type: SessionType,
    val subjectId: String?,
    val teacherId: String?,
    val sectionId: String?,
    val roomTypeRequired: String? = null,
    val durationPeriods: Int = 1,
)

/** A single assignable slot — the "color" in graph-coloring terms. */
data class Timeslot(
    val dayOfWeek: Int, // 1..7, matches java.time.DayOfWeek.getValue()
    val periodIndex: Int, // 0-based period within the day
)

/** Engine output for one session: which timeslot(s) and which room it landed in. */
data class ScheduleAssignment(
    val sessionId: String,
    val startTimeslot: Timeslot,
    val roomId: String?,
)
