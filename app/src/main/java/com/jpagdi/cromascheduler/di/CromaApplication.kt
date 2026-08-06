package com.jpagdi.cromascheduler.di

import android.app.Application
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CromaApplication : Application() {
    lateinit var container: AppContainer
        private set

    // A process-lifetime scope for one-shot startup work (currently just default
    // period-config seeding) that must survive independent of any single screen's
    // ViewModel — there's no screen "responsible" for app startup itself.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.scheduleRepository.ensureDefaultPeriodConfigExists()
        }
    }
}

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided — CromaSchedulerTheme's content must be wrapped with CompositionLocalProvider(LocalAppContainer provides ...)")
}
