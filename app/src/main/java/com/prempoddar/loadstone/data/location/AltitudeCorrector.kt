package com.prempoddar.loadstone.data.location

import android.content.Context
import android.location.Location
import androidx.core.location.LocationCompat
import androidx.core.location.altitude.AltitudeConverterCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts raw GPS altitude into height above mean sea level.
 *
 * Raw `Location.altitude` is height above the **WGS84 ellipsoid**, which in many regions sits tens
 * of meters away from actual sea level (in India the geoid is ~40–100 m below it), so unconverted
 * readings look wrong — often negative on dry land. [AltitudeConverterCompat] applies the geoid
 * correction.
 *
 * The correction is a slowly varying function of position (centimeters per kilometer), so we learn
 * the local offset once off the main thread and then apply it cheaply to every later fix. Until the
 * offset is known we report `null` rather than a misleading ellipsoidal number.
 */
@Singleton
class AltitudeCorrector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** mslAltitude - ellipsoidAltitude for the current area, once learned. */
    private val offset = AtomicReference<Double?>(null)
    private val learning = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Height above mean sea level in meters, or null while the correction is unknown. */
    fun mslAltitudeMeters(location: Location): Double? {
        if (!location.hasAltitude()) return null
        if (LocationCompat.hasMslAltitude(location)) {
            return LocationCompat.getMslAltitudeMeters(location)
        }
        offset.get()?.let { return location.altitude + it }
        learnOffset(location)
        return null
    }

    private fun learnOffset(location: Location) {
        if (!learning.compareAndSet(false, true)) return
        val sample = Location(location)
        scope.launch {
            runCatching {
                AltitudeConverterCompat.addMslAltitudeToLocation(context, sample)
                if (LocationCompat.hasMslAltitude(sample)) {
                    offset.set(LocationCompat.getMslAltitudeMeters(sample) - sample.altitude)
                }
            }
            learning.set(false)
        }
    }
}
