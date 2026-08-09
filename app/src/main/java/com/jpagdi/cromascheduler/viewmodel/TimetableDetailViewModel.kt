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

class TimetableDetailViewModel(private val repository: ScheduleRepository, private val runId: String) : ViewModel() {
    var run by mutableStateOf<ScheduleRunEntity?>(null)
        private set
    var conflictCount by mutableStateOf(0)
        private set
    var loaded by mutableStateOf(false)
        private set
    var deleted by mutableStateOf(false)
        private set

    fun load() {
        viewModelScope.launch {
            run = repository.getRun(runId)
            conflictCount = repository.getConflicts(runId).size
            loaded = true
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteRun(runId)
            deleted = true
        }
    }

    companion object {
        fun factory(repository: ScheduleRepository, runId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TimetableDetailViewModel(repository, runId) as T
        }
    }
}
