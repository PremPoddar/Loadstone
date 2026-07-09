package com.prempoddar.loadstone.core.geo

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Inverse-variance weighted averaging of GPS fixes with outlier rejection and a
 * movement guard. This is the accuracy core: the estimated accuracy shrinks as
 * consistent samples accumulate, which is what makes the drop-pin readout visibly
 * tighten the longer the user waits.
 *
 * Not thread-safe; drive it from a single collector.
 */
class LocationAveraging(
    private val maxAccuracyMeters: Float = 50f,
    private val movementResetMeters: Double = 15.0,
    private val maxSamples: Int = 60,
) {
    private val samples = ArrayDeque<RawFix>()

    fun reset() = samples.clear()

    /** Ingest a fix. Fixes worse than [maxAccuracyMeters] are ignored. */
    fun add(fix: RawFix) {
        if (fix.accuracyMeters <= 0f || fix.accuracyMeters > maxAccuracyMeters) return
        val cur = current()
        if (cur != null &&
            GeoMath.distanceMeters(cur.latitude, cur.longitude, fix.latitude, fix.longitude) >
            movementResetMeters
        ) {
            // Sustained move away from the converged point: the user is walking,
            // so start over rather than averaging across the walk.
            samples.clear()
        }
        samples.addLast(fix)
        while (samples.size > maxSamples) samples.removeFirst()
    }

    /** Current converged estimate, or null if no samples have been accepted. */
    fun current(): PinFix? {
        if (samples.isEmpty()) return null
        var wSum = 0.0
        var latSum = 0.0
        var lonSum = 0.0
        for (s in samples) {
            val w = 1.0 / (s.accuracyMeters.toDouble() * s.accuracyMeters) // inverse variance
            wSum += w
            latSum += w * s.latitude
            lonSum += w * s.longitude
        }
        val lat = latSum / wSum
        val lon = lonSum / wSum
        // Standard error of the weighted mean ~ 1 / sqrt(sum of weights).
        val est = (1.0 / sqrt(wSum)).toFloat()
        val best = samples.minOf { it.accuracyMeters }
        // Never claim to be tighter than a fraction of the best single fix.
        return PinFix(lat, lon, max(est, best * 0.3f), samples.size)
    }
}
