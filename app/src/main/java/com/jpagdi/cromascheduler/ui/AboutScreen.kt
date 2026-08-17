package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.R

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { CromaTopBar("About", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.logo_chroma),
                contentDescription = "Chroma Timetables logo",
                modifier = Modifier.size(220.dp),
            )
            Text("Chroma Timetables", style = MaterialTheme.typography.headlineSmall)
            Text("Chroma Engine v1.0.0", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Developed by Sir_JPagdi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About the scheduling engine", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Chroma schedules everything using graph coloring (Greedy, Welsh-Powell, and DSATUR) " +
                            "plus deterministic constraint checking. Every schedule is built entirely on this device, " +
                            "and running the same data through the same algorithm always produces the same result.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
