package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoMathTest {
    @Test fun `distance between same point is zero`() {
        assertThat(GeoMath.distanceMeters(37.0, -122.0, 37.0, -122.0)).isWithin(0.01).of(0.0)
    }

    @Test fun `distance one degree of latitude is about 111 km`() {
        val d = GeoMath.distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertThat(d).isWithin(1000.0).of(111_195.0)
    }

    @Test fun `bearing due north is zero`() {
        assertThat(GeoMath.initialBearingDegrees(0.0, 0.0, 1.0, 0.0)).isWithin(0.5).of(0.0)
    }

    @Test fun `bearing due east is ninety`() {
        assertThat(GeoMath.initialBearingDegrees(0.0, 0.0, 0.0, 1.0)).isWithin(0.5).of(90.0)
    }

    @Test fun `arrow angle points ahead when heading equals bearing`() {
        assertThat(GeoMath.arrowAngleDegrees(90.0, 90.0)).isWithin(0.01).of(0.0)
    }

    @Test fun `arrow angle wraps into 0 to 360`() {
        assertThat(GeoMath.arrowAngleDegrees(10.0, 350.0)).isWithin(0.01).of(20.0)
    }
}
