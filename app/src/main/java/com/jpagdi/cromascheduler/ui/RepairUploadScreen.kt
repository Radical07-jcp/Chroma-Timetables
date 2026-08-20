package com.jpagdi.cromascheduler.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.CreateTimetableViewModel
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

/**
 * The "different feature where users upload schedules with conflict and our app will repair it"
 * entry point — deliberately separate from Generate, since an existing schedule being repaired
 * isn't something the engine builds, it's something a school already has. Shares the same
 * choose-type -> define-periods wizard steps Generate uses (via [wizard]) so the uploaded
 * schedule's real period times display correctly everywhere downstream, then lands on
 * [RepairWorkflowScreen] the moment the upload finishes, already validated — the same guided
 * teacher/room/class/subject/period repair workflow available from an existing timetable's
 * Timetable Actions, just starting from freshly-uploaded data instead of one already in the app.
 */
@Composable
fun RepairUploadScreen(wizard: CreateTimetableViewModel, onBack: () -> Unit, onImported: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))
    val context = LocalContext.current

    var name by remember { mutableStateOf("Uploaded Schedule") }
    val sessionType = wizard.sessionType

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && sessionType != null) {
            viewModel.importExistingSchedule(context, uri, name, sessionType, wizard.periodBlocks, wizard.activeDays)
        }
    }

    LaunchedEffect(viewModel.operationState) {
        val state = viewModel.operationState
        if (state is OperationUiState.ImportedForRepair) {
            onImported(state.runId)
            viewModel.resetOperationState()
            wizard.reset()
        }
    }

    Scaffold(topBar = { CromaTopBar("Repair a Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Upload an assignments.csv for a ${sessionType?.label() ?: "schedule"} that already exists — sessionId, dayOfWeek, periodIndex, and an optional roomId per row. Those sessions must already be imported. We'll check it against the periods you just defined and offer to fix any conflicts.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name this schedule") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { filePicker.launch("text/*") }, modifier = Modifier.fillMaxWidth(), enabled = sessionType != null) {
                        Text("Choose assignments.csv")
                    }
                }
            }

            when (val state = viewModel.operationState) {
                is OperationUiState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Importing and checking for conflicts…")
                }
                is OperationUiState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}
