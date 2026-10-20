package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RadarTest {
    private val lat = 19.0; private val lon = 72.8
    private fun north(m: Double) = Radar.Target(1, lat + m / 111_320.0, lon)
    private fun east(m: Double) = Radar.Target(2, lat, lon + m / (111_320.0 * Math.cos(Math.toRadians(lat))))

    @Test fun northIsUpAndEastIsRightWithNorthUp() {
        val (n, e) = Radar.layout(lat, lon, 0.0, 100.0, listOf(north(50.0), east(50.0)))
        assertThat(n.x).isWithin(0.01).of(0.0); assertThat(n.y).isWithin(0.01).of(-0.5)
        assertThat(e.x).isWithin(0.01).of(0.5); assertThat(e.y).isWithin(0.01).of(0.0)
    }

    @Test fun turnsWithYouWhenFacingUp() {
        // Facing east: a pin to the east is straight ahead, at the top.
        val e = Radar.layout(lat, lon, 90.0, 100.0, listOf(east(50.0))).single()
        assertThat(e.x).isWithin(0.01).of(0.0); assertThat(e.y).isWithin(0.01).of(-0.5)
    }

    @Test fun farPinsSitOnTheRim() {
        val far = Radar.layout(lat, lon, 0.0, 100.0, listOf(north(5_000.0))).single()
        assertThat(far.beyond).isTrue()
        assertThat(far.y).isWithin(0.001).of(-1.0)
    }

    @Test fun picksRoundRangesThatFitTheNearestPins() {
        assertThat(Radar.rangeFor(80.0)).isEqualTo(100.0)
        assertThat(Radar.rangeFor(95.0)).isEqualTo(200.0) // a little room at the edge
        assertThat(Radar.autoRange(listOf(30.0, 60.0, 120.0, 3_000_000.0), show = 3)).isEqualTo(200.0)
        assertThat(Radar.autoRange(emptyList())).isEqualTo(100.0)
    }
}
