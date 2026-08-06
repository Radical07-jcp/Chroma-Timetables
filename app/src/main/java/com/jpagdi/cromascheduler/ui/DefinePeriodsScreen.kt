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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.PeriodBlock
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.PeriodConfigViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

private val DAY_LABELS = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
private val DURATION_PRESETS = listOf(40, 45, 50, 55, 60)

@Composable
fun DefinePeriodsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: PeriodConfigViewModel = viewModel(factory = ViewModelFactory(container))
    LaunchedEffect(Unit) { viewModel.load() }

    val config = viewModel.config
    val blocks = config.blocks()

    fun updateBlocks(newBlocks: List<PeriodBlock>) {
        viewModel.update(config.copy(blocksEncoded = PeriodBlock.encodeList(newBlocks)))
    }

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
                "Add one block per session your school runs — a single all-day block, or separate AM/PM blocks with a gap between them for lunch. Each block has its own start time, period length, and period count.",
                style = MaterialTheme.typography.bodyMedium,
            )

            blocks.forEachIndexed { index, block ->
                BlockEditor(
                    block = block,
                    canRemove = blocks.size > 1,
                    onChange = { updated -> updateBlocks(blocks.toMutableList().also { it[index] = updated }) },
                    onRemove = { updateBlocks(blocks.toMutableList().also { it.removeAt(index) }) },
                )
            }

            OutlinedButton(
                onClick = {
                    val previousEnd = blocks.lastOrNull()?.computedEndMinutes() ?: (7 * 60)
                    updateBlocks(blocks + PeriodBlock(label = "Block ${blocks.size + 1}", startMinutesSinceMidnight = previousEnd, periodDurationMinutes = 60, periodCount = 4))
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
                    val activeDays = config.activeDaysList().toSet()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_LABELS.forEach { (dayNum, label) ->
                            FilterChip(
                                selected = dayNum in activeDays,
                                onClick = {
                                    val updated = if (dayNum in activeDays) activeDays - dayNum else activeDays + dayNum
                                    viewModel.update(config.copy(activeDays = updated.sorted().joinToString(",")))
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            Button(onClick = { viewModel.save() }, enabled = !viewModel.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isSaving) "Saving…" else "Save & Regenerate Periods")
            }
            if (viewModel.savedConfirmation) {
                Text(
                    "Saved. Existing schedules keep their assignments, but re-validate them if you changed period length, count, or blocks.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BlockEditor(block: PeriodBlock, canRemove: Boolean, onChange: (PeriodBlock) -> Unit, onRemove: () -> Unit) {
    var labelText by remember(block.label) { mutableStateOf(block.label) }
    var startHour by remember(block.startMinutesSinceMidnight) { mutableStateOf(block.startMinutesSinceMidnight / 60) }
    var startMinute by remember(block.startMinutesSinceMidnight) { mutableStateOf(block.startMinutesSinceMidnight % 60) }
    var durationText by remember(block.periodDurationMinutes) { mutableStateOf(block.periodDurationMinutes.toString()) }
    var countText by remember(block.periodCount) { mutableStateOf(block.periodCount.toString()) }
    var enableBreak by remember(block.breakAfterPeriod) { mutableStateOf(block.breakAfterPeriod != null) }

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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startHour.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let { h -> if (h in 0..23) { startHour = h; onChange(block.copy(startMinutesSinceMidnight = h * 60 + startMinute)) } } },
                    label = { Text("Start hour") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = startMinute.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let { m -> if (m in 0..59) { startMinute = m; onChange(block.copy(startMinutesSinceMidnight = startHour * 60 + m)) } } },
                    label = { Text("Start min") },
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enableBreak, onCheckedChange = { checked ->
                    enableBreak = checked
                    onChange(if (checked) block.copy(breakAfterPeriod = 0, breakDurationMinutes = 15) else block.copy(breakAfterPeriod = null, breakDurationMinutes = 0))
                })
                Spacer(Modifier.width(8.dp))
                Text("Break within this block")
            }
            if (enableBreak) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("After period")
                    OutlinedTextField(
                        value = ((block.breakAfterPeriod ?: 0) + 1).toString(),
                        onValueChange = { text -> text.toIntOrNull()?.let { p -> if (p in 1..block.periodCount) onChange(block.copy(breakAfterPeriod = p - 1)) } },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text("for")
                    OutlinedTextField(
                        value = block.breakDurationMinutes.toString(),
                        onValueChange = { text -> text.toIntOrNull()?.let { m -> if (m >= 0) onChange(block.copy(breakDurationMinutes = m)) } },
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
