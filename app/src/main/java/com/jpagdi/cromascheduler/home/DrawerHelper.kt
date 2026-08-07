package com.jpagdi.cromascheduler.home

import android.view.Gravity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout

/**
 * Every field is a callback rather than an Intent/Activity reference, so this stays a plain View
 * wiring helper with zero navigation knowledge of its own — HomeActivity is the only place that
 * knows what "Import Data" or "Settings" actually navigate to (via [com.jpagdi.cromascheduler.host.HostScreen]).
 */
data class DrawerActions(
    val onImportData: () -> Unit,
    val onGenerateSchedule: () -> Unit,
    val onDefinePeriods: () -> Unit,
    val onTeachers: () -> Unit,
    val onSettings: () -> Unit,
    val onAbout: () -> Unit,
)

object DrawerHelper {

    /** Wires [hamburger] to open [drawerLayout] and every row inside the included drawer_content.xml to its matching [actions] callback. Closes the drawer before firing the callback so the destination Activity doesn't launch behind a still-open drawer animation. */
    fun attach(drawerLayout: DrawerLayout, hamburger: View, actions: DrawerActions) {
        hamburger.setOnClickListener { drawerLayout.openDrawer(Gravity.END) }

        fun row(id: Int, action: () -> Unit) {
            drawerLayout.findViewById<View>(id)?.setOnClickListener {
                drawerLayout.closeDrawer(Gravity.END)
                action()
            }
        }

        row(com.jpagdi.cromascheduler.R.id.drawerImportData, actions.onImportData)
        row(com.jpagdi.cromascheduler.R.id.drawerGenerateSchedule, actions.onGenerateSchedule)
        row(com.jpagdi.cromascheduler.R.id.drawerDefinePeriods, actions.onDefinePeriods)
        row(com.jpagdi.cromascheduler.R.id.drawerTeachers, actions.onTeachers)
        row(com.jpagdi.cromascheduler.R.id.drawerSettings, actions.onSettings)
        row(com.jpagdi.cromascheduler.R.id.drawerAbout, actions.onAbout)
    }
}
