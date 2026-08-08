package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.CromaShapes
import com.jpagdi.cromascheduler.designsystem.CromaStatus
import com.jpagdi.cromascheduler.designsystem.DashboardCard
import com.jpagdi.cromascheduler.designsystem.StatusPill
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.RunWithConflicts
import com.jpagdi.cromascheduler.viewmodel.TimetableWorkspaceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The hub for one schedule type. Import Data and Generate Schedule live here — scoped to
 * [sessionType], no type picker anywhere in either flow — alongside Validate / Optimize / View /
 * Export for whichever run is currently latest, and a history list for anything generated before
 * that. This screen is what used to be split across a global Home Dashboard plus a separate
 * "pick a run" screen for Validate/Repair/Export; collapsing it into one place scoped by type is
 * the actual restructuring the "generate and import buttons must be within the timetable details"
 * request was asking for.
 */
@Composable
fun TimetableWorkspaceScreen(
    sessionType: SessionTypeEntity,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onGenerate: () -> Unit,
    onValidate: (runId: String) -> Unit,
    onOptimize: (runId: String) -> Unit,
    onResults: (runId: String) -> Unit,
    onExport: (runId: String, runName: String) -> Unit,
    onDefinePeriods: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: TimetableWorkspaceViewModel = viewModel(
        factory = TimetableWorkspaceViewModel.factory(container.scheduleRepository, sessionType),
    )
    LaunchedEffect(Unit) { viewModel.load() }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { CromaTopBar(sessionType.label(), onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (viewModel.periodsConfigured == false) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Periods not set up yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Generate is disabled until periods are defined.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            TextButton(onClick = onDefinePeriods) { Text("Set up") }
                        }
                    }
                }
            }

            item {
                DashboardCard(
                    title = "Import Data",
                    subtitle = "Teachers, subjects, rooms, sections, sessions, availability",
                    icon = Icons.Filled.UploadFile,
                    accent = CromaAccents.Blue,
                    onClick = onImport,
                )
            }
            item {
                DashboardCard(
                    title = "Generate Schedule",
                    subtitle = "Run DSATUR (or another algorithm) against imported data",
                    icon = Icons.Filled.AutoAwesome,
                    accent = CromaAccents.Gold,
                    onClick = onGenerate,
                )
            }

            val latest = viewModel.runs.firstOrNull()
            if (latest != null) {
                item {
                    Text("Latest run", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                }
                item { LatestRunCard(latest, onValidate, onOptimize, onResults, onExport) }
            }

            val history = viewModel.runs.drop(1)
            if (history.isNotEmpty()) {
                item { Text("Earlier runs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(history, key = { it.run.id }) { entry ->
                    HistoryRow(entry, onClick = { onResults(entry.run.id) }, onDelete = { pendingDeleteId = entry.run.id })
                }
            }

            if (viewModel.runs.isEmpty()) {
                item {
                    Text(
                        "No ${sessionType.label().lowercase()} generated yet. Import your data above, then tap Generate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }

    val deleteId = pendingDeleteId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete this run?") },
            text = { Text("This removes the generated schedule and its conflict history. Imported data is not affected.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteRun(deleteId); pendingDeleteId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LatestRunCard(
    entry: RunWithConflicts,
    onValidate: (String) -> Unit,
    onOptimize: (String) -> Unit,
    onResults: (String) -> Unit,
    onExport: (String, String) -> Unit,
) {
    val run = entry.run
    Card(shape = CromaShapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(run.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${run.algorithmUsed} • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(run.createdAtEpochMillis))} • ${run.executionTimeMillis} ms",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (entry.conflictCount > 0) StatusPill("${entry.conflictCount} CONFLICTS", CromaStatus.Conflicts)
                else StatusPill("CLEAN", CromaStatus.Clean)
            }

            Button(onClick = { onResults(run.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Timetable")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onValidate(run.id) }, modifier = Modifier.weight(1f)) { Text("Validate") }
                OutlinedButton(onClick = { onOptimize(run.id) }, modifier = Modifier.weight(1f)) { Text("Optimize") }
                OutlinedButton(onClick = { onExport(run.id, run.name) }, modifier = Modifier.weight(1f)) { Text("Export") }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: RunWithConflicts, onClick: () -> Unit, onDelete: () -> Unit) {
    val run = entry.run
    Card(onClick = onClick, shape = CromaShapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(run.name, style = MaterialTheme.typography.bodyMedium)
                Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(run.createdAtEpochMillis)), style = MaterialTheme.typography.labelSmall)
            }
            if (entry.conflictCount > 0) StatusPill("${entry.conflictCount}", CromaStatus.Conflicts) else StatusPill("OK", CromaStatus.Clean)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}
