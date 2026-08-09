package com.jpagdi.cromascheduler.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.prefs.AppPreferencesStore
import com.jpagdi.cromascheduler.engine.coloring.ColoringAlgorithmRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: AppPreferencesStore) : ViewModel() {

    val algorithmNames: List<String> = ColoringAlgorithmRegistry.algorithms.keys.sorted()

    /** null while the DataStore read is in flight — GenerateScreen and this screen both treat null as "use the engine's own default (DSATUR)". */
    val defaultAlgorithmName = store.defaultAlgorithmName
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setDefaultAlgorithm(name: String) {
        viewModelScope.launch { store.setDefaultAlgorithmName(name) }
    }

    companion object {
        fun factory(store: AppPreferencesStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(store) as T
        }
    }
}
