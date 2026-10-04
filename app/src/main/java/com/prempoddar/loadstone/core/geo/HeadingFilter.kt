package com.prempoddar.loadstone.core.geo

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Smooths a stream of compass headings for display.
 *
 * A single fixed-strength low-pass filter cannot win here, and that is what made the arrow feel
 * jittery. Set it weak and sensor noise of a degree or two shows up as a permanent shiver; set it
 * strong and the arrow lags visibly behind you when you turn, which reads as the app being slow
 * rather than steady. The two are not the same problem and need different answers:
 *
 * - **A dead band for noise.** Changes under a degree are, at rest, overwhelmingly sensor noise
 *   rather than you moving. Inside the band the filter still tracks, but so gently that the arrow
 *   simply stops twitching.
 * - **A gain that rises with how fast you are turning.** A real turn moves the heading by tens of
 *   degrees, far outside anything noise produces, so the filter opens up and keeps pace, then
 *   closes again as you settle. The result is an arrow that is still when you are still and
 *   immediate when you turn.
 *
 * Smoothing runs on the heading's unit vector, so the 359°→0° wrap is not a discontinuity.
 */
class HeadingFilter(
    private val restAlpha: Double = REST_ALPHA,
    private val turnAlpha: Double = TURN_ALPHA,
) {
    private var sinSmoothed = 0.0
    private var cosSmoothed = 0.0
    private var started = false

    fun reset() {
        started = false
    }

    /** Feed one raw heading in degrees and get the smoothed heading, 0–360. */
    fun update(degrees: Double): Double {
        val rad = Math.toRadians(degrees)
        val s = sin(rad)
        val c = cos(rad)
        if (!started) {
            sinSmoothed = s
            cosSmoothed = c
            started = true
            return GeoMath.normalize360(degrees)
        }
        val alpha = alphaFor(angularDistance(current(), degrees))
        sinSmoothed += alpha * (s - sinSmoothed)
        cosSmoothed += alpha * (c - cosSmoothed)
        return current()
    }

    private fun current(): Double =
        GeoMath.normalize360(Math.toDegrees(atan2(sinSmoothed, cosSmoothed)))

    /**
     * Gain for a step of [delta] degrees: the dead band's crawl below [NOISE_BAND_DEG], rising
     * smoothly to the responsive gain by [TURN_BAND_DEG] so there is no visible gear change.
     */
    private fun alphaFor(delta: Double): Double = when {
        delta <= NOISE_BAND_DEG -> DEAD_BAND_ALPHA
        delta <= DRIFT_BAND_DEG -> lerp(DEAD_BAND_ALPHA, restAlpha, delta, NOISE_BAND_DEG, DRIFT_BAND_DEG)
        delta <= TURN_BAND_DEG -> lerp(restAlpha, turnAlpha, delta, DRIFT_BAND_DEG, TURN_BAND_DEG)
        else -> turnAlpha
    }

    private fun lerp(from: Double, to: Double, x: Double, lo: Double, hi: Double): Double =
        from + (to - from) * ((x - lo) / (hi - lo))

    companion object {
        /** Shortest angle in degrees between two headings, 0–180. */
        fun angularDistance(a: Double, b: Double): Double {
            val d = abs(GeoMath.normalize360(a - b))
            return if (d > 180) 360 - d else d
        }

        /** Below this a change is treated as sensor noise. */
        const val NOISE_BAND_DEG = 1.2

        /** Above the dead band but still small enough to be drift rather than a deliberate turn. */
        const val DRIFT_BAND_DEG = 5.0

        /** At and above this the user is definitely turning. */
        const val TURN_BAND_DEG = 18.0

        const val DEAD_BAND_ALPHA = 0.03
        const val REST_ALPHA = 0.10
        const val TURN_ALPHA = 0.55
    }
}
