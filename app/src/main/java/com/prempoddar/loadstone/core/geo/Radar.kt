package com.prempoddar.loadstone.core.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Lays pins out around you, as a radar does: you in the middle, each pin at its true direction
 * and at its distance scaled to the screen. Works with no map and no internet.
 */
object Radar {
    /** A pin's place on the radar: x and y run -1..1 from the centre (y down), [beyond] if past the edge. */
    data class Blip(val id: Long, val x: Double, val y: Double, val distanceMeters: Double, val beyond: Boolean)

    data class Target(val id: Long, val latitude: Double, val longitude: Double)

    /** Ring distances people read at a glance. */
    val RANGES = listOf(25.0, 50.0, 100.0, 200.0, 500.0, 1_000.0, 2_000.0, 5_000.0, 10_000.0, 20_000.0, 50_000.0, 100_000.0, 500_000.0, 2_000_000.0)

    /** The smallest range that fits [farthest] with a little room at the edge. */
    fun rangeFor(farthest: Double): Double = RANGES.firstOrNull { it >= farthest * 1.1 } ?: RANGES.last()

    /**
     * A range that shows the nearest few pins well, rather than shrinking everything to fit the
     * one in another city; pins further out sit on the rim, marked as beyond it.
     */
    fun autoRange(distances: List<Double>, show: Int = 5): Double {
        if (distances.isEmpty()) return RANGES[2]
        val nearest = distances.sorted().take(show)
        return rangeFor(nearest.last())
    }

    /** [facing] is the direction at the top of the screen: 0 for north up, or your heading. */
    fun layout(latitude: Double, longitude: Double, facing: Double, range: Double, pins: List<Target>): List<Blip> = pins.map { p ->
        val d = GeoMath.distanceMeters(latitude, longitude, p.latitude, p.longitude)
        val angle = (GeoMath.initialBearingDegrees(latitude, longitude, p.latitude, p.longitude) - facing) * PI / 180
        val r = min(d / range, 1.0)
        Blip(p.id, r * sin(angle), -r * cos(angle), d, d > range)
    }
}
