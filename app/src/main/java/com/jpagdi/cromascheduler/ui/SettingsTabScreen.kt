package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.DashboardCard

@Composable
fun SettingsTabScreen(onBack: () -> Unit, onDefinePeriods: () -> Unit) {
    Scaffold(topBar = { CromaTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardCard(
                title = "Define Periods",
                subtitle = "Period length, periods per day, active days",
                icon = Icons.Filled.CalendarMonth,
                accent = CromaAccents.Indigo,
                onClick = onDefinePeriods,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About the scheduling engine", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Chroma Timetables schedules everything using graph coloring (Greedy, Welsh-Powell, and DSATUR) " +
                            "plus deterministic constraint checking. There's no AI, machine learning, or cloud service " +
                            "involved anywhere — every schedule is built entirely on this device, and running the same " +
                            "data through the same algorithm always produces the same result.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
