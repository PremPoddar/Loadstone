package com.prempoddar.loadstone.ui.navigate

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.data.location.CompassRepository
import com.prempoddar.loadstone.data.location.LocationRepository
import com.prempoddar.loadstone.data.photo.PhotoStore
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinRepository
import com.prempoddar.loadstone.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

data class NavigateUiState(
    val pin: Pin? = null,
    val arrowAngle: Float = 0f,
    val distanceMeters: Double? = null,
    val currentAccuracyMeters: Float? = null,
    val compassAccuracyClass: Int = 3,
    val arrived: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NavigateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pinRepository: PinRepository,
    private val locationRepository: LocationRepository,
    private val compassRepository: CompassRepository,
) : ViewModel() {

    private val pinId: Long = savedStateHandle[Routes.NAVIGATE_ARG_PIN_ID] ?: -1L
    private val pin = MutableStateFlow<Pin?>(null)
    private val location = MutableStateFlow<Location?>(null)
    private var started = false

    // Recompute the compass (and its declination) only when we move to a new
    // ~11 km cell, so the sensor isn't re-registered every second.
    private val heading = location
        .filterNotNull()
        .distinctUntilChangedBy { (it.latitude * 10).roundToInt() to (it.longitude * 10).roundToInt() }
        .flatMapLatest { compassRepository.headings(it) }

    val uiState: StateFlow<NavigateUiState> =
        combine(pin, location, heading) { p, loc, head ->
            if (p == null || loc == null) {
                NavigateUiState(pin = p)
            } else {
                val bearing = GeoMath.initialBearingDegrees(
                    loc.latitude, loc.longitude, p.latitude, p.longitude,
                )
                val distance = GeoMath.distanceMeters(
                    loc.latitude, loc.longitude, p.latitude, p.longitude,
                )
                val arrivedRadius = max(p.accuracyMeters.toDouble(), 5.0)
                NavigateUiState(
                    pin = p,
                    arrowAngle = GeoMath.arrowAngleDegrees(bearing, head.trueNorthDegrees.toDouble()).toFloat(),
                    distanceMeters = distance,
                    currentAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                    compassAccuracyClass = head.accuracyClass,
                    arrived = distance <= arrivedRadius,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigateUiState())

    init {
        viewModelScope.launch { pin.value = pinRepository.get(pinId) }
    }

    /** Begin location updates. Call once permission is granted. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            locationRepository.locationUpdates()
                .catch { /* handled by the waiting UI */ }
                .collect { location.value = it }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            pinRepository.delete(pinId)
            onDeleted()
        }
    }

    fun addPhoto(path: String) {
        val p = pin.value ?: return
        val updated = p.copy(photoPaths = p.photoPaths + path)
        pin.value = updated
        viewModelScope.launch { pinRepository.upsert(updated) }
    }

    fun removePhoto(path: String) {
        val p = pin.value ?: return
        val updated = p.copy(photoPaths = p.photoPaths - path)
        pin.value = updated
        viewModelScope.launch {
            pinRepository.upsert(updated)
            PhotoStore.delete(path)
        }
    }
}
