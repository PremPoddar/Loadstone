package com.prempoddar.loadstone.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
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
    private val altitudeCorrector: AltitudeCorrector,
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
                    altitudeMeters = altitudeCorrector.mslAltitudeMeters(loc),
                ),
            )
            averaging.current()?.let { trySend(it) }
        }

        // Warm start — but only from a genuinely recent fix, never a days-old cached
        // one (that used to poison the average and cause false readings).
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && isFresh(loc, maxAgeMs = 30_000)) ingest(loc)
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { if (isFresh(it)) ingest(it) }
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
                // Forward only fresh AND reasonably precise fixes. A stale cached location made
                // navigation think you'd arrived when you hadn't; a coarse network fix made it
                // report a confident but wrong distance.
                result.lastLocation?.let { if (isFresh(it) && isPrecise(it)) trySend(it) }
            }
        }
        fused.requestLocationUpdates(request, callback, null)
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    companion object {
        /** True if the fix was produced within [maxAgeMs] (default 10 s), not a cached one. */
        fun isFresh(loc: Location, maxAgeMs: Long = 10_000): Boolean {
            val elapsed = loc.elapsedRealtimeNanos
            if (elapsed <= 0L) return true // provider didn't stamp it — don't wrongly drop it
            val ageNanos = SystemClock.elapsedRealtimeNanos() - elapsed
            return ageNanos in 0..(maxAgeMs * 1_000_000)
        }

        /**
         * True if the fix is precise enough to be worth acting on. Fixes without an accuracy
         * figure, or coarser than [LocationAveraging.MAX_USABLE_ACCURACY], are network-derived
         * guesses that would produce confidently wrong distances.
         */
        fun isPrecise(loc: Location): Boolean =
            loc.hasAccuracy() && loc.accuracy > 0f &&
                loc.accuracy <= LocationAveraging.MAX_USABLE_ACCURACY
    }
}
