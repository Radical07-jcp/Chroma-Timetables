package com.jpagdi.cromascheduler.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "chroma_prefs")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

/**
 * Stores only the theme mode's enum NAME as a plain String ("LIGHT" / "DARK" / "BLACK") rather than
 * depending on designsystem's `ThemeMode` type directly — :core:data intentionally has no dependency
 * on :core:designsystem (data is lower-level than UI theming), so the enum<->String mapping happens
 * one layer up, in :app's ThemeViewModel, which already depends on both modules.
 */
class ThemePreferenceStore(private val context: Context) {

    val themeModeName: Flow<String?> = context.themeDataStore.data.map { it[THEME_MODE_KEY] }

    suspend fun setThemeModeName(name: String) {
        context.themeDataStore.edit { it[THEME_MODE_KEY] = name }
    }
}
