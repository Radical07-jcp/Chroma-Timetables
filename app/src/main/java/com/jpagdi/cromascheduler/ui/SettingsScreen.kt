package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.SettingsViewModel

/**
 * Only one setting so far — default coloring algorithm, the "initial entry" the sidebar's Settings
 * button was specifically asked to open on — but its own screen (not folded into About) since
 * Settings is meant to grow with more preferences, while About is meant to stay a static page.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.appPreferencesStore))
    val current by viewModel.defaultAlgorithmName.collectAsState()

    Scaffold(topBar = { CromaTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}
