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

data class RunWithConflicts(val run: ScheduleRunEntity, val conflictCount: Int)

class TimetableWorkspaceViewModel(private val repository: ScheduleRepository, private val sessionType: SessionTypeEntity) : ViewModel() {
    var runs by mutableStateOf<List<RunWithConflicts>>(emptyList())
        private set
    var periodsConfigured by mutableStateOf<Boolean?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            val all = repository.getRuns().filter { it.sessionType == sessionType }.sortedByDescending { it.createdAtEpochMillis }
            val conflictCounts = repository.getConflictCountsByRun()
            runs = all.map { RunWithConflicts(it, conflictCounts[it.id] ?: 0) }
            periodsConfigured = repository.isPeriodConfigured()
        }
    }

    fun deleteRun(runId: String) {
        viewModelScope.launch {
            repository.deleteRun(runId)
            load()
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository, sessionType: SessionTypeEntity) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TimetableWorkspaceViewModel(repository, sessionType) as T
        }
    }
}
