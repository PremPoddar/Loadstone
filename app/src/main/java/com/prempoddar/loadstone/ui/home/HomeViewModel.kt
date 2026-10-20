package com.prempoddar.loadstone.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.data.location.LocationRepository
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
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
                // Pinned first, then nearest. A pin you pinned is one you expect to find at the
                // top whether you are standing next to it or in another country.
                .sortedWith(
                    compareByDescending<PinListItem> { it.pin.isPinned }
                        .thenBy(nullsLast()) { it.distanceMeters },
                )
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

    /**
     * Save a pin someone sent as a sentence.
     *
     * The code itself rounds to a couple of metres, but it says nothing about how good the
     * sender's own fix was, and a pin is only as good as the measurement behind it. Claiming
     * better than a typical saved pin would make the arrow point with a confidence nobody has
     * earned, so a shared pin starts at [SHARED_ACCURACY_METERS] and tightens the first time
     * someone stands on the spot.
     */
    fun addFromWords(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch {
            pinRepository.upsert(
                Pin(
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = SHARED_ACCURACY_METERS,
                    category = PinCategory.PIN,
                ),
            )
        }
    }

    fun setPinned(id: Long, pinned: Boolean) {
        viewModelScope.launch { pinRepository.setPinned(id, pinned) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            // Its photos and voice notes go with it, rather than staying behind unseen.
            pinRepository.get(id)?.let { pin ->
                pin.photoPaths.forEach(com.prempoddar.loadstone.data.photo.PhotoStore::delete)
                pin.voiceNotes.forEach(com.prempoddar.loadstone.data.voice.VoiceRecorder::delete)
            }
            pinRepository.delete(id)
        }
    }

    fun updatePin(
        original: Pin,
        name: String,
        note: String?,
        category: PinCategory,
        photoPaths: List<String>,
    ) {
        viewModelScope.launch {
            pinRepository.upsert(
                original.copy(
                    name = name.trim().ifBlank { original.name },
                    note = note?.trim()?.ifBlank { null },
                    category = category,
                    photoPaths = photoPaths,
                ),
            )
        }
    }

    companion object {
        /** What a pin shared as words is worth before anyone has stood on it. */
        const val SHARED_ACCURACY_METERS = 10f
    }
}
