package com.prempoddar.loadstone.ui.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.data.location.CompassRepository
import com.prempoddar.loadstone.data.location.Heading
import com.prempoddar.loadstone.data.location.LocationRepository
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadarUiState(
    val pins: List<Pin> = emptyList(),
    val here: PinFix? = null,
    /** True-north heading of the phone, or null without a compass reading. */
    val heading: Double? = null,
)

@HiltViewModel
class RadarViewModel @Inject constructor(
    pins: PinRepository,
    private val locations: LocationRepository,
    private val compass: CompassRepository,
) : ViewModel() {
    private val fix = MutableStateFlow<PinFix?>(null)
    private val heading = MutableStateFlow<Heading?>(null)
    private var started = false

    val state: StateFlow<RadarUiState> = combine(pins.observeAll(), fix, heading) { p, f, h ->
        RadarUiState(
            p, f,
            if (h != null && f != null) GeoMath.normalize360(h.magneticDegrees + CompassRepository.declinationDegrees(f.latitude, f.longitude, f.altitudeMeters ?: 0.0).toDouble()) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RadarUiState())

    /** Call once location permission is granted. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch { locations.estimatedFixes().catch { }.collect { fix.value = it } }
        if (compass.hasCompass()) viewModelScope.launch { compass.headings().catch { }.collect { heading.value = it } }
    }
}
