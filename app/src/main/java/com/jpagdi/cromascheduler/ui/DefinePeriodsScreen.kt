package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.viewmodel.CreateTimetableViewModel

private val DAY_LABELS = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
private val DURATION_PRESETS = listOf(40, 45, 50, 55, 60)

/**
 * Right after picking a type (see ChooseTimetableTypeScreen), before Generate — this timetable's
 * OWN periods, never a shared setting. Nothing here calls the repository; [viewModel] just holds
 * the blocks/days in memory until GenerateScreen actually creates the run with them.
 */
@Composable
fun DefineTimetablePeriodsScreen(viewModel: CreateTimetableViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val blocks = viewModel.periodBlocks
    val activeDays = viewModel.activeDays

    Scaffold(topBar = { CromaTopBar("Define Periods", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Add one block per continuous run of periods — a single all-day block with a lunch break inside it, or separate AM/PM blocks with a gap between them. This is specific to the timetable you're creating now; another timetable can use completely different periods.",
                style = MaterialTheme.typography.bodyMedium,
            )

            blocks.forEachIndexed { index, block ->
                BlockEditor(
                    block = block,
                    canRemove = blocks.size > 1,
                    onChange = { updated -> viewModel.setPeriodBlocks(blocks.toMutableList().also { it[index] = updated }) },
                    onRemove = { viewModel.setPeriodBlocks(blocks.toMutableList().also { it.removeAt(index) }) },
                )
            }

            OutlinedButton(
                onClick = {
                    val previousEnd = blocks.lastOrNull()?.computedEndMinutes() ?: (7 * 60)
                    viewModel.setPeriodBlocks(blocks + PeriodBlock(label = "Block ${blocks.size + 1}", startMinutesSinceMidnight = previousEnd, periodDurationMinutes = 60, periodCount = 4))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Block")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active days", style = MaterialTheme.typography.titleSmall)
                    val activeSet = activeDays.toSet()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_LABELS.forEach { (dayNum, label) ->
                            FilterChip(
                                selected = dayNum in activeSet,
                                onClick = {
                                    val updated = if (dayNum in activeSet) activeSet - dayNum else activeSet + dayNum
                                    viewModel.setActiveDays(updated.sorted())
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            val canContinue = blocks.isNotEmpty() && activeDays.isNotEmpty()
            Button(onClick = onNext, enabled = canContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continue")
            }
            if (!canContinue) {
                Text(
                    "Add at least one block and one active day to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BlockEditor(block: PeriodBlock, canRemove: Boolean, onChange: (PeriodBlock) -> Unit, onRemove: () -> Unit) {
    var labelText by remember(block.label) { mutableStateOf(block.label) }

    // Raw TEXT state, separate from the parsed Int — this is what actually makes the hour/minute
    // fields editable. Binding a TextField's `value` straight to `startHour.toString()` where
    // `startHour` only updates on a successful parse means the field silently snaps back to its old
    // digits the instant you backspace to clear it (toIntOrNull("") is null, so nothing updates,
    // so the displayed text never actually changes) — from the outside that reads as "can't edit
    // this field" even though the onChange wiring underneath is fine. Every numeric field below now
    // keeps its own text buffer and only pushes a value up to the block once it parses.
    var startHourText by remember(block.startMinutesSinceMidnight) { mutableStateOf((block.startMinutesSinceMidnight / 60).toString()) }
    var startMinuteText by remember(block.startMinutesSinceMidnight) { mutableStateOf((block.startMinutesSinceMidnight % 60).toString()) }
    var durationText by remember(block.periodDurationMinutes) { mutableStateOf(block.periodDurationMinutes.toString()) }
    var countText by remember(block.periodCount) { mutableStateOf(block.periodCount.toString()) }

    var enableBreak by remember(block.breakAfterPeriod) { mutableStateOf(block.breakAfterPeriod != null) }
    var breakAfterText by remember(block.breakAfterPeriod) { mutableStateOf(((block.breakAfterPeriod ?: 0) + 1).toString()) }
    var breakMinutesText by remember(block.breakDurationMinutes) { mutableStateOf(block.breakDurationMinutes.toString()) }

    var enableLunch by remember(block.lunchAfterPeriod) { mutableStateOf(block.lunchAfterPeriod != null) }
    var lunchAfterText by remember(block.lunchAfterPeriod) { mutableStateOf(((block.lunchAfterPeriod ?: (block.periodCount / 2)) + 1).toString()) }
    var lunchMinutesText by remember(block.lunchDurationMinutes) { mutableStateOf(block.lunchDurationMinutes.takeIf { it > 0 }?.toString() ?: "45") }

    fun currentStartMinutes() = (startHourText.toIntOrNull() ?: 0) * 60 + (startMinuteText.toIntOrNull() ?: 0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it; onChange(block.copy(label = it)) },
                    label = { Text("Block name") },
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove block") }
                }
            }

            Text("Start time", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startHourText,
                    onValueChange = { text ->
                        startHourText = text
                        val h = text.toIntOrNull()
                        if (h != null && h in 0..23) onChange(block.copy(startMinutesSinceMidnight = currentStartMinutes()))
                    },
                    label = { Text("Hour (0-23)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = startMinuteText,
                    onValueChange = { text ->
                        startMinuteText = text
                        val m = text.toIntOrNull()
                        if (m != null && m in 0..59) onChange(block.copy(startMinutesSinceMidnight = currentStartMinutes()))
                    },
                    label = { Text("Minute (0-59)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Text("Period length (minutes)", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DURATION_PRESETS.forEach { minutes ->
                    FilterChip(
                        selected = block.periodDurationMinutes == minutes,
                        onClick = { durationText = minutes.toString(); onChange(block.copy(periodDurationMinutes = minutes)) },
                        label = { Text("$minutes") },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { text -> durationText = text; text.toIntOrNull()?.takeIf { it > 0 }?.let { onChange(block.copy(periodDurationMinutes = it)) } },
                    label = { Text("Custom length") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { text -> countText = text; text.toIntOrNull()?.takeIf { it > 0 }?.let { onChange(block.copy(periodCount = it)) } },
                    label = { Text("Periods in block") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enableBreak, onCheckedChange = { checked ->
                    enableBreak = checked
                    onChange(if (checked) block.copy(breakAfterPeriod = 0, breakDurationMinutes = 15) else block.copy(breakAfterPeriod = null, breakDurationMinutes = 0))
                })
                Spacer(Modifier.width(8.dp))
                Text("Short break")
            }
            if (enableBreak) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("After period")
                    OutlinedTextField(
                        value = breakAfterText,
                        onValueChange = { text ->
                            breakAfterText = text
                            text.toIntOrNull()?.let { p -> if (p in 1..block.periodCount) onChange(block.copy(breakAfterPeriod = p - 1)) }
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text("for")
                    OutlinedTextField(
                        value = breakMinutesText,
                        onValueChange = { text ->
                            breakMinutesText = text
                            text.toIntOrNull()?.let { m -> if (m >= 0) onChange(block.copy(breakDurationMinutes = m)) }
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text("min")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enableLunch, onCheckedChange = { checked ->
                    enableLunch = checked
                    val defaultAfter = (block.periodCount / 2).coerceAtLeast(1) - 1
                    onChange(if (checked) block.copy(lunchAfterPeriod = defaultAfter, lunchDurationMinutes = 45) else block.copy(lunchAfterPeriod = null, lunchDurationMinutes = 0))
                })
                Spacer(Modifier.width(8.dp))
                Text("Lunch break")
            }
            if (enableLunch) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("After period")
                    OutlinedTextField(
                        value = lunchAfterText,
                        onValueChange = { text ->
                            lunchAfterText = text
                            text.toIntOrNull()?.let { p -> if (p in 1..block.periodCount) onChange(block.copy(lunchAfterPeriod = p - 1)) }
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text("for")
                    OutlinedTextField(
                        value = lunchMinutesText,
                        onValueChange = { text ->
                            lunchMinutesText = text
                            text.toIntOrNull()?.let { m -> if (m >= 0) onChange(block.copy(lunchDurationMinutes = m)) }
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text("min")
                }
            }

            val endMinutes = block.computedEndMinutes()
            Text(
                "Ends at %02d:%02d".format((endMinutes % (24 * 60)) / 60, endMinutes % 60),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
