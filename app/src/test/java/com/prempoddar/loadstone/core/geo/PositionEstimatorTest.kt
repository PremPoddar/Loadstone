package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.cos
import kotlin.random.Random

class PositionEstimatorTest {
    private val lat = 37.4219
    private val lon = -122.0840
    private val metersPerDegLat = 111_132.0
    private val metersPerDegLon = 111_320.0 * cos(Math.toRadians(lat))

    private fun at(
        eastM: Double,
        northM: Double,
        accuracy: Float,
        timeMs: Long,
        speed: Float? = 0f,
        altitude: Double? = null,
    ) = RawFix(
        latitude = lat + northM / metersPerDegLat,
        longitude = lon + eastM / metersPerDegLon,
        accuracyMeters = accuracy,
        elapsedRealtimeMs = timeMs,
        altitudeMeters = altitude,
        speedMps = speed,
    )

    private fun PinFix.errorMeters() = GeoMath.distanceMeters(latitude, longitude, lat, lon)

    @Test fun `no estimate before any usable fix`() {
        val e = PositionEstimator()
        assertThat(e.current()).isNull()
        e.add(at(0.0, 0.0, accuracy = 400f, timeMs = 0))
        assertThat(e.current()).isNull()
    }

    @Test fun `standing still converges on the true spot`() {
        val e = PositionEstimator()
        val rng = Random(7)
        repeat(40) { i ->
            e.add(
                at(
                    eastM = rng.nextDouble(-8.0, 8.0),
                    northM = rng.nextDouble(-8.0, 8.0),
                    accuracy = 8f,
                    timeMs = i * 1000L,
                ),
            )
        }
        val fix = e.current()!!
        assertThat(fix.errorMeters()).isLessThan(4.0)
        assertThat(fix.settled).isTrue()
        assertThat(fix.moving).isFalse()
    }

    @Test fun `the reported accuracy never claims more than GPS can deliver`() {
        val e = PositionEstimator()
        // A hundred perfectly agreeing fixes: independent-sample maths would claim centimeters.
        repeat(100) { i -> e.add(at(0.0, 0.0, accuracy = 10f, timeMs = i * 1000L)) }
        val fix = e.current()!!
        assertThat(fix.estAccuracyMeters).isAtLeast(10f * PositionEstimator.CORRELATED_FRACTION.toFloat())
        assertThat(fix.estAccuracyMeters).isAtLeast(PositionEstimator.ABSOLUTE_FLOOR_M.toFloat())
    }

    @Test fun `accuracy improves as fixes accumulate but stays honest`() {
        val e = PositionEstimator()
        e.add(at(0.0, 0.0, accuracy = 12f, timeMs = 0))
        val first = e.current()!!.estAccuracyMeters
        repeat(20) { i -> e.add(at(0.0, 0.0, accuracy = 12f, timeMs = (i + 1) * 1000L)) }
        val later = e.current()!!.estAccuracyMeters
        assertThat(later).isLessThan(first)
        assertThat(later).isGreaterThan(0f)
    }

    @Test fun `walking up to a spot leaves the pin at the spot, not back along the path`() {
        val e = PositionEstimator()
        var t = 0L
        // Approach from 60 m south at a brisk 1.5 m/s, a fix a second.
        for (step in 40 downTo 1) {
            e.add(at(0.0, -step * 1.5, accuracy = 6f, timeMs = t, speed = 1.5f))
            t += 1000
        }
        // Then stand still for half a minute.
        repeat(30) {
            e.add(at(0.0, 0.0, accuracy = 6f, timeMs = t, speed = 0f))
            t += 1000
        }
        assertThat(e.current()!!.errorMeters()).isLessThan(3.0)
    }

    @Test fun `a single wild fix does not move the estimate`() {
        val e = PositionEstimator()
        repeat(20) { i -> e.add(at(0.0, 0.0, accuracy = 5f, timeMs = i * 1000L)) }
        val before = e.current()!!
        e.add(at(70.0, 70.0, accuracy = 5f, timeMs = 20_000))
        val after = e.current()!!
        assertThat(
            GeoMath.distanceMeters(before.latitude, before.longitude, after.latitude, after.longitude),
        ).isLessThan(0.5)
    }

    @Test fun `sustained disagreement wins - the user really has moved`() {
        val e = PositionEstimator()
        repeat(20) { i -> e.add(at(0.0, 0.0, accuracy = 5f, timeMs = i * 1000L)) }
        // Phone handed over a wall: every later fix agrees on a new spot.
        repeat(10) { i -> e.add(at(90.0, 0.0, accuracy = 5f, timeMs = 20_000L + i * 1000L)) }
        val fix = e.current()!!
        assertThat(fix.errorMeters()).isGreaterThan(80.0)
    }

    @Test fun `a long gap widens the uncertainty rather than pretending nothing happened`() {
        val tight = PositionEstimator().apply {
            repeat(20) { i -> add(at(0.0, 0.0, accuracy = 5f, timeMs = i * 1000L)) }
        }
        val before = tight.current()!!.estAccuracyMeters
        // Phone in a pocket for two minutes, then one fix at walking pace.
        tight.add(at(0.0, 0.0, accuracy = 5f, timeMs = 140_000L, speed = 1.4f))
        assertThat(tight.current()!!.estAccuracyMeters).isGreaterThan(before)
        assertThat(tight.current()!!.moving).isTrue()
    }

    @Test fun `altitude is filtered alongside position`() {
        val e = PositionEstimator()
        repeat(20) { i -> e.add(at(0.0, 0.0, accuracy = 6f, timeMs = i * 1000L, altitude = 100.0 + (i % 2) * 4)) }
        assertThat(e.current()!!.altitudeMeters!!).isWithin(3.0).of(102.0)
    }

    @Test fun `reset clears everything`() {
        val e = PositionEstimator()
        repeat(5) { i -> e.add(at(0.0, 0.0, accuracy = 5f, timeMs = i * 1000L)) }
        e.reset()
        assertThat(e.current()).isNull()
    }

    @Test fun `a pin follows the fixes as they settle instead of freezing on the first ones`() {
        // What the fused provider does just after you stop: ten seconds still 6 m out while it
        // settles, then good fixes on the spot. Those later fixes are the ones that count.
        val e = PositionEstimator()
        val r = Random(11)
        var t = 0L
        repeat(10) { e.add(at(6.0 + r.nextDouble(-0.5, 0.5), r.nextDouble(-0.5, 0.5), accuracy = 8f, timeMs = t)); t += 1000 }
        repeat(30) { e.add(at(r.nextDouble(-1.0, 1.0), r.nextDouble(-1.0, 1.0), accuracy = 4f, timeMs = t)); t += 1000 }
        assertThat(e.current()!!.errorMeters()).isLessThan(1.5)
    }

    @Test fun `stopping after a walk saves where you stopped, not where you were`() {
        val e = PositionEstimator()
        val r = Random(4)
        var t = 0L
        // Walk 40 m north at 1.3 m/s, then stand at the spot for 15 s; fixes lag a little behind as fused ones do.
        for (i in 0..30) { e.add(at(0.0, i * 1.3 - 40.0 - 2.0, accuracy = 6f, timeMs = t, speed = 1.3f)); t += 1000 }
        repeat(15) { e.add(at(r.nextDouble(-1.0, 1.0), r.nextDouble(-1.0, 1.0), accuracy = 5f, timeMs = t, speed = 0f)); t += 1000 }
        assertThat(e.current()!!.errorMeters()).isLessThan(1.5)
    }
}
