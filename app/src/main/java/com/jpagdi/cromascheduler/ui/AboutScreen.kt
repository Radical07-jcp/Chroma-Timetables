package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { CromaTopBar("About", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About the scheduling engine", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Chroma Timetables schedules everything using graph coloring (Greedy, Welsh-Powell, and DSATUR) " +
                            "plus deterministic constraint checking. Every schedule is built entirely on this device, and " +
                            "running the same data through the same algorithm always produces the same result.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
