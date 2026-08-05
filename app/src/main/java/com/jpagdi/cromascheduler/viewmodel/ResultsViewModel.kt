package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.export.ScheduleExportRow
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
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
            run = repository.getRuns().find { it.id == runId }
            val conflicts = repository.getConflicts(runId)
            val input = repository.buildSchedulingInput()
            val totalPeriods = input.definedPeriodsByDay.values.sumOf { it.size }
            val roomCount = input.rooms.size
            val utilization = if (roomCount == 0 || totalPeriods == 0) {
                0.0
            } else {
                (rows.size.toDouble() / (roomCount * totalPeriods)).coerceIn(0.0, 1.0)
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
