package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

data class TimetableSummary(
    val sessionType: SessionTypeEntity,
    val latestRun: ScheduleRunEntity?,
    val conflictCount: Int,
    val runCount: Int,
)

/** Backs the Home screen's list of the three Timetable categories — Class / Examination / Laboratory — each with its own latest-run status, not a flat list of every run across every type. */
class HomeViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var summaries by mutableStateOf<List<TimetableSummary>>(emptyList())
        private set

    fun load() {
        viewModelScope.launch {
            val allRuns = repository.getRuns().sortedByDescending { it.createdAtEpochMillis }
            val conflictCounts = repository.getConflictCountsByRun()
            summaries = SessionTypeEntity.entries.map { type ->
                val runsForType = allRuns.filter { it.sessionType == type }
                val latest = runsForType.firstOrNull()
                TimetableSummary(
                    sessionType = type,
                    latestRun = latest,
                    conflictCount = latest?.let { conflictCounts[it.id] ?: 0 } ?: 0,
                    runCount = runsForType.size,
                )
            }
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
        }
    }
}
