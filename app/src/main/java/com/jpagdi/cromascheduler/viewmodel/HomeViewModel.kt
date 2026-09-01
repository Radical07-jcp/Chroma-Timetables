package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

data class TimetableRow(val run: ScheduleRunEntity, val conflictCount: Int)

/** Backs Home's list of every generated Timetable (one row per ScheduleRunEntity), newest first. */
class HomeViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var rows by mutableStateOf<List<TimetableRow>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    fun load() {
        viewModelScope.launch {
            repository.ensureLegacySnapshots()
            val roots = repository.getRootRuns()
            val conflictCounts = repository.getConflictCountsByRun()
            rows = roots.map { root ->
                // The card's status pill reflects the lineage's LATEST entry (last repair/optimize),
                // since that's the timetable a person would actually act on next — not the original
                // Generate run, which may have been conflicting and long since repaired.
                val latest = repository.getLineage(root.id).maxByOrNull { it.createdAtEpochMillis } ?: root
                TimetableRow(latest, conflictCounts[latest.id] ?: 0)
            }
            loaded = true
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
        }
    }
}
