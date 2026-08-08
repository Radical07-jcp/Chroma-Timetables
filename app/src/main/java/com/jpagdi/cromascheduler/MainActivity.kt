package com.jpagdi.cromascheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.designsystem.CromaSchedulerTheme
import com.jpagdi.cromascheduler.designsystem.ThemeMode
import com.jpagdi.cromascheduler.di.CromaApplication
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.navigation.CromaRoutes
import com.jpagdi.cromascheduler.ui.*
import com.jpagdi.cromascheduler.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CromaApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(container.themePreferenceStore))
                val themeMode by themeViewModel.themeMode.collectAsState()

                CromaSchedulerTheme(themeMode = themeMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        CromaApp(currentThemeMode = themeMode, onThemeModeChange = themeViewModel::setThemeMode)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CromaApp(
    currentThemeMode: ThemeMode?,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun closeDrawerThen(action: () -> Unit) {
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                actions = SidebarActions(
                    onHome = { closeDrawerThen { navController.navigate(CromaRoutes.HOME) { popUpTo(CromaRoutes.HOME) { inclusive = true } } } },
                    onTeachers = { closeDrawerThen { navController.navigate(CromaRoutes.TEACHERS) } },
                    onDefinePeriods = { closeDrawerThen { navController.navigate(CromaRoutes.DEFINE_PERIODS) } },
                    onRepair = { closeDrawerThen { navController.navigate(CromaRoutes.REPAIR_UPLOAD) } },
                    onSettings = { closeDrawerThen { navController.navigate(CromaRoutes.SETTINGS) } },
                ),
            )
        },
    ) {
        NavHost(navController = navController, startDestination = CromaRoutes.HOME) {
            composable(CromaRoutes.HOME) {
                HomeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenWorkspace = { type -> navController.navigate(CromaRoutes.workspace(type.name)) },
                )
            }
            composable(
                CromaRoutes.WORKSPACE,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = SessionTypeEntity.valueOf(entry.arguments!!.getString("type")!!)
                TimetableWorkspaceScreen(
                    sessionType = type,
                    onBack = { navController.popBackStack() },
                    onImport = { navController.navigate(CromaRoutes.import(type.name)) },
                    onGenerate = { navController.navigate(CromaRoutes.generate(type.name)) },
                    onValidate = { runId -> navController.navigate(CromaRoutes.validate(runId)) },
                    onOptimize = { runId -> navController.navigate(CromaRoutes.optimize(runId)) },
                    onResults = { runId -> navController.navigate(CromaRoutes.results(runId)) },
                    onExport = { runId, runName -> navController.navigate(CromaRoutes.export(runId, runName)) },
                    onDefinePeriods = { navController.navigate(CromaRoutes.DEFINE_PERIODS) },
                )
            }
            composable(
                CromaRoutes.IMPORT,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = SessionTypeEntity.valueOf(entry.arguments!!.getString("type")!!)
                ImportScreen(sessionType = type, onBack = { navController.popBackStack() })
            }
            composable(
                CromaRoutes.GENERATE,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = SessionTypeEntity.valueOf(entry.arguments!!.getString("type")!!)
                GenerateScreen(
                    sessionType = type,
                    onBack = { navController.popBackStack() },
                    onDefinePeriods = { navController.navigate(CromaRoutes.DEFINE_PERIODS) },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                CromaRoutes.VALIDATE,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments!!.getString("runId")!!
                ValidateScreen(
                    runId = runId,
                    onBack = { navController.popBackStack() },
                    onRepair = { navController.navigate(CromaRoutes.repair(runId)) },
                )
            }
            composable(
                CromaRoutes.REPAIR,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments!!.getString("runId")!!
                RepairScreen(
                    runId = runId,
                    onBack = { navController.popBackStack() },
                    onOptimize = { newRunId -> navController.navigate(CromaRoutes.optimize(newRunId)) },
                    onViewTimetable = { newRunId -> navController.navigate(CromaRoutes.results(newRunId)) },
                )
            }
            composable(CromaRoutes.REPAIR_UPLOAD) {
                RepairUploadScreen(
                    onBack = { navController.popBackStack() },
                    onImported = { runId -> navController.navigate(CromaRoutes.repair(runId)) { popUpTo(CromaRoutes.REPAIR_UPLOAD) { inclusive = true } } },
                )
            }
            composable(
                CromaRoutes.OPTIMIZE,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments!!.getString("runId")!!
                OptimizeScreen(
                    runId = runId,
                    onBack = { navController.popBackStack() },
                    onViewTimetable = { newRunId -> navController.navigate(CromaRoutes.results(newRunId)) },
                )
            }
            composable(
                CromaRoutes.RESULTS,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments!!.getString("runId")!!
                ResultsScreen(
                    runId = runId,
                    onBack = { navController.popBackStack() },
                    onExport = { rId, runName -> navController.navigate(CromaRoutes.export(rId, runName)) },
                )
            }
            composable(
                CromaRoutes.EXPORT,
                arguments = listOf(
                    navArgument("runId") { type = NavType.StringType },
                    navArgument("runName") { type = NavType.StringType },
                ),
            ) { entry ->
                val runId = entry.arguments!!.getString("runId")!!
                val runName = URLDecoder.decode(entry.arguments!!.getString("runName")!!, "UTF-8")
                ExportScreen(runId = runId, runName = runName, onBack = { navController.popBackStack() })
            }
            composable(CromaRoutes.TEACHERS) {
                TeachersScreen(
                    onBack = { navController.popBackStack() },
                    onSelectTeacher = { teacher -> navController.navigate(CromaRoutes.teacherAvailability(teacher.id, teacher.name)) },
                )
            }
            composable(
                CromaRoutes.TEACHER_AVAILABILITY,
                arguments = listOf(
                    navArgument("teacherId") { type = NavType.StringType },
                    navArgument("teacherName") { type = NavType.StringType },
                ),
            ) { entry ->
                val teacherId = entry.arguments!!.getString("teacherId")!!
                val teacherName = URLDecoder.decode(entry.arguments!!.getString("teacherName")!!, "UTF-8")
                TeacherAvailabilityScreen(teacherId = teacherId, teacherName = teacherName, onBack = { navController.popBackStack() })
            }
            composable(CromaRoutes.DEFINE_PERIODS) {
                DefinePeriodsScreen(onBack = { navController.popBackStack() })
            }
            composable(CromaRoutes.SETTINGS) {
                SettingsTabScreen(
                    onBack = { navController.popBackStack() },
                    onDefinePeriods = { navController.navigate(CromaRoutes.DEFINE_PERIODS) },
                )
            }
        }
    }
}
