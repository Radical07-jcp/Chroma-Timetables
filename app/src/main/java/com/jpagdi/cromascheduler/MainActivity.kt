package com.jpagdi.cromascheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.CromaSchedulerTheme
import com.jpagdi.cromascheduler.designsystem.DashboardCard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CromaSchedulerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HomeDashboard()
                }
            }
        }
    }
}

private data class DashboardEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: com.jpagdi.cromascheduler.designsystem.AccentColor,
)

// Phase 1 note: onClick handlers are no-ops for now. Navigation + each destination
// screen (Import, Generate, Validate, Repair, Export) lands in Phase 7 per the
// phased plan — this screen exists now so the module wiring (app -> designsystem)
// is proven to compile end-to-end before the engine/data layers are built out.
@Composable
private fun HomeDashboard() {
    val entries = listOf(
        DashboardEntry("Import Data", "Load teachers, subjects, rooms, sessions from CSV", Icons.Filled.UploadFile, CromaAccents.Teal),
        DashboardEntry("Generate Schedule", "Build a new conflict-free schedule", Icons.Filled.AutoAwesome, CromaAccents.Indigo),
        DashboardEntry("Validate Schedule", "Check an existing schedule for conflicts", Icons.Filled.FactCheck, CromaAccents.Amber),
        DashboardEntry("Repair Schedule", "Fix only the conflicting sessions", Icons.Filled.Build, CromaAccents.Maroon),
        DashboardEntry("Export Schedule", "CSV, Excel, PDF, or print-friendly view", Icons.Filled.Share, CromaAccents.Teal),
    )

    Scaffold(
        topBar = {
            Surface {
                Text(
                    text = "CromaScheduler",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(entries) { entry ->
                DashboardCard(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    icon = entry.icon,
                    accent = entry.accent,
                    onClick = { /* Phase 7 */ },
                )
            }
        }
    }
}
