package com.jpagdi.cromascheduler.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "chroma_prefs")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val DEFAULT_ALGORITHM_KEY = stringPreferencesKey("default_algorithm")
private val ACCENT_GROUP_A_KEY = intPreferencesKey("accent_group_a")
private val ACCENT_GROUP_B_KEY = intPreferencesKey("accent_group_b")

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

    /**
     * Stored as packed ARGB Ints (same representation `android.graphics.Color`/`Color.toArgb()`
     * both already use), matching how the reference app's AccentColorPrefs persisted these in
     * SharedPreferences — null means "no override saved, use the design system's own default"
     * (maroon/gold), same meaning null already has for theme mode and default algorithm above.
     */
    val accentGroupA: Flow<Int?> = context.appDataStore.data.map { it[ACCENT_GROUP_A_KEY] }
    val accentGroupB: Flow<Int?> = context.appDataStore.data.map { it[ACCENT_GROUP_B_KEY] }

    suspend fun setAccentGroupA(argb: Int) {
        context.appDataStore.edit { it[ACCENT_GROUP_A_KEY] = argb }
    }

    suspend fun setAccentGroupB(argb: Int) {
        context.appDataStore.edit { it[ACCENT_GROUP_B_KEY] = argb }
    }

    suspend fun resetAccentGroupA() {
        context.appDataStore.edit { it.remove(ACCENT_GROUP_A_KEY) }
    }

    suspend fun resetAccentGroupB() {
        context.appDataStore.edit { it.remove(ACCENT_GROUP_B_KEY) }
    }
}
