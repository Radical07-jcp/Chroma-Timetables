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
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.OperationUiState
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

/**
 * The "different feature where users upload schedules with conflict and our app will repair it"
 * entry point — deliberately separate from any single Timetable workspace, since an existing
 * schedule being repaired isn't something the engine generated, it's something a school already
 * has. Lands on [RepairScreen] the moment the upload finishes, already validated.
 */
@Composable
fun RepairUploadScreen(onBack: () -> Unit, onImported: (runId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))
    val context = LocalContext.current

    var name by remember { mutableStateOf("Uploaded Schedule") }
    var sessionType by remember { mutableStateOf(SessionTypeEntity.CLASS) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importExistingSchedule(context, uri, name, sessionType)
    }

    LaunchedEffect(viewModel.operationState) {
        val state = viewModel.operationState
        if (state is OperationUiState.ImportedForRepair) {
            onImported(state.runId)
            viewModel.resetOperationState()
        }
    }

    Scaffold(topBar = { CromaTopBar("Repair a Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Upload an assignments.csv for a schedule that already exists — sessionId, dayOfWeek, periodIndex, and an optional roomId per row. The sessions themselves must already be imported for the type you pick below. We'll check it for conflicts and offer to fix them.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name this schedule") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Box {
                        OutlinedButton(onClick = { typeMenuExpanded = true }) {
                            Text("Schedule type: ${sessionType.label()}")
                        }
                        DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                            SessionTypeEntity.entries.forEach { type ->
                                DropdownMenuItem(text = { Text(type.label()) }, onClick = { sessionType = type; typeMenuExpanded = false })
                            }
                        }
                    }

                    Button(onClick = { filePicker.launch("text/*") }, modifier = Modifier.fillMaxWidth()) {
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
