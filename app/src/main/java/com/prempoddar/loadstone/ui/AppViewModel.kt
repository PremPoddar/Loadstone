package com.prempoddar.loadstone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
import com.prempoddar.loadstone.data.pin.PinRepository
import com.prempoddar.loadstone.data.settings.Settings
import com.prempoddar.loadstone.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Top-level state: theme/start destination plus any pending imported coordinate. */
@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val pinRepository: PinRepository,
) : ViewModel() {
    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _pendingImport = MutableStateFlow<Pair<Double, Double>?>(null)
    val pendingImport: StateFlow<Pair<Double, Double>?> = _pendingImport.asStateFlow()

    fun onImportCoordinates(lat: Double, lon: Double) {
        _pendingImport.value = lat to lon
    }

    fun clearImport() {
        _pendingImport.value = null
    }

    fun saveImported(name: String, note: String?, category: PinCategory) {
        val (lat, lon) = _pendingImport.value ?: return
        viewModelScope.launch {
            pinRepository.upsert(
                Pin(
                    name = name.trim().ifBlank { "Imported pin" },
                    note = note?.trim()?.ifBlank { null },
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = 0f,
                    category = category,
                ),
            )
            _pendingImport.value = null
        }
    }
}
