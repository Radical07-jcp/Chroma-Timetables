package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun ValidateScreen(runId: String, onBack: () -> Unit, onRepair: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))

    LaunchedEffect(runId) { viewModel.validate(runId) }

    Scaffold(topBar = { CromaTopBar("Validate Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val state = viewModel.operationState) {
                is OperationUiState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Checking constraints…")
                }
                is OperationUiState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is OperationUiState.ValidateDone -> {
                    if (state.violations.isEmpty()) {
                        Text("No conflicts found — this schedule is valid.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("${state.violations.size} conflict(s) found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { onRepair(runId) }) { Text("Repair now") }
                        ViolationList(state.violations)
                    }
                }
                else -> Unit
            }
        }
    }
}
