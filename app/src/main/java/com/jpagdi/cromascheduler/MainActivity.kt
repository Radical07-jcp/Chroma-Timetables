package com.jpagdi.cromascheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jpagdi.cromascheduler.designsystem.CromaSchedulerTheme
import com.jpagdi.cromascheduler.di.CromaApplication
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.navigation.Screen
import com.jpagdi.cromascheduler.ui.DefinePeriodsScreen
import com.jpagdi.cromascheduler.ui.ExportScreen
import com.jpagdi.cromascheduler.ui.GenerateScreen
import com.jpagdi.cromascheduler.ui.HomeTabScreen
import com.jpagdi.cromascheduler.ui.ImportScreen
import com.jpagdi.cromascheduler.ui.RepairScreen
import com.jpagdi.cromascheduler.ui.ResultsScreen
import com.jpagdi.cromascheduler.ui.RunsListScreen
import com.jpagdi.cromascheduler.ui.SettingsTabScreen
import com.jpagdi.cromascheduler.ui.TeacherAvailabilityScreen
import com.jpagdi.cromascheduler.ui.TeachersScreen
import com.jpagdi.cromascheduler.ui.TimetableTabScreen
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
 * Hand-rolled nav host — see Screen.kt's doc comment for why this isn't
 * androidx.navigation:navigation-compose. The bottom bar is visible only when the
 * back stack is exactly one of the four tab roots (Screen.bottomTabs); pushing any
 * sub-flow screen (Import, Generate, Results, etc.) hides it, matching how each of
 * those screens already has its own top bar with a back button.
 */
@Composable
private fun CromaNavHost() {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.HomeTab)) }
    val current = backStack.last()
    val showBottomBar = current in Screen.bottomTabs

    fun push(screen: Screen) {
        backStack = backStack + screen
    }
    fun pop() {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }
    fun switchTab(tab: Screen) {
        backStack = listOf(tab)
    }
    fun popToTabWith(screen: Screen) {
        val tabRoot = backStack.firstOrNull { it in Screen.bottomTabs } ?: Screen.HomeTab
        backStack = listOf(tabRoot, screen)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current == Screen.HomeTab,
                        onClick = { switchTab(Screen.HomeTab) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = current == Screen.TimetableTab,
                        onClick = { switchTab(Screen.TimetableTab) },
                        icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Timetable") },
                        label = { Text("Timetable") },
                    )
                    NavigationBarItem(
                        selected = current == Screen.TeachersTab,
                        onClick = { switchTab(Screen.TeachersTab) },
                        icon = { Icon(Icons.Filled.Groups, contentDescription = "Teachers") },
                        label = { Text("Teachers") },
                    )
                    NavigationBarItem(
                        selected = current == Screen.SettingsTab,
                        onClick = { switchTab(Screen.SettingsTab) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                    )
                }
            }
        },
    ) { padding ->
        Surface(modifier = androidx.compose.ui.Modifier.padding(bottom = padding.calculateBottomPadding())) {
            when (val screen = current) {
                is Screen.HomeTab -> HomeTabScreen(
                    onImport = { push(Screen.Import) },
                    onGenerate = { push(Screen.Generate) },
                    onValidate = { push(Screen.PickRunForValidate()) },
                    onRepair = { push(Screen.PickRunForRepair()) },
                    onExport = { push(Screen.PickRunForExport()) },
                )
                is Screen.TimetableTab -> TimetableTabScreen(onSelect = { run -> push(Screen.Results(run.id)) })
                is Screen.TeachersTab -> TeachersScreen(onSelectTeacher = { teacher -> push(Screen.TeacherAvailability(teacher.id, teacher.name)) })
                is Screen.SettingsTab -> SettingsTabScreen(onDefinePeriods = { push(Screen.DefinePeriods) })

                is Screen.Import -> ImportScreen(onBack = ::pop)
                is Screen.Generate -> GenerateScreen(
                    onBack = ::pop,
                    onGenerated = { runId -> popToTabWith(Screen.Results(runId)) },
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
                    onRepaired = { newRunId -> popToTabWith(Screen.Results(newRunId)) },
                )
                is Screen.Results -> ResultsScreen(
                    runId = screen.runId,
                    onBack = { val tabRoot = backStack.firstOrNull { it in Screen.bottomTabs } ?: Screen.HomeTab; backStack = listOf(tabRoot) },
                    onExport = { runId, runName -> push(Screen.Export(runId, runName)) },
                )
                is Screen.PickRunForExport -> RunsListScreen(
                    title = "Export Schedule",
                    onBack = ::pop,
                    onSelect = { run -> backStack = backStack.dropLast(1) + Screen.Export(run.id, run.name) },
                )
                is Screen.Export -> ExportScreen(runId = screen.runId, runName = screen.runName, onBack = ::pop)
                is Screen.TeacherAvailability -> TeacherAvailabilityScreen(
                    teacherId = screen.teacherId,
                    teacherName = screen.teacherName,
                    onBack = ::pop,
                )
                is Screen.DefinePeriods -> DefinePeriodsScreen(onBack = ::pop)
            }
        }
    }
}
