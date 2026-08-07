package com.jpagdi.cromascheduler.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.jpagdi.cromascheduler.R
import com.jpagdi.cromascheduler.data.entity.ScheduleRunEntity
import com.jpagdi.cromascheduler.di.CromaApplication
import com.jpagdi.cromascheduler.host.HostScreen
import com.jpagdi.cromascheduler.ui.label
import kotlinx.coroutines.launch

/**
 * The "Specific Timetable" screen — this is where the old Home Dashboard's Validate/Repair/
 * Optimize/Export buttons actually live now, each already scoped to [runId] so there's no
 * intermediate "pick a run" step in front of any of them anymore. View Timetable is the primary
 * pinned action; the rest are the CardRow list above it. All five hand off to
 * [com.jpagdi.cromascheduler.host.ComposeHostActivity] — the real Validate/Repair/Optimize/Results/
 * Export logic hasn't moved, only where it's launched from.
 */
class TimetableDetailActivity : ComponentActivity() {

    private lateinit var runId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timetable_detail)

        runId = intent.getStringExtra(EXTRA_RUN_ID) ?: run { finish(); return }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnExport).setOnClickListener {
            val title = findViewById<TextView>(R.id.tvDetailTitle).text.toString()
            startActivity(HostScreen.Export(runId, title).toIntent(this))
        }
        findViewById<View>(R.id.btnDelete).setOnClickListener { confirmDelete() }

        findViewById<View>(R.id.rowValidate).setOnClickListener {
            startActivity(HostScreen.Validate(runId).toIntent(this))
        }
        findViewById<View>(R.id.rowRepair).setOnClickListener {
            startActivity(HostScreen.Repair(runId).toIntent(this))
        }
        findViewById<View>(R.id.rowOptimize).setOnClickListener {
            // Optimize doesn't have its own dedicated Compose screen yet — Validate already
            // surfaces the same violation list and is where the optimize/repair actions branch
            // from, so it's the correct landing spot rather than a dead button.
            startActivity(HostScreen.Validate(runId).toIntent(this))
        }
        findViewById<View>(R.id.btnViewTimetable).setOnClickListener {
            startActivity(HostScreen.Results(runId).toIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        loadRun()
    }

    private fun loadRun() {
        val container = (application as CromaApplication).container
        lifecycleScope.launch {
            val run = container.scheduleRepository.getRun(runId)
            if (run == null) {
                finish()
                return@launch
            }
            val conflictCount = container.scheduleRepository.getConflicts(runId).size
            bind(run, conflictCount)
        }
    }

    private fun bind(run: ScheduleRunEntity, conflictCount: Int) {
        findViewById<TextView>(R.id.tvDetailTitle).text = run.name
        findViewById<TextView>(R.id.tvDetailSubtitle).text = "${run.sessionType.label()} • ${run.algorithmUsed}"
        findViewById<TextView>(R.id.tvStatusLine).text =
            "Generated in ${run.executionTimeMillis} ms • ${if (conflictCount == 0) "no conflicts" else "$conflictCount conflict${if (conflictCount == 1) "" else "s"}"}"

        val badge = findViewById<TextView>(R.id.tvConflictBadge)
        if (conflictCount > 0) {
            badge.visibility = View.VISIBLE
            badge.text = conflictCount.toString()
            badge.background.mutate().setTint(androidx.core.content.ContextCompat.getColor(this, R.color.status_conflicts))
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete this timetable?")
            .setMessage("This removes the generated schedule and its conflict history. The teachers/subjects/rooms/sessions it was built from are not affected.")
            .setPositiveButton("Delete") { _, _ ->
                val container = (application as CromaApplication).container
                lifecycleScope.launch {
                    container.scheduleRepository.deleteRun(runId)
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val EXTRA_RUN_ID = "runId"
        fun intent(context: Context, runId: String): Intent =
            Intent(context, TimetableDetailActivity::class.java).putExtra(EXTRA_RUN_ID, runId)
    }
}
