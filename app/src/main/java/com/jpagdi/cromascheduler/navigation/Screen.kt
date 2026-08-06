package com.jpagdi.cromascheduler.navigation

/**
 * Hand-rolled sealed class + mutableStateOf back stack, no Navigation-Compose
 * dependency — see the original doc comment logic: the app's flow doesn't need
 * deep links or saved back-stack state across process death yet.
 *
 * The four *Tab screens are the bottom-nav destinations (bottom bar visible only
 * when the back stack is exactly one of these). Everything else pushes on top
 * with its own back button and no bottom bar, same pattern as before.
 */
sealed class Screen {
    data object HomeTab : Screen()
    data object TimetableTab : Screen()
    data object TeachersTab : Screen()
    data object SettingsTab : Screen()

    data object Import : Screen()
    data object Generate : Screen()
    data class PickRunForValidate(val placeholder: Unit = Unit) : Screen()
    data class Validate(val runId: String) : Screen()
    data class PickRunForRepair(val placeholder: Unit = Unit) : Screen()
    data class Repair(val runId: String) : Screen()
    data class Results(val runId: String) : Screen()
    data class PickRunForExport(val placeholder: Unit = Unit) : Screen()
    data class Export(val runId: String, val runName: String) : Screen()
    data class TeacherAvailability(val teacherId: String, val teacherName: String) : Screen()
    data object DefinePeriods : Screen()

    companion object {
        val bottomTabs = listOf(HomeTab, TimetableTab, TeachersTab, SettingsTab)
    }
}
