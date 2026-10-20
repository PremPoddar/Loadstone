package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class ElevationFusionTest {
    /** The pressure at [meters] in the standard atmosphere, the inverse of pressureAltitude. */
    private fun hPa(meters: Double) = (1013.25 * Math.pow(1 - meters / 44_330.0, 5.255)).toFloat()

    @Test fun followsAClimbSmoothlyWithABarometer() {
        val e = ElevationFusion()
        val r = Random(3)
        e.onPressure(hPa(10.0))
        e.onGps(10.0)
        // Climb 9 m (three floors) while GPS jumps about by ±6 m.
        for (step in 1..90) {
            val truth = 10.0 + step * 0.1
            e.onPressure(hPa(truth))
            e.onGps(truth + r.nextDouble(-6.0, 6.0), 5f)
        }
        assertThat(e.meters!!).isWithin(2.0).of(19.0)
    }

    @Test fun gpsCorrectsWeatherDriftOverTime() {
        val e = ElevationFusion()
        e.onPressure(hPa(50.0)); e.onGps(50.0)
        // The weather changes: pressure says you rose 20 m, but you never moved.
        e.onPressure(hPa(70.0))
        repeat(300) { e.onGps(50.0, 4f) }
        assertThat(e.meters!!).isWithin(1.0).of(50.0)
    }

    @Test fun smoothsGpsAloneWithoutABarometer() {
        val e = ElevationFusion()
        val r = Random(5)
        repeat(200) { e.onGps(100.0 + r.nextDouble(-5.0, 5.0)) }
        assertThat(e.meters!!).isWithin(2.0).of(100.0)
    }
}
