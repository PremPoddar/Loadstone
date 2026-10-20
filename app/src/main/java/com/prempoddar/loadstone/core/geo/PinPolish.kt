package com.prempoddar.loadstone.core.geo

import kotlin.math.max

/**
 * Lets a pin keep sharpening for a little while after it is saved.
 *
 * Waiting on the drop screen until the estimate is perfect makes saving feel slow, and people
 * tap Save as soon as they can. But they usually stay put for a few more seconds (naming the pin,
 * putting the phone away), and the estimator keeps converging during that time. Those extra fixes
 * are worth having, as long as they are of the same spot: the moment the person walks off, later
 * estimates describe where they are going, not where the pin is.
 */
object PinPolish {
    sealed interface Step {
        /** The new estimate is a better reading of the same spot: store it. */
        data class Improve(val fix: PinFix) : Step
        data object Keep : Step
        /** The person has left the spot; nothing later belongs to the pin. */
        data object Stop : Step
    }

    /** Readings this much tighter than the stored one are worth a write. */
    const val MIN_GAIN_METERS = 0.3f

    /** A settled estimate this far from the stored one is worth a write even at the same accuracy. */
    const val MIN_SHIFT_METERS = 0.5

    /** However tight the saved fix, moving further than this means leaving. */
    const val MIN_LEAVE_METERS = 8.0

    /** [saved] is where the pin was saved, [best] what it holds now, [next] the newest estimate. */
    fun step(saved: PinFix, best: PinFix, next: PinFix): Step {
        val away = GeoMath.distanceMeters(saved.latitude, saved.longitude, next.latitude, next.longitude)
        if (next.moving || away > max(MIN_LEAVE_METERS, 2.0 * saved.estAccuracyMeters)) return Step.Stop
        // Tighter is better. So is a settled reading of the same quality that has moved: the
        // estimate was still catching up from the walk in when Save was tapped.
        val tighter = next.estAccuracyMeters < best.estAccuracyMeters - MIN_GAIN_METERS
        val caughtUp = next.settled && next.estAccuracyMeters <= best.estAccuracyMeters + MIN_GAIN_METERS &&
            GeoMath.distanceMeters(best.latitude, best.longitude, next.latitude, next.longitude) > MIN_SHIFT_METERS
        return if (tighter || caughtUp) Step.Improve(next) else Step.Keep
    }
}
