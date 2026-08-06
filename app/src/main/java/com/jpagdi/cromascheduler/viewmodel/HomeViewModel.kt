package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var counts by mutableStateOf(ScheduleRepository.HomeCounts(0, 0, 0, 0))
        private set

    fun load() {
        viewModelScope.launch {
            counts = repository.getHomeCounts()
        }
    }
}
