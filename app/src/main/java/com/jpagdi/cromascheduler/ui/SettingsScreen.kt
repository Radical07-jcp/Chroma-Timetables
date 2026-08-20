package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.AccentColorViewModel
import com.jpagdi.cromascheduler.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPickGroupA: () -> Unit,
    onPickGroupB: () -> Unit,
    onOpenTeachers: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.appPreferencesStore))
    val current by viewModel.defaultAlgorithmName.collectAsState()

    val accentViewModel: AccentColorViewModel = viewModel(
        factory = AccentColorViewModel.factory(container.appPreferencesStore),
    )
    val groupA by accentViewModel.groupA.collectAsState()
    val groupB by accentViewModel.groupB.collectAsState()

    Scaffold(topBar = { CromaTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "Personalize Chroma",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            FormalBodyText(
                "Choose how schedules are generated and how the interface is accented.",
            )

            SettingsSection(title = "Chroma Engine") {
                Text("v1.0.4", style = MaterialTheme.typography.titleMedium)
                FormalBodyText(
                    "UI/UX revamp with Material 3 theming, Space Grotesk typography, safer import replacement, persistent timetable source snapshots, version deletion, and preference-aware repair/optimization.",
                )
                Text("Developed by Sir_JPagdi", style = MaterialTheme.typography.labelMedium)
            }

            SettingsSection(title = "Scheduling") {
                Text("Default algorithm", style = MaterialTheme.typography.titleMedium)
                FormalBodyText(
                    "Used as the starting choice for every new timetable. You can still change it for an individual schedule.",
                )
                Spacer(Modifier.height(6.dp))
                viewModel.algorithmNames.forEach { name ->
                    val selected = (current ?: "dsatur") == name
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.setDefaultAlgorithm(name) },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            }

            SettingsSection(title = "Teacher availability") {
                Text("Time preferences", style = MaterialTheme.typography.titleMedium)
                FormalBodyText(
                    "Mark periods a teacher cannot take. These preferences feed directly into repair and optimization so an existing timetable can be adjusted locally instead of rebuilt.",
                )
                Button(onClick = onOpenTeachers, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage teacher availability")
                }
            }

            SettingsSection(title = "Accent colors") {
                FormalBodyText(
                    "Two independent accents let you keep the interface expressive without changing the selected theme.",
                )
                Spacer(Modifier.height(4.dp))
                AccentRow("Top panel", groupA, onPickGroupA)
                AccentRow("Primary actions", groupB, onPickGroupB)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                content()
            },
        )
    }
}

@Composable
private fun AccentRow(label: String, color: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text("Tap to choose", style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = color,
            ) {}
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Choose $label",
            )
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        tonalElevation = 0.dp,
    )
    // The actual clickable surface follows the same layout as the ListItem.
}
