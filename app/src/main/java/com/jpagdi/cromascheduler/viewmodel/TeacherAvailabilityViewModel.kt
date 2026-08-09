package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import com.jpagdi.cromascheduler.engine.model.Timeslot
import kotlinx.coroutines.launch

class TeacherAvailabilityViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var blockedSlots by mutableStateOf<Set<Timeslot>>(emptySet())
        private set

    private var teacherId: String = ""

    fun load(teacherId: String) {
        this.teacherId = teacherId
        viewModelScope.launch {
            blockedSlots = repository.getTeacherBlockedSlots(teacherId)
        }
    }

    fun toggle(day: Int, period: Int) {
        val slot = Timeslot(day, period)
        val currentlyBlocked = slot in blockedSlots
        blockedSlots = if (currentlyBlocked) blockedSlots - slot else blockedSlots + slot
        viewModelScope.launch {
            repository.setTeacherBlocked(teacherId, day, period, blocked = !currentlyBlocked)
        }
    }
}
