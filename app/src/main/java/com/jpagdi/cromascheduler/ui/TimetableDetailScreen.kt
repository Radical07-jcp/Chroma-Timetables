package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
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
import com.jpagdi.cromascheduler.viewmodel.TimetableDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One generated Timetable's detail page — Import/Generate don't live here anymore (those only
 * happen once, at creation time, via the New Timetable wizard); this is Validate / Optimize /
 * View / Export / Delete for a run that already exists.
 */
@Composable
fun TimetableDetailScreen(
    runId: String,
    onBack: () -> Unit,
    onValidate: (runId: String) -> Unit,
    onOptimize: (runId: String) -> Unit,
    onResults: (runId: String) -> Unit,
    onExport: (runId: String, runName: String) -> Unit,
    onDeleted: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: TimetableDetailViewModel = viewModel(factory = TimetableDetailViewModel.factory(container.scheduleRepository, runId))
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(viewModel.deleted) { if (viewModel.deleted) onDeleted() }

    var confirmDelete by remember { mutableStateOf(false) }
    val run = viewModel.run

    Scaffold(topBar = { CromaTopBar(run?.name ?: "Timetable", onBack) }) { padding ->
        if (!viewModel.loaded || run == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(run.sessionType.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(PeriodBlock.decodeList(run.periodBlocksEncoded).summary(), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${run.algorithmUsed} • ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(run.createdAtEpochMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (viewModel.conflictCount > 0) StatusPill("${viewModel.conflictCount} CONFLICTS", CromaStatus.Conflicts)
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

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete Timetable")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this timetable?") },
            text = { Text("This removes the generated schedule and its conflict history. Imported teacher/subject/room/session data is not affected.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
