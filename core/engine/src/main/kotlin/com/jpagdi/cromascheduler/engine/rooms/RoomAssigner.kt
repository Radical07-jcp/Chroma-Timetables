package com.jpagdi.cromascheduler.engine.rooms

import com.jpagdi.cromascheduler.engine.coloring.ColoringSupport
import com.jpagdi.cromascheduler.engine.model.EngineSession
import com.jpagdi.cromascheduler.engine.model.Timeslot

/** Minimal room shape the engine needs — deliberately not the Room-database RoomEntity (see :core:data mapper doc). */
data class EngineRoom(val id: String, val capacity: Int, val type: String)

data class RoomAssignmentResult(
    val roomBySession: Map<String, String>, // sessionId -> roomId
    val unassignedSessionIds: List<String>, // couldn't find any suitable, free room
)

/**
 * Greedy resource assignment: for each already-colored session (in session-id order,
 * for determinism), pick the first room that (a) matches roomTypeRequired if the
 * session specifies one, (b) has capacity >= the section's student count, (c) isn't
 * already booked by another session whose occupied periods overlap this one's, and
 * (d) isn't blocked by [roomAvailability] for any of those periods.
 *
 * Picking the FIRST suitable room (not smallest-fit, not least-recently-used) is a
 * deliberate Phase 4 simplification — "maximize room utilization" and "reduce room
 * changes" are soft constraints the spec assigns to Optimize (Phase 6), so this pass
 * only needs to produce *a* valid assignment, not necessarily a good one. ScheduleOptimizer
 * (Phase 6) is where room-choice quality gets improved without touching this function.
 */
object RoomAssigner {
    /**
     * [sessionsToAssign] is the set actually needing a NEW room decision — for Generate
     * mode that's every session; for Repair mode it's only the recolored subset (the
     * caller, RepairEngine, keeps preserved sessions' original room assignments as-is
     * and passes their occupied periods in via [preOccupiedByRoom] so the newly-assigned
     * sessions can't collide with them).
     */
    fun assignRooms(
        sessionsToAssign: List<EngineSession>,
        assignments: Map<String, Timeslot>, // covers at least sessionsToAssign; from a ColoringResult
        rooms: List<EngineRoom>,
        sectionStudentCounts: Map<String, Int>,
        roomAvailability: Map<String, Set<Timeslot>>, // roomId -> blocked timeslots
        preOccupiedByRoom: Map<String, Set<Timeslot>> = emptyMap(),
    ): RoomAssignmentResult {
        val roomBySession = mutableMapOf<String, String>()
        val occupiedByRoom = mutableMapOf<String, MutableSet<Timeslot>>()
        preOccupiedByRoom.forEach { (roomId, slots) -> occupiedByRoom.getOrPut(roomId) { mutableSetOf() }.addAll(slots) }
        val unassigned = mutableListOf<String>()

        val orderedSessions = sessionsToAssign.filter { it.id in assignments }.sortedBy { it.id }

        for (session in orderedSessions) {
            val start = assignments.getValue(session.id)
            val occupied = ColoringSupport.occupiedRun(session, start)
            val requiredCapacity = session.sectionId?.let { sectionStudentCounts[it] } ?: 0

            val candidate = rooms.firstOrNull { room ->
                (session.roomTypeRequired == null || room.type == session.roomTypeRequired) &&
                    room.capacity >= requiredCapacity &&
                    occupied.none { it in roomAvailability[room.id].orEmpty() } &&
                    occupied.none { it in occupiedByRoom.getOrDefault(room.id, emptySet()) }
            }

            if (candidate == null) {
                unassigned += session.id
            } else {
                roomBySession[session.id] = candidate.id
                occupiedByRoom.getOrPut(candidate.id) { mutableSetOf() }.addAll(occupied)
            }
        }

        return RoomAssignmentResult(roomBySession, unassigned)
    }
}
