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
}
