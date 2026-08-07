package com.jpagdi.cromascheduler.host

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.designsystem.CromaSchedulerTheme
import com.jpagdi.cromascheduler.di.CromaApplication
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.ui.DefinePeriodsScreen
import com.jpagdi.cromascheduler.ui.ExportScreen
import com.jpagdi.cromascheduler.ui.GenerateScreen
import com.jpagdi.cromascheduler.ui.ImportScreen
import com.jpagdi.cromascheduler.ui.RepairScreen
import com.jpagdi.cromascheduler.ui.ResultsScreen
import com.jpagdi.cromascheduler.ui.SettingsTabScreen
import com.jpagdi.cromascheduler.ui.TeacherAvailabilityScreen
import com.jpagdi.cromascheduler.ui.TeachersScreen
import com.jpagdi.cromascheduler.ui.ValidateScreen

/**
 * The app is intentionally MIXED: Home and Timetable Detail (see [com.jpagdi.cromascheduler.home])
 * are plain Android Views + XML layouts + a DrawerLayout, matching MCQ Quick Check's own
 * Activity/XML architecture; everything they hand off to — Import, Generate, Validate, Repair,
 * Results, Export, Teachers, Teacher Availability, Define Periods, Settings — is still the existing,
 * already-working Jetpack Compose screen from earlier phases, just hosted one-per-Activity instead
 * of behind a hand-rolled Compose back stack. This file is the ONLY place that bridges the two:
 * every XML screen navigates by calling [start], never by touching Compose types directly, and every
 * Compose screen keeps using its own `onBack`/callback lambdas exactly as before, wired here to
 * either [finish] or [start]-ing the next host. Nothing about the Compose screens themselves had to
 * change to support this except adding a couple of missing `onBack` parameters (Settings, Teachers)
 * that only made sense once they could be reached outside the old bottom-tab flow.
 */
class ComposeHostActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CromaApplication).container
        val screen = HostScreen.fromIntent(intent)

        setContent {
            CromaSchedulerTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        when (screen) {
                            is HostScreen.Import -> ImportScreen(onBack = ::finish, initialSessionType = screen.sessionType)
                            is HostScreen.Generate -> GenerateScreen(
                                onBack = ::finish,
                                onGenerated = { runId ->
                                    startActivity(com.jpagdi.cromascheduler.home.TimetableDetailActivity.intent(this, runId))
                                    finish()
                                },
                                initialSessionType = screen.sessionType,
                            )
                            is HostScreen.Validate -> ValidateScreen(
                                runId = screen.runId,
                                onBack = ::finish,
                                onRepair = { runId -> startActivity(HostScreen.Repair(runId).toIntent(this)) },
                            )
                            is HostScreen.Repair -> RepairScreen(
                                runId = screen.runId,
                                onBack = ::finish,
                                onRepaired = { newRunId ->
                                    startActivity(com.jpagdi.cromascheduler.home.TimetableDetailActivity.intent(this, newRunId))
                                    finish()
                                },
                            )
                            is HostScreen.Results -> ResultsScreen(
                                runId = screen.runId,
                                onBack = ::finish,
                                onExport = { runId, runName -> startActivity(HostScreen.Export(runId, runName).toIntent(this)) },
                            )
                            is HostScreen.Export -> ExportScreen(runId = screen.runId, runName = screen.runName, onBack = ::finish)
                            is HostScreen.Teachers -> TeachersScreen(
                                onBack = ::finish,
                                onSelectTeacher = { teacher -> startActivity(HostScreen.TeacherAvailability(teacher.id, teacher.name).toIntent(this)) },
                            )
                            is HostScreen.TeacherAvailability -> TeacherAvailabilityScreen(
                                teacherId = screen.teacherId,
                                teacherName = screen.teacherName,
                                onBack = ::finish,
                            )
                            is HostScreen.DefinePeriods -> DefinePeriodsScreen(onBack = ::finish)
                            is HostScreen.Settings -> SettingsTabScreen(
                                onBack = ::finish,
                                onDefinePeriods = { startActivity(HostScreen.DefinePeriods.toIntent(this)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val EXTRA_KIND = "kind"
private const val EXTRA_RUN_ID = "runId"
private const val EXTRA_RUN_NAME = "runName"
private const val EXTRA_TEACHER_ID = "teacherId"
private const val EXTRA_TEACHER_NAME = "teacherName"
private const val EXTRA_SESSION_TYPE = "sessionType"

sealed class HostScreen {
    data class Import(val sessionType: SessionTypeEntity? = null) : HostScreen()
    data class Generate(val sessionType: SessionTypeEntity? = null) : HostScreen()
    data class Validate(val runId: String) : HostScreen()
    data class Repair(val runId: String) : HostScreen()
    data class Results(val runId: String) : HostScreen()
    data class Export(val runId: String, val runName: String) : HostScreen()
    data object Teachers : HostScreen()
    data class TeacherAvailability(val teacherId: String, val teacherName: String) : HostScreen()
    data object DefinePeriods : HostScreen()
    data object Settings : HostScreen()

    fun toIntent(context: Context): Intent {
        val intent = Intent(context, ComposeHostActivity::class.java)
        intent.putExtra(EXTRA_KIND, this::class.simpleName)
        when (this) {
            is Import -> sessionType?.let { intent.putExtra(EXTRA_SESSION_TYPE, it.name) }
            is Generate -> sessionType?.let { intent.putExtra(EXTRA_SESSION_TYPE, it.name) }
            is Validate -> intent.putExtra(EXTRA_RUN_ID, runId)
            is Repair -> intent.putExtra(EXTRA_RUN_ID, runId)
            is Results -> intent.putExtra(EXTRA_RUN_ID, runId)
            is Export -> {
                intent.putExtra(EXTRA_RUN_ID, runId)
                intent.putExtra(EXTRA_RUN_NAME, runName)
            }
            is TeacherAvailability -> {
                intent.putExtra(EXTRA_TEACHER_ID, teacherId)
                intent.putExtra(EXTRA_TEACHER_NAME, teacherName)
            }
            Teachers, DefinePeriods, Settings -> Unit
        }
        return intent
    }

    companion object {
        fun fromIntent(intent: Intent): HostScreen {
            val kind = intent.getStringExtra(EXTRA_KIND)
            val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE)?.let {
                runCatching { SessionTypeEntity.valueOf(it) }.getOrNull()
            }
            return when (kind) {
                "Import" -> Import(sessionType)
                "Generate" -> Generate(sessionType)
                "Validate" -> Validate(intent.getStringExtra(EXTRA_RUN_ID).orEmpty())
                "Repair" -> Repair(intent.getStringExtra(EXTRA_RUN_ID).orEmpty())
                "Results" -> Results(intent.getStringExtra(EXTRA_RUN_ID).orEmpty())
                "Export" -> Export(intent.getStringExtra(EXTRA_RUN_ID).orEmpty(), intent.getStringExtra(EXTRA_RUN_NAME).orEmpty())
                "TeacherAvailability" -> TeacherAvailability(
                    intent.getStringExtra(EXTRA_TEACHER_ID).orEmpty(),
                    intent.getStringExtra(EXTRA_TEACHER_NAME).orEmpty(),
                )
                "DefinePeriods" -> DefinePeriods
                "Settings" -> Settings
                else -> Teachers
            }
        }
    }
}
