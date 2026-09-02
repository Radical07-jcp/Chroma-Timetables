package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.RepairDimension
import com.jpagdi.cromascheduler.viewmodel.RepairWorkflowStep
import com.jpagdi.cromascheduler.viewmodel.RepairWorkflowViewModel

/**
 * The guided manual Repair workflow: pick what kind of thing you're adjusting (Teacher, Room,
 * Class, Subject, or Period), pick which specific ones, preview their current schedule, then
 * tap a field to see and pick from what else is available to swap it with. Nothing here
 * requires a flagged conflict to exist first — a voluntary swap two teachers want, with nothing
 * "wrong" in the validator's eyes, is exactly what this is for.
 */
@Composable
fun RepairWorkflowScreen(
    runId: String,
    onBack: () -> Unit,
    onSaved: (newRunId: String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: RepairWorkflowViewModel = viewModel(factory = RepairWorkflowViewModel.factory(container.scheduleRepository, runId))

    LaunchedEffect(runId) { viewModel.load() }
    LaunchedEffect(viewModel.saved) { if (viewModel.saved) viewModel.savedRunId?.let(onSaved) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(topBar = { CromaTopBar("Repair Schedule", onBack) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CromaWorkflowTags(active = "VALIDATE")

            if (!viewModel.loaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            when (viewModel.step) {
                RepairWorkflowStep.PICK_TYPE -> PickTypeStep(viewModel)
                RepairWorkflowStep.PICK_ENTITIES -> PickEntitiesStep(viewModel)
                RepairWorkflowStep.PREVIEW -> PreviewStep(viewModel)
            }
        }
    }
}

@Composable
private fun PickTypeStep(viewModel: RepairWorkflowViewModel) {
    Text(
        "What do you want to adjust? Choose the kind of thing involved — for example, pick Teacher if two or more teachers want to swap periods, rooms, classes, or subjects.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(RepairDimension.entries) { d ->
            val icon: ImageVector = when (d) {
                RepairDimension.TEACHER -> Icons.Filled.Person
                RepairDimension.ROOM -> Icons.Filled.MeetingRoom
                RepairDimension.CLASS -> Icons.Filled.Groups
                RepairDimension.SUBJECT -> Icons.Filled.MenuBook
                RepairDimension.PERIOD -> Icons.Filled.Schedule
            }
            val description = when (d) {
                RepairDimension.TEACHER -> "Two or more teachers need to trade time, rooms, classes, or subjects."
                RepairDimension.ROOM -> "Two or more rooms are involved — classes need to trade rooms."
                RepairDimension.CLASS -> "Two or more classes/sections need adjusting against each other."
                RepairDimension.SUBJECT -> "Two or more subjects need adjusting against each other."
                RepairDimension.PERIOD -> "Look at one or more specific time periods and adjust who's in them."
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.pickDimension(d)
                },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(d.label, style = MaterialTheme.typography.titleMedium)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PickEntitiesStep(viewModel: RepairWorkflowViewModel) {
    val dimension = viewModel.dimension
    Text(
        if (dimension == RepairDimension.PERIOD)
            "Choose one or more time periods to review and adjust."
        else
            "Choose two or more ${dimension.label.lowercase()}s that are involved. Everything else on the timetable stays exactly as it is.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (dimension) {
            RepairDimension.TEACHER -> viewModel.teachers.forEach { t ->
                EntityRow(t.name, t.id in viewModel.selectedEntityIds) { viewModel.toggleEntity(t.id) }
            }
            RepairDimension.ROOM -> viewModel.rooms.forEach { r ->
                EntityRow(r.name, r.id in viewModel.selectedEntityIds) { viewModel.toggleEntity(r.id) }
            }
            RepairDimension.CLASS -> viewModel.sections.forEach { s ->
                EntityRow(s.name, s.id in viewModel.selectedEntityIds) { viewModel.toggleEntity(s.id) }
            }
            RepairDimension.SUBJECT -> viewModel.subjects.forEach { s ->
                EntityRow(s.name, s.id in viewModel.selectedEntityIds) { viewModel.toggleEntity(s.id) }
            }
            RepairDimension.PERIOD -> viewModel.periods
                .sortedWith(compareBy({ it.dayOfWeek }, { it.periodIndex }))
                .forEach { p ->
                    val key = p.dayOfWeek to p.periodIndex
                    EntityRow(
                        "${ScheduleRepository.dayLabelFor(p.dayOfWeek)}, ${p.startTime}–${p.endTime}",
                        key in viewModel.selectedPeriods,
                    ) { viewModel.togglePeriod(p.dayOfWeek, p.periodIndex) }
                }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { viewModel.backToType() }, modifier = Modifier.weight(1f)) { Text("Back") }
        Button(
            onClick = { viewModel.goToPreview() },
            enabled = viewModel.canProceedFromEntities,
            modifier = Modifier.weight(1f),
        ) { Text("Preview") }
    }
}

@Composable
private fun EntityRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.PreviewStep(viewModel: RepairWorkflowViewModel) {
    val dimension = viewModel.dimension
    val otherDimensions = RepairDimension.entries.filter { it != dimension }
    var adjustMenuOpen by remember { mutableStateOf(false) }
    var pickerForSession by remember { mutableStateOf<ScheduleRepository.RunSessionState?>(null) }

    Text(
        "Previewing the current schedule for the selected ${if (dimension == RepairDimension.PERIOD) "period(s)" else dimension.label.lowercase() + "(s)"}.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ExposedDropdownMenuBox(expanded = adjustMenuOpen, onExpandedChange = { adjustMenuOpen = it }) {
        OutlinedTextField(
            value = viewModel.adjustBy?.label ?: "Just viewing — tap to adjust",
            onValueChange = {},
            readOnly = true,
            label = { Text("Adjust by") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = adjustMenuOpen) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = adjustMenuOpen, onDismissRequest = { adjustMenuOpen = false }) {
            DropdownMenuItem(text = { Text("Just viewing") }, onClick = { viewModel.selectAdjustBy(null); adjustMenuOpen = false })
            otherDimensions.forEach { d ->
                DropdownMenuItem(text = { Text(d.label) }, onClick = { viewModel.selectAdjustBy(d); adjustMenuOpen = false })
            }
        }
    }

    if (viewModel.pendingChanges > 0) {
        Text(
            "${viewModel.pendingChanges} adjustment(s) made — not yet saved.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    viewModel.repairMessage?.let { message ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                val candidates = viewModel.scopeExpansionLabels()
                candidates.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                if (viewModel.scopeExpansionCandidates.isNotEmpty()) {
                    Button(onClick = { viewModel.expandRepairScope() }, enabled = !viewModel.busy) {
                        Text("Expand scope & Repair")
                    }
                }
            }
        }
    }

    viewModel.validationMessage?.let { message ->
        Text(
            message,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        viewModel.previewGroups.forEach { (groupLabel, rows) ->
            item {
                Text(groupLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (rows.isEmpty()) {
                item { Text("Nothing currently scheduled here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(rows, key = { it.sessionId }) { row ->
                val adjustBy = viewModel.adjustBy
                Card(
                    modifier = Modifier.fillMaxWidth().let { m ->
                        if (adjustBy != null) m.clickable { pickerForSession = row } else m
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = if (viewModel.isSessionChanged(row.sessionId)) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HighlightableField(
                                "${row.dayLabel}, ${row.startTime}",
                                viewModel.isTimeslotChanged(row.sessionId),
                            )
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HighlightableField(
                                row.subjectName ?: "—",
                                false,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            HighlightableField(
                                row.teacherName ?: "—",
                                false,
                            )
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HighlightableField(
                                row.sectionName ?: "—",
                                false,
                            )
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HighlightableField(
                                row.roomName ?: "Unassigned",
                                viewModel.isRoomChanged(row.sessionId),
                            )
                        }
                    }
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { viewModel.backToEntities() }, modifier = Modifier.weight(1f)) { Text("Back") }
    }
    OutlinedButton(
        onClick = { viewModel.validateWorking() },
        enabled = !viewModel.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Validate") }
    Button(
        onClick = { viewModel.save() },
        enabled = viewModel.pendingChanges > 0 && !viewModel.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (viewModel.busy) "Saving…" else "Save & Repair") }

    val tapped = pickerForSession
    if (tapped != null && viewModel.adjustBy != null) {
        val adjustBy = viewModel.adjustBy!!
        val options = viewModel.scopedSessions.filter { it.sessionId != tapped.sessionId }
        AlertDialog(
            onDismissRequest = { pickerForSession = null },
            title = { Text("Available ${adjustBy.label.lowercase()} options") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (options.isEmpty()) {
                        Text("No other session in this preview to trade with.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        options.forEach { option ->
                            val label = when (adjustBy) {
                                RepairDimension.PERIOD -> "${option.dayLabel}, ${option.startTime} — currently ${option.teacherName ?: option.sectionName ?: "—"}"
                                RepairDimension.ROOM -> "${option.roomName ?: "Unassigned"} — ${option.teacherName ?: "—"}, ${option.dayLabel} ${option.startTime}"
                                RepairDimension.CLASS -> "${option.sectionName ?: "—"} — ${option.teacherName ?: "—"}, ${option.dayLabel} ${option.startTime}"
                                RepairDimension.SUBJECT -> "${option.subjectName ?: "—"} — ${option.teacherName ?: "—"}, ${option.dayLabel} ${option.startTime}"
                                RepairDimension.TEACHER -> "${option.teacherName ?: "—"} — ${option.subjectName ?: "—"}, ${option.dayLabel} ${option.startTime}"
                            }
                            Text(
                                label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.swapWith(tapped.sessionId, option.sessionId)
                                        pickerForSession = null
                                    }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickerForSession = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun HighlightableField(text: String, highlighted: Boolean) {
    Text(
        text,
        style = if (highlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
}
