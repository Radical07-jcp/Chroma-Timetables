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
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
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

    /** The sessions that the user explicitly authorized for this Repair workflow.
     * This is stable across swaps: moving a session out of a selected period/room does not silently
     * remove it from the repair scope. Scope can only grow through an explicit user action. */
    var repairScopeSessionIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var scopeExpansionCandidates by mutableStateOf<Set<String>>(emptySet())
        private set
    var repairMessage by mutableStateOf<String?>(null)
        private set

    /** Captures the baseline conflicts of the source timetable so pre-existing conflicts outside
     * this workflow's scope do not block a clean manual adjustment from being saved. */
    private var baselineViolationKeys: Set<String> = emptySet()

    /** Stable preview grouping captured before edits, so swapped sessions stay visible even after
     * moving away from their originally selected period/room. */
    private var scopeGroupLabelBySessionId: Map<String, String> = emptyMap()

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
    var validationMessage by mutableStateOf<String?>(null)
        private set
    var lastSwapSessionIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var lastSwapBeforeRows by mutableStateOf<Map<String, ScheduleRepository.RunSessionState>>(emptyMap())
        private set
    private var changedSessionIds: Set<String> = emptySet()

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
            baselineViolationKeys = repository.validateWorkingCopy(sourceRunId, workingTimeslots, workingRooms)
                .map(::violationKey)
                .toSet()
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
        lastSwapSessionIds = emptySet()
        lastSwapBeforeRows = emptyMap()
        changedSessionIds = emptySet()
        repairMessage = null
        scopeExpansionCandidates = emptySet()
        repairScopeSessionIds = sessionsMatchingSelectedScope()
        scopeGroupLabelBySessionId = buildScopeGroupLabels(repairScopeSessionIds)
        validationMessage = null
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
    fun currentRowForDisplay(base: ScheduleRepository.RunSessionState): ScheduleRepository.RunSessionState {
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

    /** Sessions authorized by the current Repair scope, reflecting the live working timetable. */
    val scopedSessions: List<ScheduleRepository.RunSessionState>
        get() = allSessions
            .filter { it.sessionId in repairScopeSessionIds }
            .map(::currentRowForDisplay)

    /** Grouped using the scope selected when Preview opened. The grouping is stable during edits so
     * a session that moves out of a selected period/room remains visible in the live draft. */
    val previewGroups: List<Pair<String, List<ScheduleRepository.RunSessionState>>>
        get() {
            val rowsById = scopedSessions.associateBy { it.sessionId }
            val grouped = repairScopeSessionIds
                .groupBy { scopeGroupLabelBySessionId[it] ?: "Expanded scope" }
                .mapValues { (_, ids) -> ids.mapNotNull(rowsById::get).sortedWith(compareBy({ it.day }, { it.period }, { it.subjectName ?: "" })) }
            return grouped.entries.sortedBy { it.key }.map { it.key to it.value }
        }

    private fun sessionsMatchingSelectedScope(): Set<String> {
        return when (dimension) {
            RepairDimension.TEACHER -> allSessions.filter { it.teacherId in selectedEntityIds }.map { it.sessionId }.toSet()
            RepairDimension.ROOM -> allSessions.filter { it.roomId in selectedEntityIds }.map { it.sessionId }.toSet()
            RepairDimension.CLASS -> allSessions.filter { it.sectionId in selectedEntityIds }.map { it.sessionId }.toSet()
            RepairDimension.SUBJECT -> allSessions.filter { it.subjectId in selectedEntityIds }.map { it.sessionId }.toSet()
            RepairDimension.PERIOD -> allSessions.filter { (it.day to it.period) in selectedPeriods }.map { it.sessionId }.toSet()
        }
    }

    private fun buildScopeGroupLabels(sessionIds: Set<String>): Map<String, String> {
        return allSessions.filter { it.sessionId in sessionIds }.associate { row ->
            val label = when (dimension) {
                RepairDimension.TEACHER -> selectedEntityIds.firstOrNull { it == row.teacherId }?.let { id -> teachers.find { it.id == id }?.name } ?: "Selected teachers"
                RepairDimension.ROOM -> selectedEntityIds.firstOrNull { it == row.roomId }?.let { id -> rooms.find { it.id == id }?.name } ?: "Selected rooms"
                RepairDimension.CLASS -> selectedEntityIds.firstOrNull { it == row.sectionId }?.let { id -> sections.find { it.id == id }?.name } ?: "Selected classes"
                RepairDimension.SUBJECT -> selectedEntityIds.firstOrNull { it == row.subjectId }?.let { id -> subjects.find { it.id == id }?.name } ?: "Selected subjects"
                RepairDimension.PERIOD -> "${row.dayLabel}, ${periods.find { it.dayOfWeek == row.day && it.periodIndex == row.period }?.startTime ?: "Period ${row.period}"}"
            }
            row.sessionId to label
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
        val before = allSessions
            .filter { it.sessionId == aId || it.sessionId == bId }
            .associate { it.sessionId to currentRowForDisplay(it) }
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
        repairScopeSessionIds = repairScopeSessionIds + aId + bId
        pendingChanges += 1
        lastSwapSessionIds = setOf(aId, bId)
        lastSwapBeforeRows = before
        changedSessionIds = changedSessionIds + aId + bId
        validationMessage = null
        repairMessage = null
        scopeExpansionCandidates = emptySet()
    }

    fun validateWorking() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                val violations = repository.validateWorkingCopy(sourceRunId, workingTimeslots, workingRooms)
                validationMessage = when {
                    violations.isEmpty() -> "Current adjustments are clean."
                    violations.size == 1 -> "1 conflict remains in the current adjustments."
                    else -> "${violations.size} conflicts remain in the current adjustments."
                }
            } catch (t: Throwable) {
                errorMessage = "Couldn't validate the current adjustments: ${t.message ?: t::class.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun save() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                val currentViolations = repository.validateWorkingCopy(sourceRunId, workingTimeslots, workingRooms)
                val introducedViolations = currentViolations.filter { violationKey(it) !in baselineViolationKeys }

                if (introducedViolations.isEmpty()) {
                    // Clean adjustment: persist exactly what is visible in the live working copy.
                    savedRunId = repository.commitRepairWorkflow(
                        sourceRunId = sourceRunId,
                        assignments = workingTimeslots,
                        roomBySession = workingRooms,
                        violations = currentViolations,
                        optimized = false,
                    )
                    saved = true
                    return@launch
                }

                // Conflict introduced by this adjustment: optimize only the currently authorized
                // Repair scope. The whole timetable remains in the constraint graph; everything
                // outside repairScopeSessionIds is frozen by RepairEngine's selected-session mode.
                val result = repository.repairWorkingCopyWithinScope(
                    sourceRunId = sourceRunId,
                    assignments = workingTimeslots,
                    roomBySession = workingRooms,
                    selectedSessionIds = repairScopeSessionIds,
                )

                val unresolvedNew = result.remainingViolations.filter { violationKey(it) !in baselineViolationKeys }
                if (unresolvedNew.isNotEmpty()) {
                    val outsideScope = unresolvedNew
                        .flatMap { listOfNotNull(it.sessionAId, it.sessionBId) }
                        .filter { it !in repairScopeSessionIds }
                        .toSet()
                    scopeExpansionCandidates = outsideScope
                    repairMessage = if (outsideScope.isNotEmpty()) {
                        "Repair needs to consider a schedule outside the current scope. Expand the scope to allow those schedules to move; nothing outside the current scope has been changed."
                    } else {
                        "The adjustment is still conflicting within the current scope. No outside schedule was changed."
                    }
                    return@launch
                }

                savedRunId = repository.commitRepairWorkflow(
                    sourceRunId = sourceRunId,
                    assignments = result.assignments,
                    roomBySession = result.roomBySession,
                    violations = result.remainingViolations,
                    optimized = true,
                )
                workingTimeslots = result.assignments
                workingRooms = workingRooms + result.roomBySession
                pendingChanges += result.recoloredSessionIds.count { it !in changedSessionIds }
                validationMessage = if (result.remainingViolations.isEmpty()) "Repair completed within the selected scope." else "Repair completed; pre-existing conflicts remain outside this adjustment."
                saved = true
            } catch (t: Throwable) {
                errorMessage = "Couldn't save this repair: ${t.message ?: t::class.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun expandRepairScope() {
        val additions = scopeExpansionCandidates
        if (additions.isEmpty() || busy) return
        repairScopeSessionIds = repairScopeSessionIds + additions
        scopeExpansionCandidates = emptySet()
        repairMessage = "Scope expanded by explicit user choice. Repair can now consider the newly authorized schedule(s)."
        save()
    }

    val lastSwapAfterRows: Map<String, ScheduleRepository.RunSessionState>
        get() = allSessions
            .filter { it.sessionId in lastSwapSessionIds }
            .associate { it.sessionId to currentRowForDisplay(it) }

    fun scopeExpansionLabels(): List<String> = scopeExpansionCandidates.mapNotNull { id ->
        allSessions.firstOrNull { it.sessionId == id }?.let { row ->
            "${row.subjectName ?: "Session"} • ${row.teacherName ?: row.sectionName ?: "—"} • current ${currentRowForDisplay(row).dayLabel}, ${currentRowForDisplay(row).startTime}"
        }
    }.sorted()

    private fun violationKey(v: ConstraintViolation): String {
        val pair = listOfNotNull(v.sessionAId, v.sessionBId).sorted().joinToString("|")
        return "${v.type.name}|$pair"
    }

    companion object {
        fun factory(repository: ScheduleRepository, runId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RepairWorkflowViewModel(repository, runId) as T
        }
    }
}
