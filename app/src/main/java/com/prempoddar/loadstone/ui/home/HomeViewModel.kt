package com.prempoddar.loadstone.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.core.geo.GeoMath
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

data class PinListItem(val pin: Pin, val distanceMeters: Double?)
data class HomeUiState(val items: List<PinListItem> = emptyList(), val hasLocation: Boolean = false)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pinRepository: PinRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val location = MutableStateFlow<Location?>(null)
    private var locationStarted = false

    val uiState: StateFlow<HomeUiState> =
        combine(pinRepository.observeAll(), location) { pins, loc ->
            val items = pins
                .map { pin ->
                    val d = loc?.let {
                        GeoMath.distanceMeters(it.latitude, it.longitude, pin.latitude, pin.longitude)
                    }
                    PinListItem(pin, d)
                }
                .sortedWith(compareBy(nullsLast()) { it.distanceMeters })
            HomeUiState(items = items, hasLocation = loc != null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /** Called once the user has granted location permission. */
    fun startLocationUpdates() {
        if (locationStarted) return
        locationStarted = true
        viewModelScope.launch {
            locationRepository.locationUpdates()
                .catch { /* location unavailable; list still works without distances */ }
                .collect { location.value = it }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { pinRepository.delete(id) }
    }
}
