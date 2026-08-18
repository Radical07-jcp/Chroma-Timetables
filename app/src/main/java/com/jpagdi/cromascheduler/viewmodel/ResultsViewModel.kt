package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.export.ScheduleExportRow
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import com.jpagdi.cromascheduler.data.timeslot.TimeslotGenerator
import kotlinx.coroutines.launch

data class ResultsStatistics(
    val conflictCount: Int,
    val executionTimeMillis: Long,
    val roomUtilization: Double, // 0.0-1.0
)

class ResultsViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var rows by mutableStateOf<List<ScheduleExportRow>>(emptyList())
        private set

    var run by mutableStateOf<ScheduleRunEntity?>(null)
        private set

    var statistics by mutableStateOf(ResultsStatistics(0, 0, 0.0))
        private set

    var isLoading by mutableStateOf(true)
        private set

    fun load(runId: String) {
        isLoading = true
        viewModelScope.launch {
            rows = repository.buildExportRows(runId)
            val loadedRun = repository.getRun(runId)
            run = loadedRun
            val conflicts = repository.getConflicts(runId)

            // Room utilization is computed against THIS run's own period grid, not a shared
            // global one — a run predating per-run period storage falls back the same way
            // ScheduleRepository's own internal timeslotsFor() does.
            val blocks = loadedRun?.let { PeriodBlock.decodeList(it.periodBlocksEncoded) }?.ifEmpty { PeriodBlock.FALLBACK_DEFAULT } ?: PeriodBlock.FALLBACK_DEFAULT
            val days = loadedRun?.activeDaysEncoded?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.ifEmpty { PeriodBlock.FALLBACK_DEFAULT_DAYS } ?: PeriodBlock.FALLBACK_DEFAULT_DAYS
            val totalPeriods = TimeslotGenerator.generate(blocks, days).size
            val allRoomsCount = repository.roomCountForRun(runId)
            val utilization = if (allRoomsCount == 0 || totalPeriods == 0) {
                0.0
            } else {
                (rows.size.toDouble() / (allRoomsCount * totalPeriods)).coerceIn(0.0, 1.0)
            }
            statistics = ResultsStatistics(
                conflictCount = conflicts.size,
                executionTimeMillis = run?.executionTimeMillis ?: 0,
                roomUtilization = utilization,
            )
            isLoading = false
        }
    }
}
