package com.prempoddar.loadstone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.data.settings.Settings
import com.prempoddar.loadstone.data.settings.SettingsRepository
import com.prempoddar.loadstone.data.settings.ThemeMode
import com.prempoddar.loadstone.data.settings.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(system) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
