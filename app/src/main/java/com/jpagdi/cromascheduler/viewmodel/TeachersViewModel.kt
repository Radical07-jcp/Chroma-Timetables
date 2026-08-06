package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.TeacherEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class TeachersViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var teachers by mutableStateOf<List<TeacherEntity>>(emptyList())
        private set

    fun load() {
        viewModelScope.launch { teachers = repository.getTeachers() }
    }
}
