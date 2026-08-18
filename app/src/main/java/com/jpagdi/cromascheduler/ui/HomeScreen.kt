package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.designsystem.CromaStatus
import com.jpagdi.cromascheduler.designsystem.LocalButtonAccent
import com.jpagdi.cromascheduler.designsystem.StatusPill
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.HomeViewModel
import com.jpagdi.cromascheduler.viewmodel.TimetableRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenTimetable: (runId: String) -> Unit,
    onCreateTimetable: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container.scheduleRepository))
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { CromaHomeHeader(onOpenDrawer) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTimetable,
                icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                text = { Text("New timetable") },
                containerColor = LocalButtonAccent.current,
                contentColor = com.jpagdi.cromascheduler.designsystem.AccentPrefs.textColorFor(LocalButtonAccent.current),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
            )
        },
    ) { padding ->
        if (!viewModel.loaded) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else if (viewModel.rows.isEmpty()) {
            EmptyHome(
                modifier = Modifier.fillMaxSize().padding(padding),
                onCreate = onCreateTimetable,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CromaWorkflowTags(active = "PLAN")
                }
                item {
                    Column(
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Your timetables", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${viewModel.rows.size} saved schedule${if (viewModel.rows.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(viewModel.rows, key = { it.run.id }) { row ->
                    TimetableRowCard(row, onClick = { onOpenTimetable(row.run.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Build your first timetable", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        FormalBodyText(
            "Set the schedule type and periods, import your data, then let Chroma generate a conflict-aware timetable on this device.",
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(containerColor = LocalButtonAccent.current),
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create timetable")
        }
    }
}

@Composable
private fun TimetableRowCard(row: TimetableRow, onClick: () -> Unit) {
    val run = row.run
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(run.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        run.sessionType.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Periods", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(run.periodBlocksDisplay(), style = MaterialTheme.typography.bodyMedium)
                }
                if (row.conflictCount > 0) {
                    StatusPill("${row.conflictCount} CONFLICT${if (row.conflictCount == 1) "" else "S"}", CromaStatus.Conflicts)
                } else {
                    StatusPill("CLEAN", CromaStatus.Clean)
                }
            }

            Text(
                "Generated ${SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(run.createdAtEpochMillis))}  •  ${run.algorithmUsed}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ScheduleRunEntity.periodBlocksDisplay(): String =
    com.jpagdi.cromascheduler.data.entity.PeriodBlock.decodeList(periodBlocksEncoded).summary()
