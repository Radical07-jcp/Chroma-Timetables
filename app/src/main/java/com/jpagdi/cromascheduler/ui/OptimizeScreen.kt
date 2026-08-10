package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

private enum class OptimizeStage { READY, OPTIMIZING, RE_VALIDATING, RESULT }

/**
 * Reachable from a Timetable workspace (optimize an already-generated run) or from the end of
 * Repair (optimize further right after a repair). Either way, the produced run is re-validated
 * before this screen calls it done — same "auto-validate before shipping" rule Repair follows,
 * since optimizing (compacting schedules, reducing idle time/room changes) must never be allowed to
 * quietly reintroduce a hard-constraint conflict.
 */
@Composable
fun OptimizeScreen(runId: String, onBack: () -> Unit, onViewTimetable: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    var stage by remember { mutableStateOf(OptimizeStage.READY) }
    var finalRunId by remember { mutableStateOf<String?>(null) }
    var finalViolations by remember { mutableStateOf<List<ConstraintViolation>>(emptyList()) }

    LaunchedEffect(viewModel.operationState, stage) {
        when (val state = viewModel.operationState) {
            is OperationUiState.OptimizeDone -> {
                stage = OptimizeStage.RE_VALIDATING
                finalRunId = state.newRunId
                viewModel.validate(state.newRunId)
            }
            is OperationUiState.ValidateDone -> if (stage == OptimizeStage.RE_VALIDATING) {
                finalViolations = state.violations
                stage = OptimizeStage.RESULT
            }
            else -> Unit
        }
    }

    Scaffold(topBar = { CromaTopBar("Optimize Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (stage) {
                OptimizeStage.READY -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Improve schedule quality", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Reduces teacher idle time, room changes, and gaps in section schedules, and favors morning slots where possible — without ever violating a hard constraint. Produces a new saved timetable; this one stays untouched.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = { stage = OptimizeStage.OPTIMIZING; viewModel.optimize(runId) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            ) {
                                Text("Optimize Now")
                            }
                        }
                    }
                }
                OptimizeStage.OPTIMIZING -> LoadingLine("Optimizing…")
                OptimizeStage.RE_VALIDATING -> LoadingLine("Re-validating the optimized schedule…")
                OptimizeStage.RESULT -> {
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
                                if (clean) "Optimized — re-validated, 0 conflicts" else "Optimized — re-validated, ${finalViolations.size} conflict(s)",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    if (finalViolations.isNotEmpty()) ViolationList(finalViolations)
                    Button(onClick = { finalRunId?.let(onViewTimetable) }, modifier = Modifier.fillMaxWidth()) { Text("View Timetable") }
                }
            }
        }
    }
}

@Composable
private fun LoadingLine(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
