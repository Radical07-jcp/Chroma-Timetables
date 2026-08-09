package com.jpagdi.cromascheduler.di

import android.app.Application
import androidx.compose.runtime.compositionLocalOf

class CromaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided — CromaSchedulerTheme's content must be wrapped with CompositionLocalProvider(LocalAppContainer provides ...)")
}
