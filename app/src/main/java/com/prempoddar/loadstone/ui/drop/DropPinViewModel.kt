package com.prempoddar.loadstone.ui.drop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.data.location.LocationRepository
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
import com.prempoddar.loadstone.data.pin.PinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DropUiState(
    val fix: PinFix? = null,
    val stabilized: Boolean = false,
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = fix != null && !saving
}

@HiltViewModel
class DropPinViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val pinRepository: PinRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DropUiState())
    val state: StateFlow<DropUiState> = _state.asStateFlow()

    private var started = false

    /** Begin collecting fixes. Call once permission is granted. */
    fun startMeasuring() {
        if (started) return
        started = true
        viewModelScope.launch {
            locationRepository.averagedFixes()
                .catch { /* ignore; user sees the waiting state */ }
                .collect { fix ->
                    val stable = fix.estAccuracyMeters <= GOOD_ENOUGH_METERS && fix.sampleCount >= MIN_SAMPLES
                    _state.value = _state.value.copy(fix = fix, stabilized = stable)
                }
        }
    }

    fun savePin(name: String, note: String?, category: PinCategory, onSaved: () -> Unit) {
        val fix = _state.value.fix ?: return
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            pinRepository.upsert(
                Pin(
                    name = name.trim().ifBlank { "Pin" },
                    note = note?.trim()?.ifBlank { null },
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.estAccuracyMeters,
                    category = category,
                ),
            )
            onSaved()
        }
    }

    companion object {
        const val GOOD_ENOUGH_METERS = 5f
        const val MIN_SAMPLES = 8
    }
}
