package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

/**
 * Generating a schedule is never a silent, unchecked action: ScheduleRepository.generate() already
 * runs every hard constraint through the engine as part of coloring and persists the resulting
 * conflict count on the SAME run it just created — that's the "validation before generating"
 * behavior. What was actually missing before wasn't the validation itself, it was ever SHOWING it:
 * this screen now displays that conflict count immediately, on this screen, before handing off to
 * the Timetable workspace, so it's visibly the result of generating rather than something that
 * only showed up if you separately went and tapped Validate afterward.
 */
@Composable
fun GenerateScreen(sessionType: SessionTypeEntity, onBack: () -> Unit, onDefinePeriods: () -> Unit, onDone: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    var name by remember { mutableStateOf("${sessionType.label()} — ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date())}") }
    var algorithm by remember { mutableStateOf(viewModel.algorithmNames.firstOrNull { it == "dsatur" } ?: viewModel.algorithmNames.firstOrNull().orEmpty()) }
    var algorithmMenuExpanded by remember { mutableStateOf(false) }
    var periodsConfigured by remember { mutableStateOf<Boolean?>(null) }
    var generatedRunId by remember { mutableStateOf<String?>(null) }
    var conflictCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { periodsConfigured = container.scheduleRepository.isPeriodConfigured() }

    LaunchedEffect(viewModel.operationState) {
        val state = viewModel.operationState
        if (state is OperationUiState.GenerateDone) {
            generatedRunId = state.runId
            conflictCount = container.scheduleRepository.getConflicts(state.runId).size
        }
    }

    Scaffold(topBar = { CromaTopBar("Generate ${sessionType.label()}", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                periodsConfigured == false -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Set up your periods first", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Generate needs to know your actual bell schedule — period count, length, and which days run — before it can place sessions. This only takes a minute and is a one-time setup.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = onDefinePeriods, modifier = Modifier.fillMaxWidth()) { Text("Define Periods") }
                        }
                    }
                }
                generatedRunId != null -> {
                    val clean = conflictCount == 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (clean) "Generated — automatically validated, 0 conflicts" else "Generated — automatically validated, ${conflictCount ?: 0} conflict(s) found",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (clean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            if (!clean) {
                                Text(
                                    "This can happen when the data itself has an unavoidable overlap (e.g. a teacher double-booked in the source data). Open this timetable's Validate action to see exactly which sessions conflict.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Button(onClick = { onDone(generatedRunId!!) }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Schedule name") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Box {
                                OutlinedButton(onClick = { algorithmMenuExpanded = true }) {
                                    Text("Algorithm: ${algorithm.ifBlank { "dsatur" }}")
                                }
                                DropdownMenu(expanded = algorithmMenuExpanded, onDismissRequest = { algorithmMenuExpanded = false }) {
                                    viewModel.algorithmNames.forEach { n ->
                                        DropdownMenuItem(text = { Text(n) }, onClick = { algorithm = n; algorithmMenuExpanded = false })
                                    }
                                }
                            }
                            Text(
                                "DSATUR is the default — it adapts as it colors and generally produces the fewest wasted timeslots. Greedy and Welsh-Powell are here for comparison.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    val state = viewModel.operationState
                    Button(
                        onClick = { viewModel.generate(name, sessionType, algorithm.ifBlank { "dsatur" }) },
                        enabled = periodsConfigured == true && state !is OperationUiState.Running,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state is OperationUiState.Running) "Generating…" else "Generate ${sessionType.label()}")
                    }
                    if (state is OperationUiState.Failed) Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
