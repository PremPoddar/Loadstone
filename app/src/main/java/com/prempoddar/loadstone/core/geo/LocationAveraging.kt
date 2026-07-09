package com.prempoddar.loadstone.core.geo

import kotlin.math.sqrt

/**
 * Inverse-variance weighted averaging of GPS fixes over a rolling time window.
 *
 * Design notes (why it behaves the way it does):
 * - **No catastrophic reset.** Individual fixes jitter a lot while accuracy is poor. We only treat
 *   a fix as "moved away" after several consecutive far readings (real walking), and even then we
 *   rebuild rather than wipe on a single jump. A lone outlier is skipped, not destructive.
 * - **The far threshold scales with the current accuracy**, so 30–80 m noise at ±50 m is accepted
 *   and averaged, while a genuine walk is still detected.
 * - **Honest accuracy.** GPS errors are correlated (multipath, geometry), so averaging can't beat
 *   the hardware by √N. The estimate reflects the best recent fix and how much the samples actually
 *   agree (their scatter), floored at a realistic consumer-GPS limit. It tightens as the *hardware*
 *   improves and the readings converge — not merely because more samples arrived.
 *
 * Not thread-safe; drive it from a single collector.
 */
class LocationAveraging(
    private val windowMs: Long = 45_000,
    private val maxSamples: Int = 120,
) {
    private val samples = ArrayDeque<RawFix>()
    private var consecutiveFar = 0

    fun reset() {
        samples.clear()
        consecutiveFar = 0
    }

    /** Ingest a fix. Only physically impossible fixes (non-positive accuracy) are dropped. */
    fun add(fix: RawFix) {
        if (fix.accuracyMeters <= 0f) return
        val cur = current()
        if (cur != null) {
            val d = GeoMath.distanceMeters(cur.latitude, cur.longitude, fix.latitude, fix.longitude)
            val threshold = maxOf(cur.estAccuracyMeters * 3.0, fix.accuracyMeters * 3.0, 25.0)
            if (d > threshold) {
                consecutiveFar++
                if (consecutiveFar < MOVE_CONFIRM) return // lone outlier — skip, keep averaging
                samples.clear() // sustained move: the user really walked away, so rebuild
                consecutiveFar = 0
            } else {
                consecutiveFar = 0
            }
        }
        samples.addLast(fix)
        evictOld(fix.timestampMs)
        while (samples.size > maxSamples) samples.removeFirst()
    }

    private fun evictOld(nowMs: Long) {
        while (samples.isNotEmpty() && nowMs - samples.first().timestampMs > windowMs) {
            samples.removeFirst()
        }
    }

    /** Current converged estimate, or null if no samples have been accepted. */
    fun current(): PinFix? {
        if (samples.isEmpty()) return null
        var wSum = 0.0
        var latSum = 0.0
        var lonSum = 0.0
        var altWSum = 0.0
        var altSum = 0.0
        for (s in samples) {
            val w = 1.0 / (s.accuracyMeters.toDouble() * s.accuracyMeters) // inverse variance
            wSum += w
            latSum += w * s.latitude
            lonSum += w * s.longitude
            s.altitudeMeters?.let { altSum += w * it; altWSum += w }
        }
        val lat = latSum / wSum
        val lon = lonSum / wSum

        // Weighted RMS scatter of the samples around the centroid: how much they actually agree.
        var spreadNum = 0.0
        for (s in samples) {
            val w = 1.0 / (s.accuracyMeters.toDouble() * s.accuracyMeters)
            val d = GeoMath.distanceMeters(lat, lon, s.latitude, s.longitude)
            spreadNum += w * d * d
        }
        val spread = sqrt(spreadNum / wSum).toFloat()
        val best = samples.minOf { it.accuracyMeters }

        // Can't honestly claim tighter than ~half the best single fix (systematic floor), than the
        // observed scatter, or than a realistic consumer-GPS limit of ~3 m.
        val est = maxOf(3f, best * 0.5f, spread)
        val altitude = if (altWSum > 0) altSum / altWSum else null

        return PinFix(lat, lon, est, samples.size, altitude)
    }

    companion object {
        /** Consecutive far readings required before we accept that the user has moved. */
        const val MOVE_CONFIRM = 4
    }
}
