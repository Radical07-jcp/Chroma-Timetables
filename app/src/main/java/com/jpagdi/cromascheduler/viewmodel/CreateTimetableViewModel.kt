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
 */
class CreateTimetableViewModel : ViewModel() {
    var sessionType by mutableStateOf<SessionTypeEntity?>(null)
        private set

    var periodBlocks by mutableStateOf(listOf(PeriodBlock(label = "Day", startMinutesSinceMidnight = 7 * 60 + 30, periodDurationMinutes = 60, periodCount = 8)))
        private set

    var activeDays by mutableStateOf(listOf(1, 2, 3, 4, 5))
        private set

    fun setSessionType(type: SessionTypeEntity) {
        sessionType = type
    }

    fun setPeriodBlocks(blocks: List<PeriodBlock>) {
        periodBlocks = blocks
    }

    fun setActiveDays(days: List<Int>) {
        activeDays = days
    }

    fun reset() {
        sessionType = null
        periodBlocks = listOf(PeriodBlock(label = "Day", startMinutesSinceMidnight = 7 * 60 + 30, periodDurationMinutes = 60, periodCount = 8))
        activeDays = listOf(1, 2, 3, 4, 5)
    }
}
