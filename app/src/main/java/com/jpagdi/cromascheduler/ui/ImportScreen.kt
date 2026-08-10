package com.jpagdi.cromascheduler.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * [sessionType] is fixed by which Timetable workspace this was opened from — there's no picker
 * here anymore. That's the actual fix for "don't mix schedule types": the type isn't a choice made
 * mid-import, it's already decided by where you navigated from, so there's nothing to get wrong.
 * CsvImportService still enforces it row-by-row against sessions.csv's own `type` column underneath.
 */
@Composable
fun ImportScreen(sessionType: SessionTypeEntity, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ImportViewModel = viewModel(factory = ViewModelFactory(container))
    val context = LocalContext.current

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importZip(context, uri, sessionType)
    }
    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) viewModel.importCsvFiles(context, uris, sessionType)
    }

    Scaffold(topBar = { CromaTopBar("Import ${sessionType.label()} Data", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Bring teachers.csv, subjects.csv, rooms.csv, sections.csv, sessions.csv, and availability.csv — one zip with all six, or pick the files individually. Any session row that isn't ${sessionType.label()} gets skipped rather than imported, so this stays a clean ${sessionType.label()} import.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { zipLauncher.launch("application/zip") }) { Text("Choose zip") }
                        OutlinedButton(onClick = { filesLauncher.launch("text/*") }) { Text("Choose CSV files") }
                    }

                    when (val state = viewModel.uiState) {
                        is ImportUiState.Idle -> Unit
                        is ImportUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Importing…")
                        }
                        is ImportUiState.Failed -> Text("Import failed: ${state.message}", color = MaterialTheme.colorScheme.error)
                        is ImportUiState.Done -> Unit
                    }
                }
            }

            val doneState = viewModel.uiState as? ImportUiState.Done
            if (doneState != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Box(modifier = Modifier.padding(16.dp)) { ImportResultView(doneState) }
                }
            }
        }
    }
}

@Composable
private fun ImportResultView(state: ImportUiState.Done) {
    val result = state.result
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Imported", style = MaterialTheme.typography.titleMedium)
        Text("Teachers: ${result.teacherCount}  •  Subjects: ${result.subjectCount}  •  Rooms: ${result.roomCount}")
        Text("Sections: ${result.sectionCount}  •  Sessions: ${result.sessionCount}  •  Availability blocks: ${result.availabilityBlockCount}")

        if (result.filesMissing.isNotEmpty()) {
            Text("Not included in this import: ${result.filesMissing.joinToString(", ")}", color = MaterialTheme.colorScheme.error)
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
