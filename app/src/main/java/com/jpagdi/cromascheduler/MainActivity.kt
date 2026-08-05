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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.designsystem.AccentColor
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.CromaSchedulerTheme
import com.jpagdi.cromascheduler.designsystem.DashboardCard
import com.jpagdi.cromascheduler.di.CromaApplication
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.navigation.Screen
import com.jpagdi.cromascheduler.ui.ExportScreen
import com.jpagdi.cromascheduler.ui.GenerateScreen
import com.jpagdi.cromascheduler.ui.ImportScreen
import com.jpagdi.cromascheduler.ui.RepairScreen
import com.jpagdi.cromascheduler.ui.ResultsScreen
import com.jpagdi.cromascheduler.ui.RunsListScreen
import com.jpagdi.cromascheduler.ui.ValidateScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CromaApplication).container
        setContent {
            CromaSchedulerTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        CromaNavHost()
                    }
                }
            }
        }
    }
}

/**
 * Hand-rolled nav host backed by a simple back-stack list — see Screen.kt's doc
 * comment for why this doesn't use androidx.navigation:navigation-compose.
 */
@Composable
private fun CromaNavHost() {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val current = backStack.last()

    fun push(screen: Screen) {
        backStack = backStack + screen
    }
    fun popTo(screen: Screen) {
        // Used when a flow finishes (e.g. Generate -> Results) and shouldn't leave
        // the intermediate form screen on the back stack for the user to land on
        // by pressing Back.
        backStack = listOf(Screen.Home, screen)
    }
    fun pop() {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }

    when (val screen = current) {
        is Screen.Home -> HomeDashboard(
            onImport = { push(Screen.Import) },
            onGenerate = { push(Screen.Generate) },
            onValidate = { push(Screen.PickRunForValidate()) },
            onRepair = { push(Screen.PickRunForRepair()) },
            onExport = { push(Screen.PickRunForExport()) },
        )
        is Screen.Import -> ImportScreen(onBack = ::pop)
        is Screen.Generate -> GenerateScreen(
            onBack = ::pop,
            onGenerated = { runId -> popTo(Screen.Results(runId)) },
        )
        is Screen.PickRunForValidate -> RunsListScreen(
            title = "Validate Schedule",
            onBack = ::pop,
            onSelect = { run -> push(Screen.Validate(run.id)) },
        )
        is Screen.Validate -> ValidateScreen(
            runId = screen.runId,
            onBack = ::pop,
            onRepair = { runId -> push(Screen.Repair(runId)) },
        )
        is Screen.PickRunForRepair -> RunsListScreen(
            title = "Repair Schedule",
            onBack = ::pop,
            onSelect = { run -> push(Screen.Repair(run.id)) },
        )
        is Screen.Repair -> RepairScreen(
            runId = screen.runId,
            onBack = ::pop,
            onRepaired = { newRunId -> popTo(Screen.Results(newRunId)) },
        )
        is Screen.Results -> ResultsScreen(
            runId = screen.runId,
            onBack = { backStack = listOf(Screen.Home) },
            onExport = { runId -> push(Screen.Export(runId)) },
        )
        is Screen.PickRunForExport -> RunsListScreen(
            title = "Export Schedule",
            onBack = ::pop,
            onSelect = { run -> backStack = backStack.dropLast(1) + Screen.Export(run.id) },
        )
        is Screen.Export -> ExportScreen(
            runId = screen.runId,
            runName = screen.runId, // acceptable fallback — ExportScreen only uses this for the display filename/label
            onBack = ::pop,
        )
    }
}

private data class DashboardEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: AccentColor,
    val onClick: () -> Unit,
)

@Composable
private fun HomeDashboard(
    onImport: () -> Unit,
    onGenerate: () -> Unit,
    onValidate: () -> Unit,
    onRepair: () -> Unit,
    onExport: () -> Unit,
) {
    val entries = listOf(
        DashboardEntry("Import Data", "Load teachers, subjects, rooms, sessions from CSV", Icons.Filled.UploadFile, CromaAccents.Teal, onImport),
        DashboardEntry("Generate Schedule", "Build a new conflict-free schedule", Icons.Filled.AutoAwesome, CromaAccents.Indigo, onGenerate),
        DashboardEntry("Validate Schedule", "Check an existing schedule for conflicts", Icons.Filled.FactCheck, CromaAccents.Amber, onValidate),
        DashboardEntry("Repair Schedule", "Fix only the conflicting sessions", Icons.Filled.Build, CromaAccents.Maroon, onRepair),
        DashboardEntry("Export Schedule", "CSV, Excel, PDF, or print-friendly view", Icons.Filled.Share, CromaAccents.Teal, onExport),
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
                    onClick = entry.onClick,
                )
            }
        }
    }
}
