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
    private val maxAccuracyMeters: Float = MAX_USABLE_ACCURACY,
) {
    private val samples = ArrayDeque<RawFix>()
    private var consecutiveFar = 0

    fun reset() {
        samples.clear()
        consecutiveFar = 0
    }

    /**
     * Ingest a fix. Readings too coarse to ever locate a spot precisely (Wi-Fi and cell-tower
     * derived fixes, typically hundreds of meters) are dropped outright: their inverse-variance
     * weight is small but non-zero, and letting one in as the *first* sample would also seed the
     * movement guard from the wrong place.
     */
    fun add(fix: RawFix) {
        if (fix.accuracyMeters <= 0f || fix.accuracyMeters > maxAccuracyMeters) return
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
        trimToSettled()
    }

    /**
     * Drop the approach.
     *
     * Walking up to a spot with the screen open used to blend the whole path into the average: the
     * movement guard never fires because each step is small next to the running mean, so the pin
     * landed tens of meters back along the way you came. Standing still produces *scatter* — the
     * older and newer halves of the window sit on top of each other — whereas walking produces
     * *drift*, with the halves clearly separated. While that separation is larger than the noise,
     * the older half belongs to the approach, so discard it.
     */
    private fun trimToSettled() {
        while (samples.size >= MIN_SPLIT_SAMPLES) {
            val half = samples.size / 2
            val older = centroidOf(samples.take(half))
            val newer = centroidOf(samples.drop(half))
            val drift = GeoMath.distanceMeters(older.first, older.second, newer.first, newer.second)
            val noise = maxOf(samples.drop(half).minOf { it.accuracyMeters }.toDouble(), 4.0)
            if (drift <= noise) return
            repeat(half) { samples.removeFirst() }
        }
    }

    private fun centroidOf(list: List<RawFix>): Pair<Double, Double> {
        var wSum = 0.0
        var lat = 0.0
        var lon = 0.0
        for (s in list) {
            val w = 1.0 / (s.accuracyMeters.toDouble() * s.accuracyMeters)
            wSum += w
            lat += w * s.latitude
            lon += w * s.longitude
        }
        return (lat / wSum) to (lon / wSum)
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

        /**
         * Worst fix that can still contribute to a pin. Real GNSS outdoors is 3–15 m; anything
         * beyond this is a network-derived guess that would only poison the average.
         */
        const val MAX_USABLE_ACCURACY = 100f

        /** Fewest samples worth splitting in half to tell drift (walking) from scatter (standing). */
        const val MIN_SPLIT_SAMPLES = 6
    }
}
