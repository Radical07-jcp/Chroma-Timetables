package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

private enum class OptimizeStage { READY, OPTIMIZING, RE_VALIDATING, RESULT }

/**
 * Optimization is deliberately presented as "repair what changed", not "generate again".
 * The default strategy preserves the submitted timetable and searches for the smallest legal
 * set of local moves/swaps. A change budget prevents an accidental full reshuffle.
 */
@Composable
fun OptimizeScreen(runId: String, onBack: () -> Unit, onViewTimetable: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ViewModelFactory(container)
    )

    var stage by remember { mutableStateOf(OptimizeStage.READY) }
    var maxChanges by remember { mutableIntStateOf(12) }
    var outcome by remember { mutableStateOf<com.jpagdi.cromascheduler.data.repository.ScheduleRepository.OptimizationOutcome?>(null) }
    var finalViolations by remember { mutableStateOf<List<com.jpagdi.cromascheduler.engine.validation.ConstraintViolation>>(emptyList()) }

    LaunchedEffect(viewModel.operationState, stage) {
        when (val state = viewModel.operationState) {
            is OperationUiState.OptimizeDone -> {
                outcome = state.outcome
                stage = OptimizeStage.RE_VALIDATING
                viewModel.validate(state.outcome.runId)
            }
            is OperationUiState.ValidateDone -> if (stage == OptimizeStage.RE_VALIDATING) {
                finalViolations = state.violations
                stage = OptimizeStage.RESULT
            }
            else -> Unit
        }
    }

    Scaffold(topBar = { CromaTopBar("Optimize Schedule", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { CromaWorkflowTags(active = "OPTIMIZE") }

            when (stage) {
                OptimizeStage.READY -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Repair, don't rebuild", style = MaterialTheme.typography.headlineSmall)
                                FormalBodyText(
                                    "Keep the timetable that was already handed in. Chroma will first repair hard conflicts caused by changed teacher availability or time preferences, then look for small quality improvements.",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                FormalBodyText(
                                    "Teacher and subject assignments stay intact. When two people can exchange slots, Chroma can perform a direct swap instead of reshuffling the school.",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("Change budget", style = MaterialTheme.typography.titleLarge)
                                FormalBodyText(
                                    "Maximum number of assignment moves. Lower values protect the submitted schedule more aggressively.",
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf(4, 8, 12, 20).forEach { budget ->
                                        FilterChip(
                                            selected = maxChanges == budget,
                                            onClick = { maxChanges = budget },
                                            label = { Text("$budget") },
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        stage = OptimizeStage.OPTIMIZING
                                        viewModel.optimize(runId, maxChanges)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Repair & Optimize")
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "The original timetable is never modified. Optimization creates a new version in its history.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OptimizeStage.OPTIMIZING -> item { LoadingLine("Finding minimal changes…") }
                OptimizeStage.RE_VALIDATING -> item { LoadingLine("Re-validating every assignment…") }

                OptimizeStage.RESULT -> {
                    val result = outcome
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (finalViolations.isEmpty())
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer,
                                contentColor = if (finalViolations.isEmpty())
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    if (finalViolations.isEmpty()) "Optimization complete" else "Optimization needs review",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                FormalBodyText(
                                    if (finalViolations.isEmpty())
                                        "The resulting timetable was re-validated with no hard conflicts."
                                    else
                                        "${finalViolations.size} conflict(s) remain. Nothing was silently accepted.",
                                    color = if (finalViolations.isEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    if (result != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                MetricCard("Assignments changed", result.changes.toString(), Modifier.weight(1f))
                                MetricCard("Swaps", result.swaps.toString(), Modifier.weight(1f))
                            }
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Text("What Chroma did", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "Conflicts before: ${result.violationsBefore}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        "Conflicts after: ${result.violationsAfter}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    FormalBodyText(
                                        if (result.changes == 0)
                                            "No change was necessary. The existing timetable was already the best result found within the selected budget."
                                        else
                                            "${result.changes} assignment move(s) were made${if (result.swaps > 0) ", including ${result.swaps} direct swap(s)" else ""}.",
                                    )
                                }
                            }
                        }
                    }
                    if (finalViolations.isNotEmpty()) {
                        item { ViolationList(finalViolations) }
                    }
                    item {
                        Button(
                            onClick = { outcome?.runId?.let(onViewTimetable) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("View Optimized Timetable") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingLine(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
