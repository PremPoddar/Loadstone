package com.prempoddar.loadstone.ui.navigate

import android.hardware.SensorManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.core.geo.PinRefinement
import com.prempoddar.loadstone.data.location.CompassRepository
import com.prempoddar.loadstone.data.location.Heading
import com.prempoddar.loadstone.data.location.LocationRepository
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinRepository
import com.prempoddar.loadstone.data.settings.SettingsRepository
import com.prempoddar.loadstone.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.prempoddar.loadstone.data.photo.PhotoStore
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class NavigateUiState(
    val pin: Pin? = null,
    /**
     * Where to draw the arrow relative to the top of the screen (0 = dead ahead), or null while
     * the direction is unknown — no usable fix, no compass reading yet, or too close for the
     * direction to mean anything.
     */
    val arrowAngle: Float? = null,
    val distanceMeters: Double? = null,
    val currentAccuracyMeters: Float? = null,
    /** Pin uncertainty and live-fix uncertainty together: the radius you may have to search. */
    val uncertaintyMeters: Double? = null,
    /** The compass reports it is uncalibrated, so the arrow can be well off until a figure 8. */
    val compassNeedsCalibration: Boolean = false,
    /** False on phones without a compass: distance only, no arrow. */
    val compassAvailable: Boolean = true,
    val arrived: Boolean = false,
    /** True while the only fixes available are too coarse to give a real distance or bearing. */
    val waitingForFix: Boolean = true,
    /** True when you are inside the uncertainty: there is no direction left to give, so look around. */
    val searchHere: Boolean = false,
    /** True when this visit is a better measurement than the pin holds, so it is worth saving. */
    val canRefine: Boolean = false,
    val refined: Boolean = false,
)

/** The parts of a position estimate that navigation needs, kept free of android.location for tests. */
data class NavFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val settled: Boolean = false,
)

/**
 * @param trueHeading the direction the user faces relative to true north, or null until the
 *   compass has reported.
 * @param compassAccuracyClass the compass's SensorManager.SENSOR_STATUS_* value, or null before
 *   its first reading.
 */
internal fun navigateState(
    pin: Pin?,
    fix: NavFix?,
    trueHeading: Double?,
    compassAccuracyClass: Int?,
    compassAvailable: Boolean,
    refined: Boolean = false,
): NavigateUiState {
    val currentAccuracy = fix?.accuracyMeters
    // A Wi-Fi or cell fix can be hundreds of meters out. Showing a distance derived from one reads
    // as authoritative and is simply wrong — it is why standing on the spot could report "75 m
    // away". Withhold distance and bearing until the fix is good enough to mean something.
    val usable = currentAccuracy != null && currentAccuracy <= NavigateViewModel.NAV_MAX_ACCURACY
    val base = NavigateUiState(
        pin = pin,
        currentAccuracyMeters = currentAccuracy,
        compassNeedsCalibration = compassAccuracyClass != null &&
            compassAccuracyClass <= SensorManager.SENSOR_STATUS_ACCURACY_LOW,
        compassAvailable = compassAvailable,
        waitingForFix = !usable,
        refined = refined,
    )
    if (pin == null || fix == null || currentAccuracy == null || !usable) return base

    val bearing = GeoMath.initialBearingDegrees(fix.latitude, fix.longitude, pin.latitude, pin.longitude)
    val distance = GeoMath.distanceMeters(fix.latitude, fix.longitude, pin.latitude, pin.longitude)

    // Errors in the pin and in where you are standing now are independent, so they combine in
    // quadrature rather than adding. This single number is what the whole screen should be honest
    // about: it is the radius you may still have to search once the distance reads zero.
    val uncertainty = sqrt(
        pin.accuracyMeters.toDouble() * pin.accuracyMeters + currentAccuracy.toDouble() * currentAccuracy,
    )

    // Within the uncertainty the bearing is noise. Pointing anyway is what made the arrow spin as
    // you got close: a 5 m error seen from 4 m away swings the direction through tens of degrees
    // between fixes. Better to say "it's around here" than to point confidently at nothing.
    val directionMeaningful = distance > max(uncertainty, NavigateViewModel.MIN_BEARING_DISTANCE)

    val trustworthy = currentAccuracy <= NavigateViewModel.ARRIVE_MAX_ACCURACY
    val arrived = trustworthy && distance <= max(uncertainty, NavigateViewModel.MIN_ARRIVE_RADIUS)

    val observation = PinRefinement.Observation(fix.latitude, fix.longitude, currentAccuracy.toDouble())
    val stored = PinRefinement.Observation(pin.latitude, pin.longitude, pin.accuracyMeters.toDouble())

    return base.copy(
        arrowAngle = if (directionMeaningful) {
            trueHeading?.let { GeoMath.arrowAngleDegrees(bearing, it).toFloat() }
        } else {
            null
        },
        distanceMeters = distance,
        uncertaintyMeters = uncertainty,
        arrived = arrived,
        searchHere = !directionMeaningful,
        canRefine = !refined && fix.settled && PinRefinement.worthMerging(stored, observation),
    )
}

@HiltViewModel
class NavigateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pinRepository: PinRepository,
    private val locationRepository: LocationRepository,
    compassRepository: CompassRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val arcHintVisible: StateFlow<Boolean> = settingsRepository.settings
        .map { !it.arcHintDismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val settings = settingsRepository

    private val pinId: Long = savedStateHandle[Routes.NAVIGATE_ARG_PIN_ID] ?: -1L
    private val pin = MutableStateFlow<Pin?>(null)

    // Navigation runs on the same filtered estimate the drop-pin screen uses, not on raw fixes.
    // Raw fixes hop several meters between seconds, and at close range that hop is most of the
    // bearing — which is what the jittering arrow actually was.
    private val fix = MutableStateFlow<PinFix?>(null)
    private val refined = MutableStateFlow(false)
    private var started = false

    private val compassAvailable = compassRepository.hasCompass()

    // The compass runs from the moment the screen opens. It used to start only after the first
    // precise GPS fix, and everything waited on it, so until then (and for good on phones without
    // the sensor) the screen showed no pin, no distance and a frozen arrow.
    private val heading: Flow<Heading?> = flow {
        emit(null)
        emitAll(compassRepository.headings())
    }

    // Declination drifts over hundreds of km, so recompute it only on entering a new ~11 km cell.
    private var declinationCell: Pair<Int, Int>? = null
    private var declination = 0f

    private fun declinationAt(fix: PinFix): Float {
        val cell = (fix.latitude * 10).roundToInt() to (fix.longitude * 10).roundToInt()
        if (cell != declinationCell) {
            declinationCell = cell
            declination = CompassRepository.declinationDegrees(
                fix.latitude,
                fix.longitude,
                fix.altitudeMeters ?: 0.0,
            )
        }
        return declination
    }

    // Learns the compass's local error from the direction you walk; see CourseCorrection.
    private val correction = com.prempoddar.loadstone.core.geo.CourseCorrection()
    private var lastCorrected: PinFix? = null

    val uiState: StateFlow<NavigateUiState> =
        combine(pin, fix, heading, refined) { p, f, head, didRefine ->
            val compassTrue = if (head != null && f != null) {
                GeoMath.normalize360(head.magneticDegrees + declinationAt(f).toDouble())
            } else {
                null
            }
            if (f != null && f !== lastCorrected) {
                lastCorrected = f
                correction.update(f, compassTrue, android.os.SystemClock.elapsedRealtime())
            }
            navigateState(
                pin = p,
                fix = f?.let { NavFix(it.latitude, it.longitude, it.estAccuracyMeters, it.settled) },
                trueHeading = compassTrue?.let(correction::apply),
                compassAccuracyClass = head?.accuracyClass,
                compassAvailable = compassAvailable,
                refined = didRefine,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigateUiState())

    init {
        viewModelScope.launch { pin.value = pinRepository.get(pinId) }
    }

    /** Begin location updates. Call once permission is granted. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            locationRepository.estimatedFixes()
                .catch { /* handled by the waiting UI */ }
                .collect { fix.value = it }
        }
    }

    /**
     * Fold this visit into the pin, so the spot the app remembers improves every time you stand on
     * it with a good fix. This is what finally corrects pins saved badly in the past: their stored
     * error is permanent until a second, independent measurement is averaged with it.
     */
    fun refineFromHere() {
        val current = pin.value ?: return
        val here = fix.value ?: return
        val stored = PinRefinement.Observation(current.latitude, current.longitude, current.accuracyMeters.toDouble())
        val observed = PinRefinement.Observation(here.latitude, here.longitude, here.estAccuracyMeters.toDouble())
        if (!PinRefinement.worthMerging(stored, observed)) return
        val merged = PinRefinement.merge(stored, observed)
        val updated = current.copy(
            latitude = merged.latitude,
            longitude = merged.longitude,
            accuracyMeters = merged.sigmaMeters.toFloat(),
            elevationMeters = here.altitudeMeters ?: current.elevationMeters,
        )
        pin.value = updated
        refined.value = true
        viewModelScope.launch { pinRepository.upsert(updated) }
    }

    fun dismissArcHint() {
        viewModelScope.launch { settings.setArcHintDismissed(true) }
    }

    fun addPhotos(paths: List<String>) {
        val current = pin.value ?: return
        val updated = current.copy(photoPaths = current.photoPaths + paths)
        pin.value = updated
        viewModelScope.launch { pinRepository.upsert(updated) }
    }

    fun removePhoto(path: String) {
        val current = pin.value ?: return
        val updated = current.copy(photoPaths = current.photoPaths - path)
        pin.value = updated
        viewModelScope.launch { pinRepository.upsert(updated) }
        PhotoStore.delete(path)
    }

    companion object {
        /** Above this current-fix uncertainty we won't claim "arrived". */
        const val ARRIVE_MAX_ACCURACY = 20f

        /** Above this, a fix is too coarse to derive a meaningful distance or bearing from. */
        const val NAV_MAX_ACCURACY = 50f

        /** However tight the fixes, a direction this close to the spot is guesswork. */
        const val MIN_BEARING_DISTANCE = 5.0

        /** However tight the fixes, standing this close counts as arrived. */
        const val MIN_ARRIVE_RADIUS = 6.0
    }
}
