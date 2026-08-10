package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.designsystem.CromaShapes
import com.jpagdi.cromascheduler.designsystem.CromaStatus
import com.jpagdi.cromascheduler.designsystem.StatusPill
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.HomeViewModel
import com.jpagdi.cromascheduler.viewmodel.TimetableRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every generated Timetable, one row per ScheduleRunEntity, newest first — this is Home now,
 * full stop, not a list of 3 schedule-type categories. Creating a new one (the FAB) is a wizard:
 * pick a type, define THAT timetable's own periods, then generate — see CreateTimetableTypeScreen /
 * CreateTimetablePeriodsScreen / GenerateScreen. Tapping an existing row opens TimetableDetailScreen.
 */
@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, onOpenTimetable: (runId: String) -> Unit, onCreateTimetable: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container.scheduleRepository))
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { CromaHomeHeader(onOpenDrawer) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTimetable,
                icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                text = { Text("New Timetable") },
                containerColor = com.jpagdi.cromascheduler.designsystem.CromaColors.Gold,
                contentColor = com.jpagdi.cromascheduler.designsystem.CromaColors.Navy,
            )
        },
    ) { padding ->
        if (!viewModel.loaded) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (viewModel.rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No timetables yet. Tap \"New Timetable\" to pick a schedule type, define its periods, and generate your first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(viewModel.rows, key = { it.run.id }) { row ->
                    TimetableRowCard(row, onClick = { onOpenTimetable(row.run.id) })
                }
            }
        }
    }
}

@Composable
private fun TimetableRowCard(row: TimetableRow, onClick: () -> Unit) {
    val run = row.run
    Card(
        onClick = onClick,
        shape = CromaShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(run.name, style = MaterialTheme.typography.titleMedium)
                    Text(run.sessionType.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (row.conflictCount > 0) StatusPill("${row.conflictCount} CONFLICT${if (row.conflictCount == 1) "" else "S"}", CromaStatus.Conflicts)
                else StatusPill("CLEAN", CromaStatus.Clean)
            }
            InfoLine("Periods", run.periodBlocksDisplay())
            InfoLine("Algorithm", run.algorithmUsed)
            InfoLine("Generated", SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(run.createdAtEpochMillis)))
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/** Decodes just for display — Home shouldn't need a repository round trip just to show what periods a run used. */
private fun ScheduleRunEntity.periodBlocksDisplay(): String =
    com.jpagdi.cromascheduler.data.entity.PeriodBlock.decodeList(periodBlocksEncoded).summary()
