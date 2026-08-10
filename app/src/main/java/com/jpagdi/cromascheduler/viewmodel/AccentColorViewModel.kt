package com.jpagdi.cromascheduler.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jpagdi.cromascheduler.data.prefs.AppPreferencesStore
import com.jpagdi.cromascheduler.designsystem.AccentPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Compose-side half of the reference app's AccentColorPrefs — see AccentPrefs.kt in
 * designsystem for the math/presets this reads from, and Theme.kt for how groupA/groupB actually
 * reach every screen (CromaSchedulerTheme -> LocalHeaderAccent/LocalButtonAccent), live, with no
 * View-tree walking involved.
 */
class AccentColorViewModel(private val store: AppPreferencesStore) : ViewModel() {

    val groupA = store.accentGroupA
        .map { argb -> argb?.let { Color(it) } ?: AccentPrefs.DEFAULT_GROUP_A }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentPrefs.DEFAULT_GROUP_A)

    val groupB = store.accentGroupB
        .map { argb -> argb?.let { Color(it) } ?: AccentPrefs.DEFAULT_GROUP_B }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentPrefs.DEFAULT_GROUP_B)

    fun setGroupA(color: Color) {
        viewModelScope.launch { store.setAccentGroupA(color.toArgb()) }
    }

    fun setGroupB(color: Color) {
        viewModelScope.launch { store.setAccentGroupB(color.toArgb()) }
    }

    fun resetGroupA() {
        viewModelScope.launch { store.resetAccentGroupA() }
    }

    fun resetGroupB() {
        viewModelScope.launch { store.resetAccentGroupB() }
    }

    companion object {
        fun factory(store: AppPreferencesStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AccentColorViewModel(store) as T
        }
    }
}
