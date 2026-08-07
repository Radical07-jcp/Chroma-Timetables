package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

/**
 * [initialSessionType] preselects the type without skipping the prompt — Home's FAB (no run in
 * context) leaves this null so the dialog opens on CLASS by default; a future "regenerate for this
 * timetable" entry point could pass the source run's own type instead.
 */
@Composable
fun GenerateScreen(onBack: () -> Unit, onGenerated: (runId: String) -> Unit, initialSessionType: SessionTypeEntity? = null) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    var name by remember { mutableStateOf("New Schedule") }
    var sessionType by remember { mutableStateOf(initialSessionType ?: SessionTypeEntity.CLASS) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var algorithm by remember { mutableStateOf(viewModel.algorithmNames.firstOrNull { it == "dsatur" } ?: viewModel.algorithmNames.firstOrNull().orEmpty()) }
    var algorithmMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.operationState) {
        val state = viewModel.operationState
        if (state is OperationUiState.GenerateDone) {
            onGenerated(state.runId)
            viewModel.resetOperationState()
        }
    }

    if (showTypeDialog) {
        ScheduleTypePromptDialog(
            initial = sessionType,
            title = "Generate which schedule type?",
            body = "Only sessions of this type are colored into the new timetable — a Generate run " +
                "is always one schedule type, never a mix.",
            onConfirm = { sessionType = it; showTypeDialog = false },
            onDismiss = { showTypeDialog = false },
        )
    }

    Scaffold(topBar = { CromaTopBar("Generate Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Schedule name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedButton(onClick = { showTypeDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Schedule type: ${sessionType.label()}")
                    }

                    Box {
                        OutlinedButton(onClick = { algorithmMenuExpanded = true }) {
                            Text("Algorithm: ${algorithm.ifBlank { "dsatur" }}")
                        }
                        DropdownMenu(expanded = algorithmMenuExpanded, onDismissRequest = { algorithmMenuExpanded = false }) {
                            viewModel.algorithmNames.forEach { name ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { algorithm = name; algorithmMenuExpanded = false })
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
                enabled = state !is OperationUiState.Running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state is OperationUiState.Running) "Generating…" else "Generate ${sessionType.label()}")
            }

            if (state is OperationUiState.Failed) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
