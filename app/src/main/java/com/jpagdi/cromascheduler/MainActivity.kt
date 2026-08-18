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
import com.jpagdi.cromascheduler.viewmodel.AccentColorViewModel
import com.jpagdi.cromascheduler.viewmodel.CreateTimetableViewModel
import com.jpagdi.cromascheduler.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CromaApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(container.appPreferencesStore))
                val themeMode by themeViewModel.themeMode.collectAsState()

                val accentViewModel: AccentColorViewModel = viewModel(factory = AccentColorViewModel.factory(container.appPreferencesStore))
                val groupA by accentViewModel.groupA.collectAsState()
                val groupB by accentViewModel.groupB.collectAsState()

                CromaSchedulerTheme(themeMode = themeMode, groupA = groupA, groupB = groupB) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        CromaApp(
                            currentThemeMode = themeMode,
                            onThemeModeChange = themeViewModel::setThemeMode,
                            accentViewModel = accentViewModel,
                        )
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
    accentViewModel: AccentColorViewModel,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Two independent wizard instances, each living for the lifetime of MainActivity (not scoped
    // to a single nav destination) so their state survives across the 3 screens each wizard spans.
    // Separate instances because Create and Repair-upload can each be mid-flow independently —
    // starting one shouldn't clobber progress in the other.
    val createWizard: CreateTimetableViewModel = viewModel()
    val repairWizard: CreateTimetableViewModel = viewModel(key = "repairWizard")

    fun closeDrawerThen(action: () -> Unit) {
        // Navigation must wait for the drawer transition to finish. Navigating immediately while
        // ModalNavigationDrawer is still settling can leave the window in a blank/frozen-looking
        // state on some devices.
        scope.launch {
            drawerState.close()
            action()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                actions = SidebarActions(
                    onHome = { closeDrawerThen { navController.navigate(CromaRoutes.HOME) { popUpTo(CromaRoutes.HOME) { inclusive = true } } } },
                    onNewTimetable = {
                        closeDrawerThen {
                            createWizard.reset()
                            navController.navigate(CromaRoutes.CREATE_CHOOSE_TYPE)
                        }
                    },
                    onRepair = {
                        closeDrawerThen {
                            repairWizard.reset()
                            navController.navigate(CromaRoutes.REPAIR_CHOOSE_TYPE)
                        }
                    },
                    onSettings = { closeDrawerThen { navController.navigate(CromaRoutes.SETTINGS) } },
                    onAbout = { closeDrawerThen { navController.navigate(CromaRoutes.ABOUT) } },
                ),
            )
        },
    ) {
        NavHost(navController = navController, startDestination = CromaRoutes.HOME) {
            composable(CromaRoutes.HOME) {
                HomeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenTimetable = { runId -> navController.navigate(CromaRoutes.timetableDetail(runId)) },
                    onCreateTimetable = {
                        createWizard.reset()
                        navController.navigate(CromaRoutes.CREATE_CHOOSE_TYPE)
                    },
                )
            }

            // --- New Timetable wizard ---
            composable(CromaRoutes.CREATE_CHOOSE_TYPE) {
                ChooseTimetableTypeScreen(
                    viewModel = createWizard,
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(CromaRoutes.CREATE_DEFINE_PERIODS) },
                )
            }
            composable(CromaRoutes.CREATE_DEFINE_PERIODS) {
                DefineTimetablePeriodsScreen(
                    viewModel = createWizard,
                    onBack = { navController.popBackStack() },
                    onNext = {
                        createWizard.sessionType?.let { navController.navigate(CromaRoutes.createImport(it.name)) }
                    },
                )
            }
            composable(
                CromaRoutes.CREATE_IMPORT,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = SessionTypeEntity.valueOf(entry.arguments!!.getString("type")!!)
                ImportScreen(
                    sessionType = type,
                    onBack = { navController.popBackStack() },
                    onImported = {
                        navController.navigate(CromaRoutes.CREATE_GENERATE) {
                            popUpTo(CromaRoutes.CREATE_IMPORT) { inclusive = true }
                        }
                    },
                )
            }
            composable(CromaRoutes.CREATE_GENERATE) {
                GenerateScreen(
                    wizard = createWizard,
                    onBack = { navController.popBackStack() },
                    onImportData = { createWizard.sessionType?.let { navController.navigate(CromaRoutes.createImport(it.name)) } },
                    onDone = { runId ->
                        createWizard.reset()
                        navController.navigate(CromaRoutes.timetableDetail(runId)) {
                            popUpTo(CromaRoutes.HOME)
                        }
                    },
                )
            }

            // --- Repair-upload wizard: same first two steps, different third step ---
            composable(CromaRoutes.REPAIR_CHOOSE_TYPE) {
                ChooseTimetableTypeScreen(
                    viewModel = repairWizard,
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(CromaRoutes.REPAIR_DEFINE_PERIODS) },
                )
            }
            composable(CromaRoutes.REPAIR_DEFINE_PERIODS) {
                DefineTimetablePeriodsScreen(
                    viewModel = repairWizard,
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(CromaRoutes.REPAIR_UPLOAD) },
                )
            }
            composable(CromaRoutes.REPAIR_UPLOAD) {
                RepairUploadScreen(
                    wizard = repairWizard,
                    onBack = { navController.popBackStack() },
                    onImported = { runId ->
                        navController.navigate(CromaRoutes.repair(runId)) { popUpTo(CromaRoutes.REPAIR_CHOOSE_TYPE) { inclusive = true } }
                    },
                )
            }

            composable(
                CromaRoutes.IMPORT,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = SessionTypeEntity.valueOf(entry.arguments!!.getString("type")!!)
                ImportScreen(
                    sessionType = type,
                    onBack = { navController.popBackStack() },
                    onImported = { navController.popBackStack() },
                )
            }
            composable(
                CromaRoutes.TIMETABLE_DETAIL,
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments!!.getString("runId")!!
                TimetableDetailScreen(
                    runId = runId,
                    onBack = { navController.popBackStack() },
                    onResults = { navController.navigate(CromaRoutes.results(runId)) },
                    onExport = { rId, runName -> navController.navigate(CromaRoutes.export(rId, runName)) },
                    onTeachers = { navController.navigate(CromaRoutes.TEACHERS) },
                    onDeleted = { navController.popBackStack(CromaRoutes.HOME, inclusive = false) },
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
            composable(CromaRoutes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onPickGroupA = { navController.navigate(CromaRoutes.ACCENT_GROUP_A) },
                    onPickGroupB = { navController.navigate(CromaRoutes.ACCENT_GROUP_B) },
                    onOpenTeachers = { navController.navigate(CromaRoutes.TEACHERS) },
                )
            }
            composable(CromaRoutes.ACCENT_GROUP_A) {
                val current by accentViewModel.groupA.collectAsState()
                AccentColorPickerScreen(
                    title = "Top Panel Accent",
                    current = current,
                    onBack = { navController.popBackStack() },
                    onPick = { color -> accentViewModel.setGroupA(color); navController.popBackStack() },
                    onReset = { accentViewModel.resetGroupA(); navController.popBackStack() },
                )
            }
            composable(CromaRoutes.ACCENT_GROUP_B) {
                val current by accentViewModel.groupB.collectAsState()
                AccentColorPickerScreen(
                    title = "Button Accent",
                    current = current,
                    onBack = { navController.popBackStack() },
                    onPick = { color -> accentViewModel.setGroupB(color); navController.popBackStack() },
                    onReset = { accentViewModel.resetGroupB(); navController.popBackStack() },
                )
            }
            composable(CromaRoutes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
