package com.prempoddar.loadstone.core.geo

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure geodesic helpers used by the accuracy engine and the compass. No Android
 * dependencies so it is trivially unit-testable.
 */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    private fun Double.toRad() = this * PI / 180.0
    private fun Double.toDeg() = this * 180.0 / PI

    /** Normalizes any angle into the [0, 360) range. */
    fun normalize360(deg: Double): Double = ((deg % 360) + 360) % 360

    /** Great-circle (Haversine) distance in meters between two lat/lon points. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).toRad()
        val dLon = (lon2 - lon1).toRad()
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1.toRad()) * cos(lat2.toRad()) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Initial great-circle bearing (0–360, true north) from point 1 toward point 2. */
    fun initialBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = lat1.toRad()
        val phi2 = lat2.toRad()
        val dLon = (lon2 - lon1).toRad()
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        return normalize360(atan2(y, x).toDeg())
    }

    /**
     * Angle (0–360) at which to draw the follow-back arrow, relative to where the
     * top of the phone currently points. 0 means "dead ahead".
     */
    fun arrowAngleDegrees(bearingToTarget: Double, deviceHeading: Double): Double =
        normalize360(bearingToTarget - deviceHeading)

    /**
     * Compass heading (0–360, clockwise from the north of [r]) of the direction the user faces
     * while looking at the screen. [r] is a world-from-device rotation matrix (row-major 3×3) as
     * SensorManager builds it; [displayRotation] is the Surface.ROTATION_* constant.
     *
     * This is deliberately not the heading of the phone's top edge, which is what
     * SensorManager.getOrientation's azimuth reports. Held up in front of you, that edge points
     * at the sky, so its heading is the tiny horizontal shadow of a near-vertical line: a few
     * degrees of hand wobble swung the arrow by tens of degrees. The screen's left–right edge
     * stays level however far the phone is tipped toward you, so the direction square to it is
     * always well defined: the top edge's heading when flat, the camera's when upright.
     *
     * Returns null when that edge itself points up (the phone is on its side with rotation
     * locked), where no direction is "ahead".
     */
    fun facingHeadingDegrees(r: FloatArray, displayRotation: Int): Double? {
        // East and north components of the screen's rightward edge. In landscape that is a
        // different edge of the device.
        val east: Float
        val north: Float
        when (displayRotation and 3) {
            1 -> { east = -r[1]; north = -r[4] } // ROTATION_90: the device's -Y
            2 -> { east = -r[0]; north = -r[3] } // ROTATION_180: -X
            3 -> { east = r[1]; north = r[4] }   // ROTATION_270: +Y
            else -> { east = r[0]; north = r[3] } // ROTATION_0: +X
        }
        if (east * east + north * north < MIN_LEVEL_SQUARED) return null
        // Facing = up × right, whose (east, north) is (-north, east).
        return normalize360(atan2(-north.toDouble(), east.toDouble()).toDeg())
    }

    /**
     * The angle equivalent to [target] (mod 360) nearest to [current], so an animated rotation
     * takes the short way round however many turns [current] has accumulated.
     */
    fun nearestEquivalentAngle(current: Float, target: Float): Float {
        // Reduce first: `%` keeps the dividend's sign, so once [current] passed ~540° the old
        // single-step form went negative and turned a 10° step into a 350° spin the wrong way.
        val delta = ((target - current) % 360f + 540f) % 360f - 180f
        return current + delta
    }

    /** Below this (the screen's side edge within ~6° of vertical) no heading is defined. */
    private const val MIN_LEVEL_SQUARED = 0.01f
}
