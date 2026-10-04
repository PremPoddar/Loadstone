package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.core.geo.PinRefinement.Observation
import org.junit.Test

class PinRefinementTest {
    private val lat = 51.5074
    private val lon = -0.1278
    private val metersPerDegLat = 111_132.0

    private fun northOf(meters: Double, sigma: Double) =
        Observation(lat + meters / metersPerDegLat, lon, sigma)

    @Test fun `a tighter visit pulls the pin most of the way toward itself`() {
        val stored = northOf(0.0, sigma = 12.0)
        val observed = northOf(10.0, sigma = 4.0)
        val merged = PinRefinement.merge(stored, observed)
        val moved = GeoMath.distanceMeters(stored.latitude, stored.longitude, merged.latitude, merged.longitude)
        assertThat(moved).isGreaterThan(8.0)
        assertThat(moved).isLessThan(10.0)
    }

    @Test fun `two independent visits beat either one`() {
        val merged = PinRefinement.merge(northOf(0.0, 8.0), northOf(6.0, 8.0))
        assertThat(merged.sigmaMeters).isLessThan(8.0)
    }

    @Test fun `the merged uncertainty never drops below what GPS can do`() {
        var obs = Observation(lat, lon, 4.0)
        repeat(20) { obs = PinRefinement.merge(obs, Observation(lat, lon, 4.0)) }
        assertThat(obs.sigmaMeters).isAtLeast(PositionEstimator.ABSOLUTE_FLOOR_M)
    }

    @Test fun `a looser visit is not worth merging`() {
        assertThat(PinRefinement.worthMerging(northOf(0.0, 4.0), northOf(1.0, 30.0))).isFalse()
    }

    @Test fun `a visit that disagrees wildly is a different place, not a correction`() {
        assertThat(PinRefinement.worthMerging(northOf(0.0, 5.0), northOf(300.0, 5.0))).isFalse()
    }

    @Test fun `standing on the spot with a good fix is worth merging`() {
        assertThat(PinRefinement.worthMerging(northOf(0.0, 9.0), northOf(5.0, 4.0))).isTrue()
    }

    @Test fun `longitude is weighted correctly away from the equator`() {
        // Two observations 10 m apart in longitude at 51 N. Averaging raw degrees would move the
        // pin the same number of degrees as a 10 m latitude step, which is 16 m on the ground.
        val metersPerDegLon = 111_320.0 * Math.cos(Math.toRadians(lat))
        val a = Observation(lat, lon, 5.0)
        val b = Observation(lat, lon + 10.0 / metersPerDegLon, 5.0)
        val merged = PinRefinement.merge(a, b)
        assertThat(GeoMath.distanceMeters(a.latitude, a.longitude, merged.latitude, merged.longitude))
            .isWithin(0.5).of(5.0)
    }
}
