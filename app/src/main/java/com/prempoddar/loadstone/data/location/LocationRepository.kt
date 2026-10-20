package com.prempoddar.loadstone.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.core.geo.PositionEstimator
import com.prempoddar.loadstone.core.geo.RawFix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams location from the fused provider. [estimatedFixes] runs the accuracy engine live so the
 * drop-pin UI can watch the estimate tighten. Callers must hold ACCESS_FINE_LOCATION before
 * collecting.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val fused: FusedLocationProviderClient,
    private val altitudeCorrector: AltitudeCorrector,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {
    /** Satellites the receiver is using for its fix, and how many it can see. */
    data class Satellites(val used: Int, val visible: Int)

    @SuppressLint("MissingPermission")
    fun satellites(): Flow<Satellites> = callbackFlow {
        val manager = context.getSystemService(android.location.LocationManager::class.java)
        val callback = object : android.location.GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: android.location.GnssStatus) {
                var used = 0
                for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) used++
                trySend(Satellites(used, status.satelliteCount))
            }
        }
        val ok = runCatching { manager.registerGnssStatusCallback(callback, android.os.Handler(android.os.Looper.getMainLooper())) }.getOrDefault(false)
        if (!ok) close()
        awaitClose { manager.unregisterGnssStatusCallback(callback) }
    }

    @SuppressLint("MissingPermission")
    fun estimatedFixes(): Flow<PinFix> = callbackFlow {
        val estimator = PositionEstimator()

        fun ingest(loc: Location) {
            if (!loc.hasAccuracy()) return
            estimator.add(loc.toRawFix(altitudeCorrector.mslAltitudeMeters(loc)))
            estimator.current()?.let { trySend(it) }
        }

        // Warm start — but only from a genuinely recent fix, never a days-old cached one, which
        // would anchor the estimate wherever the phone was last switched on.
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && isFresh(loc, maxAgeMs = 30_000)) ingest(loc)
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { if (isFresh(it)) ingest(it) }
            }
        }
        fused.requestLocationUpdates(preciseRequest(), callback, null)
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Forward only fresh AND reasonably precise fixes. A stale cached location made
                // navigation think you'd arrived when you hadn't; a coarse network fix made it
                // report a confident but wrong distance.
                result.lastLocation?.let { if (isFresh(it) && isPrecise(it)) trySend(it) }
            }
        }
        fused.requestLocationUpdates(preciseRequest(), callback, null)
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    companion object {
        /**
         * One update per second from the satellites themselves. [Granularity.GRANULARITY_FINE] is
         * explicit rather than inherited from the permission, so a build that also asks for coarse
         * location can never silently downgrade the stream to fuzzed, block-level positions.
         */
        private fun preciseRequest(): LocationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setWaitForAccurateLocation(false)
                .build()

        /** True if the fix was produced within [maxAgeMs] (default 10 s), not a cached one. */
        fun isFresh(loc: Location, maxAgeMs: Long = 10_000): Boolean {
            val elapsed = loc.elapsedRealtimeNanos
            if (elapsed <= 0L) return true // provider didn't stamp it — don't wrongly drop it
            val ageNanos = SystemClock.elapsedRealtimeNanos() - elapsed
            return ageNanos in 0..(maxAgeMs * 1_000_000)
        }

        /**
         * True if the fix is precise enough to be worth acting on. Fixes without an accuracy
         * figure, or coarser than [PositionEstimator.MAX_USABLE_ACCURACY], are network-derived
         * guesses that would produce confidently wrong distances.
         */
        fun isPrecise(loc: Location): Boolean =
            loc.hasAccuracy() && loc.accuracy > 0f &&
                loc.accuracy <= PositionEstimator.MAX_USABLE_ACCURACY
    }
}

/**
 * Convert a platform fix into the estimator's input.
 *
 * The timestamp is the provider's monotonic one, never [Location.getTime]: wall-clock time jumps
 * whenever the network corrects the clock, and the filter reads the gap between fixes as "how far
 * could the user have walked", so a jump would either freeze it or blow its uncertainty wide open.
 */
private fun Location.toRawFix(mslAltitude: Double?) = RawFix(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracy,
    elapsedRealtimeMs = if (elapsedRealtimeNanos > 0L) {
        elapsedRealtimeNanos / 1_000_000
    } else {
        SystemClock.elapsedRealtime()
    },
    altitudeMeters = mslAltitude,
    altitudeAccuracyMeters = if (hasVerticalAccuracy()) verticalAccuracyMeters else null,
    speedMps = if (hasSpeed()) speed else null,
)
