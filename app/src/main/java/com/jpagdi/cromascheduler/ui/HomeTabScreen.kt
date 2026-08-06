package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.designsystem.AccentColor
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.CromaShapes
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.HomeViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

@Composable
fun HomeTabScreen(
    onImport: () -> Unit,
    onGenerate: () -> Unit,
    onValidate: () -> Unit,
    onRepair: () -> Unit,
    onExport: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(container))
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            Surface {
                Text("Chroma Timetables", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            StatusOverviewCard(viewModel.counts)
            GenerateScheduleHeroCard(onClick = onGenerate)
            WorkflowSection(onImport = onImport, onValidate = onValidate, onRepair = onRepair)
            OutputSection(onExport = onExport)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusOverviewCard(counts: com.jpagdi.cromascheduler.data.repository.ScheduleRepository.HomeCounts) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CromaShapes.small).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(8.dp))
                Text("STATUS OVERVIEW", style = MaterialTheme.typography.labelMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("${counts.teachers}", "Teachers")
                StatColumn("${counts.subjects}", "Subjects")
                StatColumn("${counts.rooms}", "Rooms")
                StatColumn("${counts.sessions}", "Sessions")
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GenerateScheduleHeroCard(onClick: () -> Unit) {
    val gradient = Brush.linearGradient(
        listOf(CromaAccents.Teal.surface.copy(alpha = 0.85f), CromaAccents.Indigo.surface.copy(alpha = 0.85f), CromaAccents.Maroon.surface.copy(alpha = 0.6f)),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CromaShapes.large)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CromaShapes.small)
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Open", tint = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Generate Schedule", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                "Build a new conflict-free schedule using graph coloring",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Deterministic • Runs fully on-device",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

private data class WorkflowStep(val title: String, val subtitle: String, val icon: ImageVector, val accent: AccentColor, val onClick: () -> Unit)

@Composable
private fun WorkflowSection(onImport: () -> Unit, onValidate: () -> Unit, onRepair: () -> Unit) {
    val steps = listOf(
        WorkflowStep("Import Data", "Load teachers, subjects, rooms, sessions", Icons.Filled.UploadFile, CromaAccents.Teal, onImport),
        WorkflowStep("Validate Schedule", "Check an existing schedule for conflicts", Icons.Filled.FactCheck, CromaAccents.Amber, onValidate),
        WorkflowStep("Repair Schedule", "Fix only the conflicting sessions", Icons.Filled.Build, CromaAccents.Maroon, onRepair),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WORKFLOW", style = MaterialTheme.typography.labelMedium)
            Text("${steps.size} steps", style = MaterialTheme.typography.labelMedium)
        }
        steps.forEach { step ->
            com.jpagdi.cromascheduler.designsystem.DashboardCard(
                title = step.title, subtitle = step.subtitle, icon = step.icon, accent = step.accent, onClick = step.onClick,
            )
        }
    }
}

@Composable
private fun OutputSection(onExport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("OUTPUT", style = MaterialTheme.typography.labelMedium)
        com.jpagdi.cromascheduler.designsystem.DashboardCard(
            title = "Export Schedule",
            subtitle = "CSV, Excel, PDF, or print-friendly view",
            icon = Icons.Filled.Share,
            accent = CromaAccents.Teal,
            onClick = onExport,
        )
    }
}
