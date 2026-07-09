package com.prempoddar.loadstone.data.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.prempoddar.loadstone.core.geo.LocationAveraging
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.core.geo.RawFix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams location from the fused provider. [averagedFixes] runs the accuracy
 * engine live so the drop-pin UI can watch the estimate tighten. Callers must
 * hold ACCESS_FINE_LOCATION before collecting.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val fused: FusedLocationProviderClient,
) {
    @SuppressLint("MissingPermission")
    fun averagedFixes(): Flow<PinFix> = callbackFlow {
        val averaging = LocationAveraging()

        fun ingest(loc: Location) {
            if (!loc.hasAccuracy()) return
            averaging.add(
                RawFix(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracyMeters = loc.accuracy,
                    timestampMs = loc.time,
                    altitudeMeters = if (loc.hasAltitude()) loc.altitude else null,
                ),
            )
            averaging.current()?.let { trySend(it) }
        }

        // Warm start: show the last-known position immediately while GPS locks on.
        fused.lastLocation.addOnSuccessListener { loc -> loc?.let(::ingest) }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(::ingest)
            }
        }
        fused.requestLocationUpdates(request, callback, null)
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        fused.requestLocationUpdates(request, callback, null)
        awaitClose { fused.removeLocationUpdates(callback) }
    }
}
