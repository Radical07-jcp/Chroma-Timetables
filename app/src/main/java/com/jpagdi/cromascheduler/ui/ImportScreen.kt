package com.jpagdi.cromascheduler.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.ImportUiState
import com.jpagdi.cromascheduler.viewmodel.ImportViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

/**
 * [initialSessionType] lets a caller pre-select the type (e.g. Timetable Detail's "Import more data
 * for this timetable" action passes that run's own type so the dialog opens already on the right
 * choice) — the user can still change it before confirming. Passed null from the drawer's plain
 * "Import Data" entry point, which has no run in context yet.
 */
@Composable
fun ImportScreen(onBack: () -> Unit, initialSessionType: SessionTypeEntity? = null) {
    val container = LocalAppContainer.current
    val viewModel: ImportViewModel = viewModel(factory = ViewModelFactory(container))
    val context = LocalContext.current

    // The type prompt gates BOTH pickers below — neither launcher fires until a type has been
    // confirmed, which is what makes this a real prompt rather than an optional afterthought.
    var confirmedType by remember { mutableStateOf(initialSessionType) }
    var showTypeDialog by remember { mutableStateOf(initialSessionType == null) }
    var pendingLaunch by remember { mutableStateOf<PendingLaunch?>(null) }

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val type = confirmedType
        if (uri != null && type != null) viewModel.importZip(context, uri, type)
    }
    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val type = confirmedType
        if (uris.isNotEmpty() && type != null) viewModel.importCsvFiles(context, uris, type)
    }

    if (showTypeDialog) {
        ScheduleTypePromptDialog(
            initial = confirmedType ?: SessionTypeEntity.CLASS,
            title = "What is this data for?",
            body = "Pick the schedule type this CSV/zip belongs to. Rows in sessions.csv that don't " +
                "match get skipped instead of imported, so a Class import can never quietly pull in " +
                "Meeting or Exam sessions (or vice-versa).",
            onConfirm = { type ->
                confirmedType = type
                showTypeDialog = false
                when (pendingLaunch) {
                    PendingLaunch.Zip -> zipLauncher.launch("application/zip")
                    PendingLaunch.Files -> filesLauncher.launch("text/*")
                    null -> Unit
                }
                pendingLaunch = null
            },
            onDismiss = { showTypeDialog = false; pendingLaunch = null },
        )
    }

    fun requestZip() {
        if (confirmedType == null) {
            pendingLaunch = PendingLaunch.Zip
            showTypeDialog = true
        } else {
            zipLauncher.launch("application/zip")
        }
    }
    fun requestFiles() {
        if (confirmedType == null) {
            pendingLaunch = PendingLaunch.Files
            showTypeDialog = true
        } else {
            filesLauncher.launch("text/*")
        }
    }

    Scaffold(topBar = { CromaTopBar("Import Data", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Import teachers, subjects, rooms, sections, sessions, and availability. " +
                    "Bring one zip with all six CSVs, or pick the files individually — either way, filenames decide what gets parsed.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssistChip(
                        onClick = { showTypeDialog = true },
                        label = { Text("Schedule type: ${(confirmedType ?: SessionTypeEntity.CLASS).label()}") },
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { requestZip() }) { Text("Choose zip") }
                        OutlinedButton(onClick = { requestFiles() }) { Text("Choose CSV files") }
                    }

                    when (val state = viewModel.uiState) {
                        is ImportUiState.Idle -> Unit
                        is ImportUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Importing…")
                        }
                        is ImportUiState.Failed -> Text(
                            "Import failed: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                        )
                        is ImportUiState.Done -> Unit
                    }
                }
            }

            val doneState = viewModel.uiState as? ImportUiState.Done
            if (doneState != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ImportResultView(doneState)
                    }
                }
            }
        }
    }
}

private enum class PendingLaunch { Zip, Files }

/** Human-readable label for a session type, shared by the import prompt and the generate prompt. */
fun SessionTypeEntity.label(): String = when (this) {
    SessionTypeEntity.CLASS -> "Class Schedule"
    SessionTypeEntity.EXAM -> "Examination Schedule"
    SessionTypeEntity.LAB -> "Laboratory Schedule"
    SessionTypeEntity.MEETING -> "Faculty Meeting Schedule"
    SessionTypeEntity.SEMINAR -> "Seminar Schedule"
}

/** Shared by Import and Generate — a single-choice dialog over the five [SessionTypeEntity] values. */
@Composable
fun ScheduleTypePromptDialog(
    initial: SessionTypeEntity,
    title: String,
    body: String,
    onConfirm: (SessionTypeEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(body, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                SessionTypeEntity.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(selected = selected == type, onClick = { selected = type })
                        Text(type.label())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ImportResultView(state: ImportUiState.Done) {
    val result = state.result
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Imported", style = MaterialTheme.typography.titleMedium)
        Text("Teachers: ${result.teacherCount}  •  Subjects: ${result.subjectCount}  •  Rooms: ${result.roomCount}")
        Text("Sections: ${result.sectionCount}  •  Sessions: ${result.sessionCount}  •  Availability blocks: ${result.availabilityBlockCount}")

        if (result.filesMissing.isNotEmpty()) {
            Text(
                "Not included in this import: ${result.filesMissing.joinToString(", ")}",
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (result.errors.isNotEmpty()) {
            Text("${result.errors.size} issue(s) found — rows with problems were skipped, everything else was saved:", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(result.errors) { error ->
                    Text("${error.fileName}${if (error.rowNumber > 0) " row ${error.rowNumber}" else ""}: ${error.message}", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Text("No issues found.", color = MaterialTheme.colorScheme.primary)
        }
    }
}
