package com.prempoddar.loadstone.core.geo

import kotlin.math.abs

/**
 * Corrects the compass with the direction you are actually walking.
 *
 * A phone's compass is easily pulled off by things nearby: a car body, rebar in a pavement, a
 * metal railing. GPS knows nothing about magnets, and once you have walked a few metres it can say
 * which way you went. When the screen is held ahead of you, that direction and the compass
 * should agree; whatever they persistently disagree by is the compass's error here, so it is
 * learnt slowly and taken off every reading.
 *
 * It is careful about when to learn: only from steady walking with tight fixes, never from a
 * disagreement so large it more likely means the phone is held sideways, and never beyond
 * [MAX_OFFSET]. Standing still for a while lets the correction fade, since the interference that
 * caused it usually belongs to where you were.
 */
class CourseCorrection {
    /** Degrees to add to a compass heading. */
    var offset = 0.0
        private set

    private var anchor: PinFix? = null
    private var stillSinceMs: Long? = null

    /** Feed every estimate along with the compass's true heading at that moment. */
    fun update(fix: PinFix, compassTrueDegrees: Double?, nowMs: Long) {
        if (!fix.moving) {
            anchor = null
            val since = stillSinceMs ?: nowMs.also { stillSinceMs = it }
            if (nowMs - since > FADE_AFTER_MS) offset *= FADE
            return
        }
        stillSinceMs = null
        if (fix.estAccuracyMeters > MAX_ACCURACY || compassTrueDegrees == null) return
        val from = anchor ?: run { anchor = fix; return }
        val walked = GeoMath.distanceMeters(from.latitude, from.longitude, fix.latitude, fix.longitude)
        if (walked < STRIDE_METERS) return
        anchor = fix
        val course = GeoMath.initialBearingDegrees(from.latitude, from.longitude, fix.latitude, fix.longitude)
        val disagreement = wrap(course - compassTrueDegrees)
        if (abs(disagreement) > TRUST_WITHIN) return
        offset = (offset + GAIN * wrap(disagreement - offset)).coerceIn(-MAX_OFFSET, MAX_OFFSET)
    }

    fun apply(compassTrueDegrees: Double) = GeoMath.normalize360(compassTrueDegrees + offset)

    companion object {
        const val STRIDE_METERS = 8.0
        const val MAX_ACCURACY = 10f
        const val TRUST_WITHIN = 50.0
        const val MAX_OFFSET = 40.0
        const val GAIN = 0.25
        const val FADE_AFTER_MS = 60_000L
        const val FADE = 0.98

        /** An angle folded into -180..180. */
        fun wrap(deg: Double): Double {
            var d = deg % 360.0
            if (d > 180) d -= 360 else if (d < -180) d += 360
            return d
        }
    }
}
