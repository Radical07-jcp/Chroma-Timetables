package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var customDurationText by remember(config.periodDurationMinutes) { mutableStateOf(config.periodDurationMinutes.toString()) }
    var startHour by remember(config) { mutableStateOf(config.dayStartMinutesSinceMidnight / 60) }
    var startMinute by remember(config) { mutableStateOf(config.dayStartMinutesSinceMidnight % 60) }
    var enableBreak by remember(config) { mutableStateOf(config.breakAfterPeriod != null) }

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
                "Every school runs on a different clock — set your period length, how many periods per day, and which days are active. This regenerates the whole timeslot grid your schedules are built from.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Period length (minutes)", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DURATION_PRESETS.forEach { minutes ->
                                FilterChip(
                                    selected = config.periodDurationMinutes == minutes,
                                    onClick = { viewModel.update(config.copy(periodDurationMinutes = minutes)) },
                                    label = { Text("$minutes") },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customDurationText,
                            onValueChange = { text ->
                                customDurationText = text
                                text.toIntOrNull()?.takeIf { it > 0 }?.let { viewModel.update(config.copy(periodDurationMinutes = it)) }
                            },
                            label = { Text("Custom (minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(180.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Periods per day", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { if (config.periodsPerDay > 1) viewModel.update(config.copy(periodsPerDay = config.periodsPerDay - 1)) }) { Text("−") }
                            Text("${config.periodsPerDay}", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { viewModel.update(config.copy(periodsPerDay = config.periodsPerDay + 1)) }) { Text("+") }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("First period starts at", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startHour.toString(),
                                onValueChange = { text ->
                                    text.toIntOrNull()?.let { h -> if (h in 0..23) { startHour = h; viewModel.update(config.copy(dayStartMinutesSinceMidnight = h * 60 + startMinute)) } }
                                },
                                label = { Text("Hour (0-23)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(140.dp),
                            )
                            OutlinedTextField(
                                value = startMinute.toString(),
                                onValueChange = { text ->
                                    text.toIntOrNull()?.let { m -> if (m in 0..59) { startMinute = m; viewModel.update(config.copy(dayStartMinutesSinceMidnight = startHour * 60 + m)) } }
                                },
                                label = { Text("Minute") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(140.dp),
                            )
                        }
                        Text("24-hour clock — 7:30 AM is hour 7, minute 30.", style = MaterialTheme.typography.bodySmall)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = enableBreak, onCheckedChange = { checked ->
                                enableBreak = checked
                                viewModel.update(
                                    if (checked) config.copy(breakAfterPeriod = 0, breakDurationMinutes = 15)
                                    else config.copy(breakAfterPeriod = null, breakDurationMinutes = 0),
                                )
                            })
                            Spacer(Modifier.width(8.dp))
                            Text("One daily break")
                        }
                        if (enableBreak) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("After period")
                                OutlinedTextField(
                                    value = ((config.breakAfterPeriod ?: 0) + 1).toString(),
                                    onValueChange = { text ->
                                        text.toIntOrNull()?.let { p -> if (p in 1..config.periodsPerDay) viewModel.update(config.copy(breakAfterPeriod = p - 1)) }
                                    },
                                    modifier = Modifier.width(80.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                                Text("for")
                                OutlinedTextField(
                                    value = config.breakDurationMinutes.toString(),
                                    onValueChange = { text -> text.toIntOrNull()?.let { m -> if (m >= 0) viewModel.update(config.copy(breakDurationMinutes = m)) } },
                                    modifier = Modifier.width(80.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                                Text("min")
                            }
                        }
                    }
                }
            }

            Button(onClick = { viewModel.save() }, enabled = !viewModel.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isSaving) "Saving…" else "Save & Regenerate Periods")
            }
            if (viewModel.savedConfirmation) {
                Text(
                    "Saved. Existing schedules keep their assignments, but re-validate them if you changed period length or count.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
