package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

private enum class RepairStage { REVIEW_ORIGINAL, REPAIRING, RE_VALIDATING, RESULT }

/**
 * [onRepaired] fires only once the repaired run has been re-validated — this is the "another
 * auto-validation before shipping the new sched" behavior: the caller never sees a repaired run
 * that hasn't already been re-checked, and the screen itself shows the before/after conflict count
 * so a person can see repair actually worked before deciding what to do next (view it, optimize it
 * further, or just leave it as the new saved run).
 */
@Composable
fun RepairScreen(runId: String, onBack: () -> Unit, onOptimize: (runId: String) -> Unit, onViewTimetable: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    var stage by remember { mutableStateOf(RepairStage.REVIEW_ORIGINAL) }
    var originalViolations by remember { mutableStateOf<List<ConstraintViolation>>(emptyList()) }
    var finalRunId by remember { mutableStateOf<String?>(null) }
    var finalViolations by remember { mutableStateOf<List<ConstraintViolation>>(emptyList()) }

    LaunchedEffect(runId) { viewModel.validate(runId) }

    LaunchedEffect(viewModel.operationState) {
        when (val state = viewModel.operationState) {
            is OperationUiState.ValidateDone -> if (stage == RepairStage.REVIEW_ORIGINAL) originalViolations = state.violations
            is OperationUiState.RepairDone -> {
                stage = RepairStage.RE_VALIDATING
                finalRunId = state.newRunId
                viewModel.validate(state.newRunId)
            }
            else -> Unit
        }
    }

    // The re-validate call above reuses ValidateDone, so a second listener distinguishes
    // "validating the original" from "re-validating the repaired result" via `stage`.
    LaunchedEffect(viewModel.operationState, stage) {
        val state = viewModel.operationState
        if (stage == RepairStage.RE_VALIDATING && state is OperationUiState.ValidateDone) {
            finalViolations = state.violations
            stage = RepairStage.RESULT
        }
    }

    Scaffold(topBar = { CromaTopBar("Repair Conflicts", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CromaWorkflowTags(active = "VALIDATE")
            when (stage) {
                RepairStage.REVIEW_ORIGINAL -> {
                    if (viewModel.operationState is OperationUiState.Running) {
                        LoadingRow("Checking current conflicts…")
                    } else if (originalViolations.isEmpty() && viewModel.operationState is OperationUiState.ValidateDone) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Text("No conflicts found — nothing to repair.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp))
                        }
                    } else if (originalViolations.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "${originalViolations.size} conflict(s) found. Repair preserves every session that's already valid and only recalculates the ones involved in a conflict.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(
                                    onClick = { stage = RepairStage.REPAIRING; viewModel.repair(runId, "dsatur") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                ) {
                                    Text("Repair Now")
                                }
                            }
                        }
                        ViolationList(originalViolations)
                    }
                }
                RepairStage.REPAIRING -> LoadingRow("Repairing conflicting sessions…")
                RepairStage.RE_VALIDATING -> LoadingRow("Re-validating the repaired schedule…")
                RepairStage.RESULT -> {
                    val clean = finalViolations.isEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (clean) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (clean) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (clean) "Repaired — 0 conflicts remaining" else "Repaired — ${finalViolations.size} conflict(s) remain",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Before: ${originalViolations.size} conflict(s)  →  After: ${finalViolations.size} conflict(s)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (finalViolations.isNotEmpty()) ViolationList(finalViolations)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { finalRunId?.let(onOptimize) }, modifier = Modifier.weight(1f)) { Text("Optimize Further") }
                        Button(onClick = { finalRunId?.let(onViewTimetable) }, modifier = Modifier.weight(1f)) { Text("View Timetable") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
