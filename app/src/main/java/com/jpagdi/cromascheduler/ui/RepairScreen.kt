package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

@Composable
fun RepairScreen(runId: String, onBack: () -> Unit, onRepaired: (newRunId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    LaunchedEffect(runId) { viewModel.validate(runId) }
    LaunchedEffect(viewModel.operationState) {
        val state = viewModel.operationState
        if (state is OperationUiState.RepairDone) {
            onRepaired(state.newRunId)
            viewModel.resetOperationState()
        }
    }

    Scaffold(topBar = { CromaTopBar("Repair Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val state = viewModel.operationState) {
                is OperationUiState.ValidateDone -> {
                    if (state.violations.isEmpty()) {
                        Text("No conflicts found — nothing to repair.", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            "${state.violations.size} conflict(s) found. Repair preserves every session that's already valid and only recalculates the ones involved in a conflict.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = { viewModel.repair(runId, "dsatur") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Repair")
                        }
                    }
                }
                is OperationUiState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Working…")
                }
                is OperationUiState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}
