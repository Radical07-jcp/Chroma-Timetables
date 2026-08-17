package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.designsystem.CromaStatus
import com.jpagdi.cromascheduler.designsystem.StatusPill
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.LineageEntry
import com.jpagdi.cromascheduler.viewmodel.TimetableDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One TIMETABLE's whole lineage, scrollable — the original Generate run plus every Validate /
 * Repair / Optimize built from it render as entries in one timeline here, instead of Validate or
 * Optimize spawning a separate Home card each time (that was the actual bug: "(optimized)
 * (optimized) (optimized)" cluttering Home when it's really one timetable's history).
 *
 * Teachers / Validate / Optimize / Export live in the bottom bar and act on [viewModel.latest] —
 * the most recent entry — since that's what "this timetable" means once history exists. Export
 * still needs to know WHICH entry, so its bottom-bar action opens a picker over [entries] rather
 * than assuming latest is always what someone wants to hand out.
 */
@Composable
fun TimetableDetailScreen(
    runId: String,
    onBack: () -> Unit,
    onResults: (runId: String) -> Unit,
    onExport: (runId: String, runName: String) -> Unit,
    onTeachers: () -> Unit,
    onDeleted: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: TimetableDetailViewModel = viewModel(factory = TimetableDetailViewModel.factory(container.scheduleRepository, runId))
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(viewModel.deleted) { if (viewModel.deleted) onDeleted() }

    var confirmDelete by remember { mutableStateOf(false) }
    var showExportPicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val root = viewModel.root

    Scaffold(
        topBar = { CromaTopBar(root?.name ?: "Timetable", onBack) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    enabled = viewModel.latest != null,
                    onClick = { viewModel.latest?.run?.id?.let(onResults) },
                    icon = { Icon(Icons.Filled.Visibility, contentDescription = null) },
                    label = { Text("Views") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onTeachers,
                    icon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                    label = { Text("Teachers") },
                )
                NavigationBarItem(
                    selected = false,
                    enabled = !viewModel.busy && viewModel.latest != null,
                    onClick = viewModel::validateLatest,
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("Validate") },
                )
                NavigationBarItem(
                    selected = false,
                    enabled = !viewModel.busy && viewModel.latest != null,
                    onClick = viewModel::optimizeLatest,
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    label = { Text("Optimize") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showMore = true },
                    icon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                    label = { Text("More") },
                )
            }
        },
    ) { padding ->
        if (!viewModel.loaded || root == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(root.sessionType.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(PeriodBlock.decodeList(root.periodBlocksEncoded).summary(), style = MaterialTheme.typography.bodySmall)
                    }
                    if (viewModel.busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            items(viewModel.entries) { entry ->
                LineageEntryCard(entry = entry, onView = { onResults(entry.run.id) })
            }

        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this timetable?") },
            text = { Text("This removes the generated schedule AND its full Validate/Repair/Optimize history. Imported teacher/subject/room/session data is not affected.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    if (showMore) {
        AlertDialog(
            onDismissRequest = { showMore = false },
            title = { Text("Timetable actions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showMore = false; showExportPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export a version")
                    }
                    TextButton(
                        onClick = { showMore = false; viewModel.repairLatest() },
                        enabled = !viewModel.busy && viewModel.latest != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Repair latest schedule")
                    }
                    TextButton(
                        onClick = { showMore = false; confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete timetable") }
                }
            },
            confirmButton = { TextButton(onClick = { showMore = false }) { Text("Close") } },
        )
    }

    if (showExportPicker) {
        AlertDialog(
            onDismissRequest = { showExportPicker = false },
            title = { Text("Export which version?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    viewModel.entries.reversed().forEach { entry ->
                        TextButton(
                            onClick = {
                                showExportPicker = false
                                onExport(entry.run.id, entry.run.name)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(entry.run.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExportPicker = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LineageEntryCard(entry: LineageEntry, onView: () -> Unit) {
    val run = entry.run
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onView,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(run.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (entry.conflictCount > 0) StatusPill("${entry.conflictCount} CONFLICTS", CromaStatus.Conflicts)
                else StatusPill("CLEAN", CromaStatus.Clean)
            }
            Text(
                "${run.algorithmUsed} • ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(run.createdAtEpochMillis))}",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onView) {
                Icon(Icons.Filled.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Timetable")
            }
        }
    }
}
