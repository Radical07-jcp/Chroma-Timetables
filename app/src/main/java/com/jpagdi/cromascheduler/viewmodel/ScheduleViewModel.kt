package com.jpagdi.cromascheduler.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.csv.parseExistingAssignmentsCsv
import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.data.repository.ScheduleMode
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithmRegistry
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class OperationUiState {
    data object Idle : OperationUiState()
    data object Running : OperationUiState()
    data class GenerateDone(val runId: String) : OperationUiState()
    data class ValidateDone(val violations: List<ConstraintViolation>) : OperationUiState()
    data class RepairDone(val newRunId: String) : OperationUiState()
    data class OptimizeDone(val outcome: ScheduleRepository.OptimizationOutcome) : OperationUiState()
    /** The standalone Repair feature's upload step lands here — already validated (see ScheduleRepository.importExistingSchedule), so the Repair-upload screen can go straight to showing conflicts without a second round trip. */
    data class ImportedForRepair(val runId: String, val violations: List<ConstraintViolation>) : OperationUiState()
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

    /**
     * [sessionType] is mandatory — chosen in step 1 of the New Timetable wizard — and is what makes
     * every run single-type all the way through ScheduleRepository.generate(). [periodBlocks] and
     * [activeDays] are that SAME wizard's step 2, this timetable's own periods, never a shared
     * setting — see ScheduleRunEntity's doc comment for why that's what makes different timetables
     * able to have different time periods. GENERATE_EXAM is still recorded as the run's `mode` when
     * the type is EXAM, purely for the existing mode-label display.
     */
    fun generate(name: String, sessionType: SessionTypeEntity, periodBlocks: List<PeriodBlock>, activeDays: List<Int>, algorithmName: String) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching {
                repository.generate(
                    name = name,
                    mode = if (sessionType == SessionTypeEntity.EXAM) ScheduleMode.GENERATE_EXAM else ScheduleMode.GENERATE,
                    sessionType = sessionType,
                    periodBlocks = periodBlocks,
                    activeDays = activeDays,
                    algorithmName = algorithmName,
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

    fun optimize(runId: String, maxChanges: Int = 12) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching { repository.optimize(runId, maxChanges = maxChanges) }
                .onSuccess { outcome -> operationState = OperationUiState.OptimizeDone(outcome) }
                .onFailure { e -> operationState = OperationUiState.Failed(e.message ?: "Optimize failed") }
        }
    }

    fun resetOperationState() {
        operationState = OperationUiState.Idle
    }

    /** Reads and parses an assignments.csv from [uri], persists it as a new IMPORTED run under [sessionType] with [periodBlocks]/[activeDays], and validates it immediately — the standalone Repair feature's only entry point. */
    fun importExistingSchedule(context: Context, uri: Uri, name: String, sessionType: SessionTypeEntity, periodBlocks: List<PeriodBlock>, activeDays: List<Int>) {
        operationState = OperationUiState.Running
        viewModelScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Could not open the selected file")
                }
                val parsed = parseExistingAssignmentsCsv(text)
                if (parsed.errors.isNotEmpty()) {
                    error(parsed.errors.joinToString("\n") { "${it.fileName} row ${it.rowNumber}: ${it.message}" })
                }
                repository.importExistingSchedule(name, sessionType, periodBlocks, activeDays, parsed.records)
            }.onSuccess { (runId, violations) -> operationState = OperationUiState.ImportedForRepair(runId, violations) }
                .onFailure { e -> operationState = OperationUiState.Failed(e.message ?: "Import failed") }
        }
    }
}
