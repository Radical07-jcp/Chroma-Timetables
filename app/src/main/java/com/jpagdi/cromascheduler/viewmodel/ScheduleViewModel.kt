package com.jpagdi.cromascheduler.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleMode
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithmRegistry
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import kotlinx.coroutines.launch

sealed class OperationUiState {
    data object Idle : OperationUiState()
    data object Running : OperationUiState()
    data class GenerateDone(val runId: String) : OperationUiState()
    data class ValidateDone(val violations: List<ConstraintViolation>) : OperationUiState()
    data class RepairDone(val newRunId: String) : OperationUiState()
    data class OptimizeDone(val newRunId: String) : OperationUiState()
    data class Failed(val message: String) : OperationUiState()
}

class ScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {
    var operationState by mutableStateOf<OperationUiState>(OperationUiState.Idle)
        private set

    var runs by mutableStateOf<List<ScheduleRunEntity>>(emptyList())
        private set

    val algorithmNames: List<String> = ColoringAlgorithmRegistry.algorithms.keys.sorted()

    fun loadRuns() {
        viewModelScope.launch {
            runs = repository.getRuns()
        }
    }

    fun generate(name: String, isExamMode: Boolean, algorithmName: String) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching {
                repository.generate(
                    name = name,
                    mode = if (isExamMode) ScheduleMode.GENERATE_EXAM else ScheduleMode.GENERATE,
                    algorithmName = algorithmName,
                    sessionFilter = { session -> !isExamMode || session.type == SessionTypeEntity.EXAM },
                )
            }.onSuccess { runId -> operationState = OperationUiState.GenerateDone(runId) }
                .onFailure { e -> operationState = OperationUiState.Failed(e.message ?: "Generate failed") }
        }
    }

    fun validate(runId: String) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching { repository.validate(runId) }
                .onSuccess { violations -> operationState = OperationUiState.ValidateDone(violations) }
                .onFailure { e -> operationState = OperationUiState.Failed(e.message ?: "Validate failed") }
        }
    }

    fun repair(runId: String, algorithmName: String) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching { repository.repair(runId, algorithmName) }
                .onSuccess { newRunId -> operationState = OperationUiState.RepairDone(newRunId) }
                .onFailure { e -> operationState = OperationUiState.Failed(e.message ?: "Repair failed") }
        }
    }

    fun optimize(runId: String) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching { repository.optimize(runId) }
                .onSuccess { newRunId -> operationState = OperationUiState.OptimizeDone(newRunId) }
                .onFailure { e -> operationState = OperationUiState.Failed(e.message ?: "Optimize failed") }
        }
    }

    fun resetOperationState() {
        operationState = OperationUiState.Idle
    }
}
