package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity

/**
 * Lives for the lifetime of the "New Timetable" wizard only — created fresh each time the wizard
 * starts (see MainActivity's nav graph) and never touches the repository itself; it's purely a
 * carrier for what the person picked on the way to GenerateScreen, which is the one place any of
 * this actually gets persisted (as part of the new run, not as a separately-saved setting). That's
 * the whole reason two timetables can have different periods now — nothing here is shared state.
 *
 * Read-only computed properties (sessionType/periodBlocks/activeDays) backed by private
 * mutableStateOf fields, rather than `var ... private set`, because a private-visibility setter
 * still generates a JVM setter method — which collided with the explicit setSessionType() /
 * setPeriodBlocks() / setActiveDays() functions below at the bytecode level ("platform declaration
 * clash"). A get()-only val generates no setter at all, so there's nothing left to clash with.
 */
class CreateTimetableViewModel : ViewModel() {
    private var _sessionType by mutableStateOf<SessionTypeEntity?>(null)
    val sessionType: SessionTypeEntity? get() = _sessionType

    private var _periodBlocks by mutableStateOf(listOf(PeriodBlock(label = "Day", startMinutesSinceMidnight = 7 * 60 + 30, periodDurationMinutes = 60, periodCount = 8)))
    val periodBlocks: List<PeriodBlock> get() = _periodBlocks

    private var _activeDays by mutableStateOf(listOf(1, 2, 3, 4, 5))
    val activeDays: List<Int> get() = _activeDays

    fun setSessionType(type: SessionTypeEntity) {
        _sessionType = type
    }

    fun setPeriodBlocks(blocks: List<PeriodBlock>) {
        _periodBlocks = blocks
    }

    fun setActiveDays(days: List<Int>) {
        _activeDays = days
    }

    fun reset() {
        _sessionType = null
        _periodBlocks = listOf(PeriodBlock(label = "Day", startMinutesSinceMidnight = 7 * 60 + 30, periodDurationMinutes = 60, periodCount = 8))
        _activeDays = listOf(1, 2, 3, 4, 5)
    }
}
