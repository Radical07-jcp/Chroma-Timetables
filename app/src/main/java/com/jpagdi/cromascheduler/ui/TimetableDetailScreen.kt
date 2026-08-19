package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
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
    var versionToDelete by remember { mutableStateOf<LineageEntry?>(null) }
    var renameTarget by remember { mutableStateOf<LineageEntry?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var showExportPicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showRepairDialog by remember { mutableStateOf(false) }
    var repairWizardStep by remember { mutableStateOf(1) }
    var repairEntityKind by remember { mutableStateOf(RepairEntityKind.TEACHER) }
    var selectedRepairEntityIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedRepairConflictKinds by remember { mutableStateOf<Set<RepairConflictKind>>(emptySet()) }
    val root = viewModel.root

    LaunchedEffect(showRepairDialog, viewModel.latest?.run?.id) {
        if (showRepairDialog) {
            viewModel.loadRepairConflicts()
            repairWizardStep = 1
            repairEntityKind = RepairEntityKind.TEACHER
            selectedRepairEntityIds = emptySet()
            selectedRepairConflictKinds = emptySet()
        }
    }

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
                CromaWorkflowTags(
                    active = when (viewModel.latest?.run?.mode) {
                        "OPTIMIZE" -> "OPTIMIZE"
                        "REPAIR" -> "VALIDATE"
                        else -> "PLAN"
                    },
                )
            }

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
                LineageEntryCard(
                    entry = entry,
                    onView = { onResults(entry.run.id) },
                    onDelete = { versionToDelete = entry },
                    onRename = {
                        renameTarget = entry
                        renameValue = entry.run.name
                        showMore = false
                    },
                )
            }

        }
    }

    if (versionToDelete != null) {
        val target = versionToDelete!!
        val isRoot = target.run.id == root?.id
        val isLastVersion = viewModel.entries.size == 1
        AlertDialog(
            onDismissRequest = { versionToDelete = null },
            title = { Text(if (isRoot && isLastVersion) "Delete this timetable?" else "Delete this version?") },
            text = {
                Text(
                    when {
                        isRoot && isLastVersion ->
                            "This is the only saved version. Deleting it removes the entire timetable."
                        isRoot ->
                            "This removes the original entry only. The timetable stays, continuing from its next saved version."
                        else ->
                            "This removes only this saved version. Other versions in the timetable history remain available."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        versionToDelete = null
                        viewModel.deleteVersion(target.run.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { versionToDelete = null }) { Text("Cancel") } },
        )
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
                        Text("Export a version", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = { showMore = false; showRepairDialog = true },
                        enabled = !viewModel.busy && viewModel.latest != null,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Repair schedule", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            showMore = false
                            val latestEntry = viewModel.latest ?: return@TextButton
                            renameTarget = latestEntry
                            renameValue = latestEntry.run.name
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rename latest version", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = { showMore = false; confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete timetable", modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = { TextButton(onClick = { showMore = false }) { Text("Close") } },
        )
    }

    if (showRepairDialog) {
        val details = viewModel.repairConflictDetails

        // Step 1 -> which specific teachers/rooms/classes/subjects actually show up in a
        // conflict, so step 2 only ever lists entities that are really affected.
        val affectedEntities = remember(details, repairEntityKind) {
            val ids = LinkedHashMap<String, String>() // id -> display name, insertion order
            details.forEach { d ->
                listOfNotNull(d.sessionA, d.sessionB).forEach { p ->
                    val id = p.idFor(repairEntityKind)
                    val name = p.nameFor(repairEntityKind)
                    if (id != null && name != null) ids.putIfAbsent(id, name)
                }
            }
            ids.toList() // List<Pair<id, name>>
        }

        // Step 2 -> conflicts touching at least one selected entity, broken down by kind
        // (time/room/class/subject) so step 3 can show a real count next to each option.
        val conflictsForSelectedEntities = remember(details, repairEntityKind, selectedRepairEntityIds) {
            details.filter { d ->
                listOfNotNull(d.sessionA, d.sessionB).any { p -> p.idFor(repairEntityKind) in selectedRepairEntityIds }
            }
        }
        val kindCounts = remember(conflictsForSelectedEntities) {
            RepairConflictKind.entries.associateWith { kind -> conflictsForSelectedEntities.count { it.kind() == kind } }
        }

        val finalMatches = remember(conflictsForSelectedEntities, selectedRepairConflictKinds) {
            conflictsForSelectedEntities.filter { it.kind() in selectedRepairConflictKinds }
        }
        val finalSessionIds = remember(finalMatches) {
            finalMatches.flatMap { listOfNotNull(it.sessionA.sessionId, it.sessionB?.sessionId) }.toSet()
        }

        AlertDialog(
            onDismissRequest = { showRepairDialog = false },
            title = {
                Text(
                    when (repairWizardStep) {
                        1 -> "Repair schedule — 1. Conflict type"
                        2 -> "Repair schedule — 2. Affected ${repairEntityKind.label.lowercase()}s"
                        else -> "Repair schedule — 3. What to fix"
                    },
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (repairWizardStep) {
                        1 -> {
                            Text("Choose which kind of entity is involved in the conflict you want to fix.")
                            RepairEntityKind.entries.forEach { kind ->
                                val count = remember(details, kind) {
                                    details.flatMap { listOfNotNull(it.sessionA, it.sessionB) }
                                        .mapNotNull { it.idFor(kind) }.distinct().size
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = count > 0) { repairEntityKind = kind },
                                ) {
                                    RadioButton(selected = repairEntityKind == kind, onClick = { repairEntityKind = kind }, enabled = count > 0)
                                    Text(
                                        if (count > 0) "${kind.label} (${count} affected)" else "${kind.label} — none affected",
                                        color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        2 -> {
                            Text("Select which specific ${repairEntityKind.label.lowercase()}(s) you want to repair. Everything else stays exactly as it is.")
                            if (affectedEntities.isEmpty()) {
                                Text("No ${repairEntityKind.label.lowercase()} is currently involved in a conflict.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                affectedEntities.forEach { (id, name) ->
                                    val checked = id in selectedRepairEntityIds
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedRepairEntityIds = if (checked) selectedRepairEntityIds - id else selectedRepairEntityIds + id
                                            },
                                    ) {
                                        Checkbox(checked = checked, onCheckedChange = {
                                            selectedRepairEntityIds = if (it) selectedRepairEntityIds + id else selectedRepairEntityIds - id
                                        })
                                        Text(name, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(
                                "Choose which kind of conflict on the selected ${repairEntityKind.label.lowercase()}(s)' schedule to fix. Only sessions behind these selections change — the rest of the timetable is untouched.",
                            )
                            RepairConflictKind.entries.forEach { kind ->
                                val count = kindCounts[kind] ?: 0
                                val checked = kind in selectedRepairConflictKinds
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = count > 0) {
                                            selectedRepairConflictKinds = if (checked) selectedRepairConflictKinds - kind else selectedRepairConflictKinds + kind
                                        },
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        enabled = count > 0,
                                        onCheckedChange = {
                                            selectedRepairConflictKinds = if (it) selectedRepairConflictKinds + kind else selectedRepairConflictKinds - kind
                                        },
                                    )
                                    Text(
                                        if (count > 0) "${kind.label} (${count})" else "${kind.label} — none",
                                        color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (finalSessionIds.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    "${finalSessionIds.size} session(s) will be recalculated; everything else on the timetable is preserved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                when (repairWizardStep) {
                    1 -> TextButton(
                        enabled = affectedEntities.isNotEmpty(),
                        onClick = { repairWizardStep = 2 },
                    ) { Text("Next") }
                    2 -> TextButton(
                        enabled = selectedRepairEntityIds.isNotEmpty(),
                        onClick = { repairWizardStep = 3 },
                    ) { Text("Next") }
                    else -> TextButton(
                        enabled = finalSessionIds.isNotEmpty() && !viewModel.busy,
                        onClick = {
                            showRepairDialog = false
                            viewModel.repairLatest(finalSessionIds)
                        },
                    ) { Text("Repair selected") }
                }
            },
            dismissButton = {
                if (repairWizardStep > 1) {
                    TextButton(onClick = { repairWizardStep -= 1 }) { Text("Back") }
                } else {
                    TextButton(onClick = { showRepairDialog = false }) { Text("Cancel") }
                }
            },
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = {
                renameTarget = null
                renameValue = ""
            },
            title = { Text("Rename timetable") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameTarget != null) {
                            viewModel.renameVersion(renameTarget!!.run.id, renameValue)
                        }
                        renameTarget = null
                        renameValue = ""
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameTarget = null
                        renameValue = ""
                    },
                ) { Text("Cancel") }
            },
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
private fun LineageEntryCard(entry: LineageEntry, onView: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit) {
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
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename version")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete version", tint = MaterialTheme.colorScheme.error)
                }
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

/** Which kind of entity the guided Repair dialog's step 1 lets someone target. */
private enum class RepairEntityKind(val label: String) {
    TEACHER("Teacher"), ROOM("Room"), CLASS("Class"), SUBJECT("Subject")
}

/** Which dimension of a conflict step 3 lets someone target — "the other types" per-entity. */
private enum class RepairConflictKind(val label: String) {
    TIME("Time"), ROOM("Room"), CLASS("Class"), SUBJECT("Subject")
}

private fun com.jpagdi.cromascheduler.data.repository.ScheduleRepository.ConflictParticipant.idFor(
    kind: RepairEntityKind,
): String? = when (kind) {
    RepairEntityKind.TEACHER -> teacherId
    RepairEntityKind.ROOM -> roomId
    RepairEntityKind.CLASS -> sectionId
    RepairEntityKind.SUBJECT -> subjectId
}

private fun com.jpagdi.cromascheduler.data.repository.ScheduleRepository.ConflictParticipant.nameFor(
    kind: RepairEntityKind,
): String? = when (kind) {
    RepairEntityKind.TEACHER -> teacherName
    RepairEntityKind.ROOM -> roomName
    RepairEntityKind.CLASS -> sectionName
    RepairEntityKind.SUBJECT -> subjectName
}

/**
 * Which of the four "other types" a conflict record represents — time (a teacher-schedule
 * clash), room, class, or subject. Used by the guided Repair dialog's step 3 to let someone
 * narrow a selected teacher/room/class/subject down to only the conflict kind(s) they want fixed.
 */
private fun com.jpagdi.cromascheduler.data.repository.ScheduleRepository.ConflictDetail.kind(): RepairConflictKind =
    when (conflictType) {
        "TEACHER_DOUBLE_BOOKED", "TEACHER_UNAVAILABLE", "DURATION_EXCEEDS_AVAILABLE_PERIODS" -> RepairConflictKind.TIME
        "ROOM_DOUBLE_BOOKED", "ROOM_UNAVAILABLE", "ROOM_CAPACITY_EXCEEDED" -> RepairConflictKind.ROOM
        "SECTION_DOUBLE_BOOKED" -> RepairConflictKind.CLASS
        "SUBJECT_DOUBLE_BOOKED" -> RepairConflictKind.SUBJECT
        else -> RepairConflictKind.TIME
    }
