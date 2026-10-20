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

    data class PendingImport(
        val lat: Double,
        val lon: Double,
        val name: String? = null,
        val note: String? = null,
    )

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    fun onImport(pending: PendingImport) {
        _pendingImport.value = pending
    }

    fun clearImport() {
        _pendingImport.value = null
    }

    fun saveImported(name: String, note: String?, category: PinCategory, photoPaths: List<String>, folder: String? = null) {
        val pending = _pendingImport.value ?: return
        val lat = pending.lat
        val lon = pending.lon
        viewModelScope.launch {
            pinRepository.upsert(
                Pin(
                    name = name.trim().ifBlank { "Imported pin" },
                    note = note?.trim()?.ifBlank { null },
                    latitude = lat,
                    longitude = lon,
                    // A shared link says nothing about how well the sender measured it: start
                    // modest, as a pin shared in words does, and let the first visit sharpen it.
                    accuracyMeters = com.prempoddar.loadstone.ui.home.HomeViewModel.SHARED_ACCURACY_METERS,
                    category = category,
                    photoPaths = photoPaths,
                    folder = folder,
                ),
            )
            _pendingImport.value = null
        }
    }
}
