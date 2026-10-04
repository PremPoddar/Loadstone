package com.prempoddar.loadstone.core.geo

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Turns a stream of noisy GPS fixes into one position estimate, with an uncertainty you can trust.
 *
 * This is a recursive Bayesian (Kalman) filter on a two-dimensional position held in a local
 * east/north frame, with a one-dimensional sibling for altitude. The state is a mean and an
 * isotropic variance; every fix is a measurement whose variance is the provider's reported
 * accuracy squared; between fixes the variance grows by however far the user could have walked.
 *
 * It replaces a window-of-samples average, and the difference matters:
 *
 * - **Walking away is handled by the model, not by heuristics.** Averaging has to guess which
 *   samples belong to "here" — hence thresholds, outlier counters, and a rule that threw away the
 *   older half of the window when it drifted. All of those are approximations of one fact: old
 *   information is worth less once you have moved. The filter states that directly as process
 *   noise, so an approach on foot decays out of the estimate smoothly and a stand-still does not.
 *
 * - **The uncertainty is a real number, not a proxy.** A window average can only report the scatter
 *   it happens to see, which is why standing still under a roof — every fix wrong by the same 30 m
 *   — used to look like a tight estimate. Here the variance is propagated explicitly, and floored
 *   by the part of GPS error that repeated measurements cannot remove at all (see [biasFloor]).
 *
 * - **It is stable over time.** The estimate depends only on the fixes seen and the gaps between
 *   them, never on how many samples happen to sit inside an arbitrary window, so the same walk
 *   produces the same answer whether the screen was open for ten seconds or two minutes.
 *
 * Not thread-safe; drive it from a single collector.
 */
class PositionEstimator(
    private val maxAccuracyMeters: Float = MAX_USABLE_ACCURACY,
) {
    private var frame: LocalFrame? = null

    // Horizontal state: mean offset from the frame origin, and its variance in m².
    private var east = 0.0
    private var north = 0.0
    private var variance = 0.0

    // Vertical state, filtered separately: altitude error is largely independent of horizontal.
    private var altitude: Double? = null
    private var altitudeVariance = 0.0

    private var lastTimeMs = 0L
    private var bestAccuracy = Float.MAX_VALUE
    private var accepted = 0
    private var consecutiveRejects = 0
    private var moving = false
    private var lastReportedSigma = Double.MAX_VALUE
    private var settledSince = 0

    fun reset() {
        frame = null
        east = 0.0
        north = 0.0
        variance = 0.0
        altitude = null
        altitudeVariance = 0.0
        lastTimeMs = 0L
        bestAccuracy = Float.MAX_VALUE
        accepted = 0
        consecutiveRejects = 0
        moving = false
        lastReportedSigma = Double.MAX_VALUE
        settledSince = 0
    }

    /**
     * Fold one fix into the estimate.
     *
     * Fixes too coarse to ever pin a spot — Wi-Fi and cell-tower derived guesses of hundreds of
     * meters — are dropped rather than down-weighted. Their weight is small but not zero, and a
     * single one arriving first would anchor the frame hundreds of meters from where the user is
     * actually standing.
     */
    fun add(fix: RawFix) {
        if (!fix.latitude.isFinite() || !fix.longitude.isFinite()) return
        if (fix.accuracyMeters <= 0f || fix.accuracyMeters > maxAccuracyMeters) return

        val f = frame ?: LocalFrame(fix.latitude, fix.longitude).also {
            frame = it
            initializeFrom(fix)
            return
        }

        val zEast = f.east(fix.longitude)
        val zNorth = f.north(fix.latitude)
        val r = fix.accuracyMeters.toDouble() * fix.accuracyMeters

        predictTo(fix.elapsedRealtimeMs, fix.speedMps)

        // Gate: how surprising is this fix, measured in standard deviations of the difference we
        // expect between the prediction and a correct measurement? Beyond the gate it is a
        // multipath spike, and folding it in would drag the estimate off the spot.
        val innovation = hypot(zEast - east, zNorth - north)
        val s = variance + r
        if (accepted > 0 && innovation > GATE_SIGMAS * sqrt(s)) {
            consecutiveRejects++
            // Repeated "surprises" are not noise: the user has moved further than the motion model
            // allowed, or the provider has switched sources. Believe the fixes, not the model.
            if (consecutiveRejects >= REJECTS_BEFORE_RESET) {
                frame = LocalFrame(fix.latitude, fix.longitude)
                initializeFrom(fix)
            }
            return
        }
        consecutiveRejects = 0

        // Standard scalar Kalman update, applied to each axis with the shared isotropic gain.
        val gain = variance / s
        east += gain * (zEast - east)
        north += gain * (zNorth - north)
        variance *= (1 - gain)

        updateAltitude(fix)

        bestAccuracy = min(bestAccuracy, fix.accuracyMeters)
        accepted++
        trackSettling()
    }

    private fun initializeFrom(fix: RawFix) {
        east = 0.0
        north = 0.0
        variance = fix.accuracyMeters.toDouble() * fix.accuracyMeters
        altitude = fix.altitudeMeters
        altitudeVariance = altitudeSigma(fix).let { it * it }
        lastTimeMs = fix.elapsedRealtimeMs
        bestAccuracy = fix.accuracyMeters
        accepted = 1
        consecutiveRejects = 0
        lastReportedSigma = Double.MAX_VALUE
        settledSince = 0
    }

    /**
     * Grow the uncertainty to account for the time since the last fix.
     *
     * The user may have walked in that gap, so the estimate is less trustworthy than it was. How
     * much less depends on how fast they are going: standing still is a slow creep that lets the
     * estimate keep converging, while a confirmed walking speed inflates the variance enough that
     * the next fixes dominate and the path already travelled fades out of the answer by itself.
     */
    private fun predictTo(timeMs: Long, speedMps: Float?) {
        val dt = ((timeMs - lastTimeMs).coerceAtLeast(0L)) / 1000.0
        lastTimeMs = timeMs
        if (dt <= 0.0) return
        val speed = speedMps?.takeIf { it.isFinite() && it >= 0f }?.toDouble() ?: 0.0
        moving = speed > WALKING_MPS
        val drift = max(STILL_DRIFT_MPS, speed)
        // Deterministic displacement, not a random walk: a steady 1.2 m/s for 5 s really can put
        // you 6 m away, and squaring the whole displacement says exactly that.
        variance += (drift * dt) * (drift * dt)
        altitudeVariance += (VERTICAL_DRIFT_MPS * dt) * (VERTICAL_DRIFT_MPS * dt)
    }

    private fun updateAltitude(fix: RawFix) {
        val z = fix.altitudeMeters ?: return
        val sigma = altitudeSigma(fix)
        val r = sigma * sigma
        val current = altitude
        if (current == null) {
            altitude = z
            altitudeVariance = r
            return
        }
        val gain = altitudeVariance / (altitudeVariance + r)
        altitude = current + gain * (z - current)
        altitudeVariance *= (1 - gain)
    }

    private fun altitudeSigma(fix: RawFix): Double =
        fix.altitudeAccuracyMeters?.takeIf { it > 0f }?.toDouble()
            // GNSS height is roughly half again as uncertain as horizontal position, because the
            // satellites are all on one side of you.
            ?: (fix.accuracyMeters * 1.6)

    /** Watch for the point where more fixes stop buying accuracy, so the UI can stop asking to wait. */
    private fun trackSettling() {
        val sigma = sigmaMeters()
        settledSince = if (abs(lastReportedSigma - sigma) < SETTLE_DELTA_M) settledSince + 1 else 0
        lastReportedSigma = sigma
    }

    /**
     * Part of the error that no amount of waiting removes.
     *
     * Consecutive GPS fixes are not independent samples of the truth. Ionospheric delay, satellite
     * geometry and especially multipath off a wall or a car roof bias every fix in the same
     * direction for minutes at a time, so the filter's variance — which assumes independence —
     * eventually claims a precision the hardware does not have. Reporting that number is what makes
     * a pin "±2 m" and still 20 m wrong when you come back. We refuse to claim better than a fixed
     * fraction of the best fix we have actually seen, never below the practical limit of consumer
     * GNSS.
     */
    private fun biasFloor(): Double =
        max(ABSOLUTE_FLOOR_M, bestAccuracy.toDouble() * CORRELATED_FRACTION)

    private fun sigmaMeters(): Double = max(sqrt(variance), biasFloor())

    /** Current estimate, or null until a usable fix has arrived. */
    fun current(): PinFix? {
        val f = frame ?: return null
        if (accepted == 0) return null
        return PinFix(
            latitude = f.latitudeOf(north),
            longitude = f.longitudeOf(east),
            estAccuracyMeters = sigmaMeters().toFloat(),
            sampleCount = accepted,
            altitudeMeters = altitude,
            settled = accepted >= MIN_SAMPLES_TO_SETTLE && settledSince >= SETTLE_RUN && !moving,
            moving = moving,
        )
    }

    private fun hypot(dx: Double, dy: Double) = sqrt(dx * dx + dy * dy)

    companion object {
        /**
         * Worst fix that can still contribute. Real GNSS outdoors is 3–15 m; beyond this a fix is a
         * network-derived guess that can only poison the estimate.
         */
        const val MAX_USABLE_ACCURACY = 100f

        /** How surprising a fix may be, in standard deviations, before it is treated as a spike. */
        const val GATE_SIGMAS = 3.5

        /** Surprising fixes in a row that mean the model is wrong rather than the fixes. */
        const val REJECTS_BEFORE_RESET = 3

        /** Speed above which the user is walking rather than standing with noisy fixes. */
        const val WALKING_MPS = 0.55

        /** Creep allowed while apparently still: hand movement, a step or two, provider jitter. */
        const val STILL_DRIFT_MPS = 0.18

        /** Vertical equivalent of [STILL_DRIFT_MPS]. */
        const val VERTICAL_DRIFT_MPS = 0.25

        /** Fraction of the best observed accuracy that is shared bias, not averageable noise. */
        const val CORRELATED_FRACTION = 0.55

        /** No consumer GNSS receiver is honestly better than this. */
        const val ABSOLUTE_FLOOR_M = 3.0

        /** A change in the estimate smaller than this is not an improvement worth waiting for. */
        const val SETTLE_DELTA_M = 0.15

        /** Consecutive non-improving updates before the estimate counts as settled. */
        const val SETTLE_RUN = 4

        /** Never call it settled before this many fixes, however quickly the variance collapses. */
        const val MIN_SAMPLES_TO_SETTLE = 6
    }
}
