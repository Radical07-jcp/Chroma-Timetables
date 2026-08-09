package com.jpagdi.cromascheduler.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.prefs.AppPreferencesStore
import com.jpagdi.cromascheduler.designsystem.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The one place the DataStore-persisted String and designsystem's ThemeMode enum meet — see
 * AppPreferencesStore's doc comment for why that mapping doesn't live in :core:data itself.
 * `themeMode` is null until the DataStore read completes, which CromaSchedulerTheme already treats
 * as "follow system light/dark" (see Theme.kt), so there's no incorrect flash of the wrong theme.
 */
class ThemeViewModel(private val store: AppPreferencesStore) : ViewModel() {

    val themeMode = store.themeModeName
        .map { name -> name?.let { ThemeMode.fromName(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Cycles Light -> Dark -> Black -> Light — what the sidebar's single theme pill taps through. */
    fun cycleThemeMode() {
        val next = when (themeMode.value ?: ThemeMode.LIGHT) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.BLACK
            ThemeMode.BLACK -> ThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeModeName(mode.name) }
    }

    companion object {
        fun factory(store: AppPreferencesStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ThemeViewModel(store) as T
        }
    }
}
