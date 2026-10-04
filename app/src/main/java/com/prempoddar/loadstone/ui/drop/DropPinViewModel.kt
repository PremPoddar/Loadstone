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
    /**
     * The estimate frozen when the user tapped "Save pin here". The live [fix] keeps following
     * them while they name the pin or take a photo (the engine is built to track movement), so
     * saving from it put the pin wherever they had wandered to by the time they tapped Save.
     */
    val lockedFix: PinFix? = null,
) {
    /**
     * A pin is only worth saving once the estimate is tight enough to walk back to. Saving a
     * loose fix bakes that error in permanently — the pin is then wrong every time you return.
     */
    val accurateEnough: Boolean
        get() = fix != null && fix.estAccuracyMeters <= DropPinViewModel.SAVE_MAX_ACCURACY

    val canSave: Boolean get() = accurateEnough && !saving

    /** Commit to the current estimate as the spot to save, or null if it is too loose. */
    fun lock(): DropUiState? = if (canSave) copy(lockedFix = fix) else null

    /** The pin to store, always at [lockedFix]; null if no spot has been locked in. */
    fun pinToSave(name: String, note: String?, category: PinCategory, photoPaths: List<String>): Pin? {
        val spot = lockedFix ?: return null
        return Pin(
            name = name.trim().ifBlank { "Pin" },
            note = note?.trim()?.ifBlank { null },
            latitude = spot.latitude,
            longitude = spot.longitude,
            accuracyMeters = spot.estAccuracyMeters,
            category = category,
            elevationMeters = spot.altitudeMeters,
            photoPaths = photoPaths,
        )
    }
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
            locationRepository.estimatedFixes()
                .catch { /* ignore; user sees the waiting state */ }
                .collect { fix ->
                    // "Stable" is the estimator's own verdict that more waiting will not help,
                    // plus an accuracy worth having. Counting samples instead used to call a pin
                    // ready purely because time had passed.
                    val stable = fix.settled && fix.estAccuracyMeters <= GOOD_ENOUGH_METERS
                    _state.value = _state.value.copy(fix = fix, stabilized = stable)
                }
        }
    }

    /** Freeze the current estimate as the spot to save, if it is accurate enough. */
    fun lockSpot() {
        _state.value.lock()?.let { _state.value = it }
    }

    /** The user backed out of naming the pin: let go of the frozen spot and keep measuring. */
    fun unlockSpot() {
        _state.value = _state.value.copy(lockedFix = null)
    }

    fun savePin(
        name: String,
        note: String?,
        category: PinCategory,
        photoPaths: List<String>,
        onSaved: () -> Unit,
    ) {
        val pin = _state.value.pinToSave(name, note, category, photoPaths) ?: return
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            pinRepository.upsert(pin)
            onSaved()
        }
    }

    companion object {
        const val GOOD_ENOUGH_METERS = 6f

        /**
         * Refuse to save beyond this. A pin saved at ±40 m sends you to the wrong place forever;
         * outdoors this threshold is reached within seconds, so it only blocks genuinely bad fixes.
         */
        const val SAVE_MAX_ACCURACY = 20f
    }
}
