package com.jpagdi.cromascheduler.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "chroma_prefs")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val DEFAULT_ALGORITHM_KEY = stringPreferencesKey("default_algorithm")

/**
 * Stores plain Strings rather than depending on designsystem's `ThemeMode` or engine's algorithm
 * types directly — :core:data intentionally has no dependency on :core:designsystem (data is
 * lower-level than UI theming), so any enum<->String mapping happens one layer up, in :app, which
 * already depends on both. (Previously named ThemePreferenceStore, before Settings needed a second
 * preference — same one DataStore file, just no longer theme-only.)
 */
class AppPreferencesStore(private val context: Context) {

    val themeModeName: Flow<String?> = context.appDataStore.data.map { it[THEME_MODE_KEY] }

    suspend fun setThemeModeName(name: String) {
        context.appDataStore.edit { it[THEME_MODE_KEY] = name }
    }

    /** null means "no preference saved yet" — GenerateScreen falls back to the engine's own default (DSATUR) in that case, same as before this existed. */
    val defaultAlgorithmName: Flow<String?> = context.appDataStore.data.map { it[DEFAULT_ALGORITHM_KEY] }

    suspend fun setDefaultAlgorithmName(name: String) {
        context.appDataStore.edit { it[DEFAULT_ALGORITHM_KEY] = name }
    }
}
