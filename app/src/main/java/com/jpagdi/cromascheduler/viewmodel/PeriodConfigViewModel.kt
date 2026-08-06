package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.PeriodConfigEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class PeriodConfigViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var config by mutableStateOf(PeriodConfigEntity.DEFAULT)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var savedConfirmation by mutableStateOf(false)
        private set

    fun load() {
        viewModelScope.launch { config = repository.getPeriodConfig() }
    }

    fun update(newConfig: PeriodConfigEntity) {
        config = newConfig
        savedConfirmation = false
    }

    fun save() {
        isSaving = true
        viewModelScope.launch {
            repository.savePeriodConfigAndRegenerate(config)
            isSaving = false
            savedConfirmation = true
        }
    }
}
