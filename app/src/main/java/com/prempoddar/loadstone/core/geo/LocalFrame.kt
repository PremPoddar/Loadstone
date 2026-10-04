package com.prempoddar.loadstone.core.geo

import kotlin.math.cos

/**
 * A local east/north tangent plane anchored at one latitude/longitude.
 *
 * Filtering positions directly in degrees is wrong: a degree of longitude is worth a degree of
 * latitude only at the equator, so a filter that treats the two alike is biased everywhere else,
 * and the bias grows with latitude. Over the few hundred meters a pin ever spans, the curvature of
 * the Earth is irrelevant, so converting to meters once and working in a flat frame is both exact
 * enough and far easier to reason about.
 */
class LocalFrame(val originLat: Double, val originLon: Double) {
    private val metersPerDegLat: Double
    private val metersPerDegLon: Double

    init {
        val phi = Math.toRadians(originLat)
        // WGS84 series for the length of a degree at this latitude (sub-centimeter over our range).
        metersPerDegLat = 111_132.92 - 559.82 * cos(2 * phi) + 1.175 * cos(4 * phi)
        metersPerDegLon = 111_412.84 * cos(phi) - 93.5 * cos(3 * phi)
    }

    /** Meters east of the origin. */
    fun east(lon: Double): Double = (lon - originLon) * metersPerDegLon

    /** Meters north of the origin. */
    fun north(lat: Double): Double = (lat - originLat) * metersPerDegLat

    fun latitudeOf(north: Double): Double = originLat + north / metersPerDegLat

    fun longitudeOf(east: Double): Double =
        // Near the poles a meter of easting spans an absurd number of degrees; clamping keeps the
        // conversion finite rather than producing NaN longitudes.
        if (metersPerDegLon.isFinite() && kotlin.math.abs(metersPerDegLon) > 1e-6) {
            originLon + east / metersPerDegLon
        } else {
            originLon
        }
}
