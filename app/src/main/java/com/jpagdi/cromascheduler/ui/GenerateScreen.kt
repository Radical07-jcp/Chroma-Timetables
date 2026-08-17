package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.CreateTimetableViewModel
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Step 3 of the New Timetable wizard — type (step 1) and periods (step 2) are already decided and
 * live on [wizard]; this screen only adds a name and algorithm, then generates. Automatic
 * validation happens as part of ScheduleRepository.generate() itself; this screen shows that
 * result immediately rather than requiring a separate trip to Validate afterward.
 */
@Composable
fun GenerateScreen(wizard: CreateTimetableViewModel, onBack: () -> Unit, onImportData: () -> Unit, onDone: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))
    val sessionType = wizard.sessionType ?: return

    var name by remember { mutableStateOf("${sessionType.label()} — ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date())}") }
    var algorithm by remember { mutableStateOf<String?>(null) }
    var algorithmMenuExpanded by remember { mutableStateOf(false) }
    var sessionCount by remember { mutableStateOf<Int?>(null) }
    var generatedRunId by remember { mutableStateOf<String?>(null) }
    var conflictCount by remember { mutableStateOf<Int?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sessionCount = container.scheduleRepository.sessionCountFor(sessionType)
        algorithm = container.appPreferencesStore.defaultAlgorithmName.firstOrNull()
    }

    DisposableEffect(lifecycleOwner, sessionType) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // ImportScreen writes directly to the repository. Refresh when returning so the
                // Generate button becomes available immediately after a successful import.
                scope.launch {
                    sessionCount = container.scheduleRepository.sessionCountFor(sessionType)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                sessionCount == 0 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("No ${sessionType.label().lowercase()} data yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Import teachers, subjects, rooms, sections, and sessions for this type before generating.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = onImportData,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            ) { Text("Import Data") }
                        }
                    }
                }
                generatedRunId != null -> {
                    val clean = conflictCount == 0
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
                                if (clean) "Generated — automatically validated, 0 conflicts" else "Generated — automatically validated, ${conflictCount ?: 0} conflict(s) found",
                                style = MaterialTheme.typography.titleMedium,
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Schedule name") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Box {
                                OutlinedButton(onClick = { algorithmMenuExpanded = true }) {
                                    Text("Algorithm: ${algorithm ?: "dsatur"}")
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
                        onClick = { viewModel.generate(name, sessionType, wizard.periodBlocks, wizard.activeDays, algorithm ?: "dsatur") },
                        enabled = state !is OperationUiState.Running,
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
