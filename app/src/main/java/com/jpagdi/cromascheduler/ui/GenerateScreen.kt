package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

@Composable
fun GenerateScreen(onBack: () -> Unit, onGenerated: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    var name by remember { mutableStateOf("New Schedule") }
    var isExamMode by remember { mutableStateOf(false) }
    var algorithm by remember { mutableStateOf(viewModel.algorithmNames.firstOrNull { it == "dsatur" } ?: viewModel.algorithmNames.firstOrNull().orEmpty()) }
    var algorithmMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.operationState) {
        val state = viewModel.operationState
        if (state is OperationUiState.GenerateDone) {
            onGenerated(state.runId)
            viewModel.resetOperationState()
        }
    }

    Scaffold(topBar = { CromaTopBar("Generate Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule name") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isExamMode, onCheckedChange = { isExamMode = it })
                Spacer(Modifier.width(8.dp))
                Text(if (isExamMode) "Examination schedule (EXAM sessions only)" else "Regular schedule (all sessions)")
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

            val state = viewModel.operationState
            Button(
                onClick = { viewModel.generate(name, isExamMode, algorithm.ifBlank { "dsatur" }) },
                enabled = state !is OperationUiState.Running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state is OperationUiState.Running) "Generating…" else "Generate")
            }

            if (state is OperationUiState.Failed) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
