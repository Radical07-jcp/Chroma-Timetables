package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.designsystem.CromaShapes
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.engine.model.Timeslot
import com.jpagdi.cromascheduler.viewmodel.TeacherAvailabilityViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

private val DAY_LABELS = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

@Composable
fun TeacherAvailabilityScreen(teacherId: String, teacherName: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TeacherAvailabilityViewModel = viewModel(factory = ViewModelFactory(container))
    LaunchedEffect(teacherId) { viewModel.load(teacherId) }

    val config = viewModel.periodConfig
    val activeDays = config.activeDaysList()
    val dayLabels = DAY_LABELS.filter { it.first in activeDays }

    Scaffold(topBar = { CromaTopBar("$teacherName — Availability", onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Tap a period to mark it unavailable. Tap again to clear it. This feeds straight into scheduling — no re-import needed.",
                style = MaterialTheme.typography.bodySmall,
            )

            androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(56.dp))
                        dayLabels.forEach { (_, label) ->
                            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                items(config.totalPeriodsPerDay()) { period ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("P${period + 1}", modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium)
                        dayLabels.forEach { (day, _) ->
                            val blocked = Timeslot(day, period) in viewModel.blockedSlots
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .height(36.dp)
                                    .clip(CromaShapes.small)
                                    .background(if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.toggle(day, period) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (blocked) Text("✕", color = Color.White)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}