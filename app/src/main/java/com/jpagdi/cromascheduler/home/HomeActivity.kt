package com.jpagdi.cromascheduler.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.jpagdi.cromascheduler.R
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.di.CromaApplication
import com.jpagdi.cromascheduler.host.HostScreen
import com.jpagdi.cromascheduler.ui.label
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Launcher Activity — replaces the old bottom-tab Compose MainActivity. Lists every
 * [ScheduleRunEntity] ("Timetable") as a card; tapping one opens [TimetableDetailActivity], which
 * is where the previous Home Dashboard's Validate/Repair/Optimize/Export/View buttons now live,
 * scoped to that one run. Only Import Data (header icon + drawer) and Generate New Schedule (FAB)
 * stay here, since those two are the only actions that aren't about one existing timetable.
 */
class HomeActivity : ComponentActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var timetablesContainer: LinearLayout
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        drawerLayout = findViewById(R.id.drawerLayout)
        timetablesContainer = findViewById(R.id.timetablesContainer)
        emptyView = findViewById(R.id.tvTimetablesEmpty)

        DrawerHelper.attach(
            drawerLayout = drawerLayout,
            hamburger = findViewById(R.id.btnOpenDrawer),
            actions = DrawerActions(
                onImportData = { startActivity(HostScreen.Import().toIntent(this)) },
                onGenerateSchedule = { startActivity(HostScreen.Generate().toIntent(this)) },
                onDefinePeriods = { startActivity(HostScreen.DefinePeriods.toIntent(this)) },
                onTeachers = { startActivity(HostScreen.Teachers.toIntent(this)) },
                onSettings = { startActivity(HostScreen.Settings.toIntent(this)) },
                onAbout = { startActivity(HostScreen.Settings.toIntent(this)) }, // Settings screen already hosts the About/no-AI panel
            ),
        )

        findViewById<View>(R.id.btnImport).setOnClickListener {
            startActivity(HostScreen.Import().toIntent(this))
        }
        findViewById<View>(R.id.fabGenerate).setOnClickListener {
            startActivity(HostScreen.Generate().toIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload on every return to Home (not just onCreate) — this is how the list picks up a run
        // that was just created/repaired/deleted one Activity up, with no shared ViewModel needed.
        refreshTimetables()
    }

    private fun refreshTimetables() {
        val container = (application as CromaApplication).container
        lifecycleScope.launch {
            val runs = container.scheduleRepository.getRuns().sortedByDescending { it.createdAtEpochMillis }
            val conflictCounts = container.scheduleRepository.getConflictCountsByRun()

            timetablesContainer.removeAllViews()
            emptyView.visibility = if (runs.isEmpty()) View.VISIBLE else View.GONE

            val inflater = LayoutInflater.from(this@HomeActivity)
            runs.forEach { run -> timetablesContainer.addView(buildRow(inflater, timetablesContainer, run, conflictCounts[run.id] ?: 0)) }
        }
    }

    private fun buildRow(inflater: LayoutInflater, parent: ViewGroup, run: ScheduleRunEntity, conflictCount: Int): View {
        val row = inflater.inflate(R.layout.row_timetable_card, parent, false)

        row.findViewById<TextView>(R.id.tvRowTitle).text = run.name
        row.findViewById<TextView>(R.id.tvRowSubtitle).text =
            "${run.sessionType.label()} • ${run.algorithmUsed} • ${formatDate(run.createdAtEpochMillis)}"

        val statusPill = row.findViewById<TextView>(R.id.tvRowStatus)
        applyStatus(statusPill, conflictCount)

        row.setOnClickListener {
            startActivity(TimetableDetailActivity.intent(this, run.id))
        }
        return row
    }

    companion object {
        private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

        /** Shared by [HomeActivity] and [TimetableDetailActivity] so the "clean vs conflicts" color logic can't drift between the list and the detail screen. Calls mutate() first — these pills share one @drawable/bg_pill_rounded resource, and tinting a drawable's shared constant state without mutating it first would recolor every pill on screen, not just this one. */
        fun applyStatus(pill: TextView, conflictCount: Int) {
            if (conflictCount > 0) {
                pill.text = "$conflictCount CONFLICT${if (conflictCount == 1) "" else "S"}"
                pill.background.mutate().setTint(ContextCompat.getColor(pill.context, R.color.status_conflicts))
            } else {
                pill.text = "CLEAN"
                pill.background.mutate().setTint(ContextCompat.getColor(pill.context, R.color.status_clean))
            }
        }
    }
}
