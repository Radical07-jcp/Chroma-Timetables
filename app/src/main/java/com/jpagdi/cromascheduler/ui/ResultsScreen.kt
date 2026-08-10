package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.export.ScheduleExportRow
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.ResultsViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory
import kotlin.math.roundToInt

private enum class ResultsTab(val label: String) {
    WEEKLY("Weekly"), DAILY("Daily"), TEACHER("Teacher"), CLASS("Class"), SUBJECT("Subject"), PERIOD("Period"), ROOM("Room")
}

@Composable
fun ResultsScreen(runId: String, onBack: () -> Unit, onExport: (runId: String, runName: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ResultsViewModel = viewModel(factory = ViewModelFactory(container))
    LaunchedEffect(runId) { viewModel.load(runId) }

    var tab by remember { mutableStateOf(ResultsTab.WEEKLY) }

    Scaffold(
        topBar = { CromaTopBar(viewModel.run?.name ?: "Results", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onExport(runId, viewModel.run?.name ?: "Schedule") },
                text = { Text("Export") },
                icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                containerColor = com.jpagdi.cromascheduler.designsystem.CromaColors.Gold,
                contentColor = com.jpagdi.cromascheduler.designsystem.CromaColors.Navy,
            )
        },
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StatisticsCard(viewModel)

            ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 12.dp) {
                ResultsTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                }
            }

            when (tab) {
                ResultsTab.WEEKLY -> GroupedTimetable(viewModel.rows) { it.dayLabel }
                ResultsTab.DAILY -> DailyTimetable(viewModel.rows)
                ResultsTab.TEACHER -> GroupedTimetable(viewModel.rows) { it.teacherName }
                ResultsTab.CLASS -> GroupedTimetable(viewModel.rows) { it.sectionName }
                ResultsTab.SUBJECT -> GroupedTimetable(viewModel.rows) { it.subjectName }
                ResultsTab.PERIOD -> GroupedTimetable(viewModel.rows) { "${it.startTime}–${it.endTime}" }
                ResultsTab.ROOM -> GroupedTimetable(viewModel.rows) { it.roomName }
            }
        }
    }
}

@Composable
private fun StatisticsCard(viewModel: ResultsViewModel) {
    val stats = viewModel.statistics
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Statistics", style = MaterialTheme.typography.titleSmall)
            Text("Conflicts: ${stats.conflictCount}" + if (stats.conflictCount == 0) " (none)" else "", style = MaterialTheme.typography.bodyMedium)
            Text("Execution time: ${stats.executionTimeMillis} ms", style = MaterialTheme.typography.bodyMedium)
            Text("Room utilization: ${(stats.roomUtilization * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
            Text("Sessions scheduled: ${viewModel.rows.size}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GroupedTimetable(rows: List<ScheduleExportRow>, groupKey: (ScheduleExportRow) -> String) {
    val grouped = remember(rows) { rows.groupBy(groupKey) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        grouped.forEach { (key, groupRows) ->
            item {
                Text(key, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            items(groupRows) { row -> SessionRow(row) }
        }
    }
}

@Composable
private fun DailyTimetable(rows: List<ScheduleExportRow>) {
    val days = remember(rows) { rows.map { it.dayLabel }.distinct() }
    if (days.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No sessions scheduled.") }
        return
    }
    var selectedDay by remember(days) { mutableStateOf(days.first()) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = days.indexOf(selectedDay).coerceAtLeast(0)) {
            days.forEach { day ->
                Tab(selected = day == selectedDay, onClick = { selectedDay = day }, text = { Text(day) })
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(rows.filter { it.dayLabel == selectedDay }) { row -> SessionRow(row) }
        }
    }
}

@Composable
private fun SessionRow(row: ScheduleExportRow) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${row.subjectName} (${row.sessionType})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "${row.teacherName} • ${row.sectionName} • ${row.roomName}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("${row.dayLabel}, ${row.startTime}–${row.endTime}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
