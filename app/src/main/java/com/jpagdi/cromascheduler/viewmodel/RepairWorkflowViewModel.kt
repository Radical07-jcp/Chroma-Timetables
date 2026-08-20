package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.RoomEntity
import com.jpagdi.cromascheduler.data.entity.SectionEntity
import com.jpagdi.cromascheduler.data.entity.SubjectEntity
import com.jpagdi.cromascheduler.data.entity.TeacherEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import com.jpagdi.cromascheduler.data.timeslot.TimeslotInfo
import com.jpagdi.cromascheduler.engine.model.Timeslot
import kotlinx.coroutines.launch

/** The five things a planner can center a manual repair around. */
enum class RepairDimension(val label: String) {
    TEACHER("Teacher"), ROOM("Room"), CLASS("Class"), SUBJECT("Subject"), PERIOD("Period")
}

enum class RepairWorkflowStep { PICK_TYPE, PICK_ENTITIES, PREVIEW }

/**
 * Backs the guided manual Repair workflow. Deliberately NOT gated on any flagged conflict —
 * per the spec, two teachers wanting to voluntarily trade periods is a completely normal use
 * case with nothing to "fix" in the validator's sense, so every teacher/room/class/subject/
 * period is selectable here regardless of whether it's ever shown up in a conflict.
 *
 * The dropdown shown on the Preview step ("Adjust by: Class/Subject/Time period/Room" etc, the
 * four dimensions other than the one picked in step 1) only changes how swap candidates are
 * FOUND and LABELED — a planner thinks "swap 7A's slot with 7B's" or "give this room to History
 * instead", not "swap period 3 with period 5". Underneath, every adjustment is the same
 * operation the engine already uses elsewhere (Optimize/Repair): exchange two sessions'
 * timeslot and/or room. Which field(s) move depends on the dimension: Time period swaps just
 * the timeslot, Room swaps just the room, Class/Subject/Teacher swap the whole placement (both)
 * since those aren't per-run assignment fields of their own.
 *
 * All edits happen in memory (working copies of the timeslot/room maps) so a planner can make
 * several swaps while previewing before committing — [save] persists the whole result as ONE
 * new version, not one per tap.
 */
class RepairWorkflowViewModel(private val repository: ScheduleRepository, private val sourceRunId: String) : ViewModel() {

    var step by mutableStateOf(RepairWorkflowStep.PICK_TYPE)
        private set
    var dimension by mutableStateOf(RepairDimension.TEACHER)
        private set

    var teachers by mutableStateOf<List<TeacherEntity>>(emptyList())
        private set
    var rooms by mutableStateOf<List<RoomEntity>>(emptyList())
        private set
    var sections by mutableStateOf<List<SectionEntity>>(emptyList())
        private set
    var subjects by mutableStateOf<List<SubjectEntity>>(emptyList())
        private set
    var periods by mutableStateOf<List<TimeslotInfo>>(emptyList())
        private set

    var selectedEntityIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var selectedPeriods by mutableStateOf<Set<Pair<Int, Int>>>(emptySet())
        private set

    var allSessions by mutableStateOf<List<ScheduleRepository.RunSessionState>>(emptyList())
        private set

    // Working (possibly-edited) copies — what actually gets committed on Save.
    var workingTimeslots by mutableStateOf<Map<String, Timeslot>>(emptyMap())
        private set
    var workingRooms by mutableStateOf<Map<String, String?>>(emptyMap())
        private set
    var pendingChanges by mutableStateOf(0)
        private set

    /** null = just previewing; non-null = a dimension is picked and its cells are highlighted/tappable. */
    var adjustBy by mutableStateOf<RepairDimension?>(null)
        private set

    /** The session tapped first in the current swap — waiting on a second tap to complete the swap. */
    var pendingSwapSessionId by mutableStateOf<String?>(null)
        private set

    var loaded by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set
    var savedRunId by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun load() {
        viewModelScope.launch {
            teachers = repository.getTeachers()
            rooms = repository.getRooms()
            sections = repository.getSections()
            subjects = repository.getSubjects()
            periods = repository.timeslotsForRun(sourceRunId)
            allSessions = repository.getRunSessionStates(sourceRunId)
            workingTimeslots = allSessions.associate { it.sessionId to Timeslot(it.day, it.period) }
            workingRooms = allSessions.associate { it.sessionId to it.roomId }
            loaded = true
        }
    }

    fun pickDimension(d: RepairDimension) {
        dimension = d
        selectedEntityIds = emptySet()
        selectedPeriods = emptySet()
        step = RepairWorkflowStep.PICK_ENTITIES
    }

    fun toggleEntity(id: String) {
        selectedEntityIds = if (id in selectedEntityIds) selectedEntityIds - id else selectedEntityIds + id
    }

    fun togglePeriod(day: Int, period: Int) {
        val key = day to period
        selectedPeriods = if (key in selectedPeriods) selectedPeriods - key else selectedPeriods + key
    }

    val canProceedFromEntities: Boolean
        get() = if (dimension == RepairDimension.PERIOD) selectedPeriods.isNotEmpty() else selectedEntityIds.size >= 2

    fun goToPreview() {
        step = RepairWorkflowStep.PREVIEW
        adjustBy = null
        pendingSwapSessionId = null
    }

    fun backToEntities() {
        step = RepairWorkflowStep.PICK_ENTITIES
        adjustBy = null
        pendingSwapSessionId = null
    }

    fun backToType() {
        step = RepairWorkflowStep.PICK_TYPE
    }

    fun selectAdjustBy(d: RepairDimension?) {
        adjustBy = d
        pendingSwapSessionId = null
    }

    /** The current (working, i.e. reflecting edits already made) placement of one session. */
    private fun currentRow(base: ScheduleRepository.RunSessionState): ScheduleRepository.RunSessionState {
        val ts = workingTimeslots[base.sessionId] ?: Timeslot(base.day, base.period)
        val roomId = workingRooms[base.sessionId]
        val slot = periods.find { it.dayOfWeek == ts.dayOfWeek && it.periodIndex == ts.periodIndex }
        val roomName = if (roomId == null) null else (rooms.find { it.id == roomId }?.name ?: base.roomName)
        return base.copy(
            day = ts.dayOfWeek,
            period = ts.periodIndex,
            startTime = slot?.startTime ?: base.startTime,
            endTime = slot?.endTime ?: base.endTime,
            dayLabel = ScheduleRepository.dayLabelFor(ts.dayOfWeek),
            roomId = roomId,
            roomName = roomName,
        )
    }

    /** Sessions in the current scope (per [dimension] + selection), reflecting live edits. */
    val scopedSessions: List<ScheduleRepository.RunSessionState>
        get() {
            val rows = allSessions.map { currentRow(it) }
            return when (dimension) {
                RepairDimension.TEACHER -> rows.filter { it.teacherId in selectedEntityIds }
                RepairDimension.ROOM -> rows.filter { it.roomId in selectedEntityIds }
                RepairDimension.CLASS -> rows.filter { it.sectionId in selectedEntityIds }
                RepairDimension.SUBJECT -> rows.filter { it.subjectId in selectedEntityIds }
                RepairDimension.PERIOD -> rows.filter { (it.day to it.period) in selectedPeriods }
            }
        }

    /** Grouped for the preview list: one section per selected teacher/room/class/subject, or per
     * selected period when [dimension] is PERIOD. */
    val previewGroups: List<Pair<String, List<ScheduleRepository.RunSessionState>>>
        get() {
            val rows = scopedSessions
            return when (dimension) {
                RepairDimension.TEACHER -> selectedEntityIds.mapNotNull { id -> teachers.find { it.id == id } }
                    .map { t -> t.name to rows.filter { it.teacherId == t.id } }
                RepairDimension.ROOM -> selectedEntityIds.mapNotNull { id -> rooms.find { it.id == id } }
                    .map { r -> r.name to rows.filter { it.roomId == r.id } }
                RepairDimension.CLASS -> selectedEntityIds.mapNotNull { id -> sections.find { it.id == id } }
                    .map { s -> s.name to rows.filter { it.sectionId == s.id } }
                RepairDimension.SUBJECT -> selectedEntityIds.mapNotNull { id -> subjects.find { it.id == id } }
                    .map { s -> s.name to rows.filter { it.subjectId == s.id } }
                RepairDimension.PERIOD -> selectedPeriods.sortedWith(compareBy({ it.first }, { it.second })).map { (day, period) ->
                    val label = "${ScheduleRepository.dayLabelFor(day)}, ${periods.find { it.dayOfWeek == day && it.periodIndex == period }?.startTime ?: "Period $period"}"
                    label to rows.filter { it.day == day && it.period == period }
                }
            }
        }

    /**
     * A session was tapped while [adjustBy] is set. First tap arms it as the swap source;
     * tapping a second (different) session in-scope completes the swap; tapping the same one
     * again cancels. Which field(s) get exchanged depends on [adjustBy] — see class doc.
     */
    fun onSessionTapped(sessionId: String) {
        val armed = pendingSwapSessionId
        if (armed == null) {
            pendingSwapSessionId = sessionId
            return
        }
        if (armed == sessionId) {
            pendingSwapSessionId = null
            return
        }
        performSwap(armed, sessionId, adjustBy ?: RepairDimension.PERIOD)
        pendingSwapSessionId = null
    }

    /** Directly swap two sessions by [adjustBy] — used by the "tap a cell -> pick from the list
     * of available options" picker (the primary interaction), rather than the two-tap flow. */
    fun swapWith(sourceSessionId: String, targetSessionId: String) {
        performSwap(sourceSessionId, targetSessionId, adjustBy ?: return)
    }

    private fun performSwap(aId: String, bId: String, by: RepairDimension) {
        val swapTime = by == RepairDimension.PERIOD || by == RepairDimension.CLASS || by == RepairDimension.SUBJECT || by == RepairDimension.TEACHER
        val swapRoom = by == RepairDimension.ROOM || by == RepairDimension.CLASS || by == RepairDimension.SUBJECT || by == RepairDimension.TEACHER
        if (swapTime) {
            val a = workingTimeslots[aId]
            val b = workingTimeslots[bId]
            if (a != null && b != null) {
                workingTimeslots = workingTimeslots + (aId to b) + (bId to a)
            }
        }
        if (swapRoom) {
            val a = workingRooms[aId]
            val b = workingRooms[bId]
            workingRooms = workingRooms + (aId to b) + (bId to a)
        }
        pendingChanges += 1
    }

    fun save() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                val newRunId = repository.commitManualRepair(sourceRunId, workingTimeslots, workingRooms)
                savedRunId = newRunId
                saved = true
            } catch (t: Throwable) {
                // Surface the failure instead of leaving the button spinning forever with no
                // feedback — a silent hang here was indistinguishable from "did nothing," and
                // backing out of it lost every adjustment made in this session.
                errorMessage = "Couldn't save this repair: ${t.message ?: t::class.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository, runId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RepairWorkflowViewModel(repository, runId) as T
        }
    }
}
