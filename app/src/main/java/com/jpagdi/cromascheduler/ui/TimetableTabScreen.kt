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
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.ScheduleViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimetableTabScreen(onSelect: (ScheduleRunEntity) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(container))
    LaunchedEffect(Unit) { viewModel.loadRuns() }

    Scaffold(topBar = {
        Surface { Text("Timetable", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp)) }
    }) { padding ->
        if (viewModel.runs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No schedules yet — generate one from the Home tab.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(viewModel.runs) { run ->
                    Card(onClick = { onSelect(run) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(run.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${run.mode} • ${run.algorithmUsed} • ${dateFormat.format(Date(run.createdAtEpochMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
