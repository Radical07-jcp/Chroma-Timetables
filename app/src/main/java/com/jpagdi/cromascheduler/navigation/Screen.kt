package com.jpagdi.cromascheduler.navigation

/**
 * Deliberately a hand-rolled sealed class + mutableStateOf back stack instead of
 * androidx.navigation:navigation-compose. The app's whole flow is Home -> one of
 * five destinations -> (optionally) Results, with no deep links and no need for
 * saved back-stack state across process death yet — a real nav graph dependency
 * isn't earning its keep at this size. Revisit if the flow grows more branches.
 */
sealed class Screen {
    data object Home : Screen()
    data object Import : Screen()
    data object Generate : Screen()
    data class PickRunForValidate(val placeholder: Unit = Unit) : Screen()
    data class Validate(val runId: String) : Screen()
    data class PickRunForRepair(val placeholder: Unit = Unit) : Screen()
    data class Repair(val runId: String) : Screen()
    data class Results(val runId: String) : Screen()
    data class PickRunForExport(val placeholder: Unit = Unit) : Screen()
    data class Export(val runId: String) : Screen()
}
