package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.AccentColorViewModel
import com.jpagdi.cromascheduler.viewmodel.SettingsViewModel

/**
 * Default coloring algorithm — the "initial entry" the sidebar's Settings button was specifically
 * asked to open on — plus the two accent-group pickers (Top Panel / Button), ported from the
 * reference app's own Settings-adjacent accent controls. Its own screen, not folded into About,
 * since Settings is meant to grow with more preferences while About stays a static page.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onPickGroupA: () -> Unit, onPickGroupB: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.appPreferencesStore))
    val current by viewModel.defaultAlgorithmName.collectAsState()

    val accentViewModel: AccentColorViewModel = viewModel(factory = AccentColorViewModel.factory(container.appPreferencesStore))
    val groupA by accentViewModel.groupA.collectAsState()
    val groupB by accentViewModel.groupB.collectAsState()

    Scaffold(topBar = { CromaTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Default algorithm", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Used to pre-select the algorithm every time you generate a new timetable — you can still change it per-timetable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(4.dp))
                    viewModel.algorithmNames.forEach { name ->
                        val selected = (current ?: "dsatur") == name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(selected = selected, onClick = { viewModel.setDefaultAlgorithm(name) })
                            Text(name)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Accent colors", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Two independent accents: one for headers and panels, one for primary buttons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(4.dp))
                    AccentRow("Top Panel Accent", groupA, onPickGroupA)
                    AccentRow("Button Accent", groupB, onPickGroupB)
                }
            }
        }
    }
}

@Composable
private fun AccentRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
