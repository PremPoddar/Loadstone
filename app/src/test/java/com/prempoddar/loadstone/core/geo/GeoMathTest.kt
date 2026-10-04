package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

    @Test fun `facing heading tracks the user whether the phone is flat, tilted or upright`() {
        for (heading in listOf(0.0, 45.0, 137.0, 270.0, 359.0)) {
            for (pitch in listOf(0.0, 30.0, 60.0, 85.0)) {
                val h = GeoMath.facingHeadingDegrees(phone(heading, pitch), displayRotation = 0)!!
                assertThat(angularGap(h, heading)).isLessThan(0.5)
            }
        }
    }

    @Test fun `a little hand wobble barely moves the heading of a phone held up in front of you`() {
        // Held up to read while walking, 10° of wobble used to swing the arrow by about 64°.
        for (pitch in listOf(60.0, 75.0, 85.0)) {
            val h = GeoMath.facingHeadingDegrees(phone(0.0, pitch, roll = 10.0), displayRotation = 0)!!
            assertThat(angularGap(h, 0.0)).isLessThan(5.5)
        }
    }

    @Test fun `facing heading follows the screen in landscape and upside down`() {
        for (rotation in 0..3) {
            for (pitch in listOf(0.0, 60.0)) {
                val r = phone(200.0, pitch, displayRotation = rotation)
                val h = GeoMath.facingHeadingDegrees(r, rotation)!!
                assertThat(angularGap(h, 200.0)).isLessThan(0.5)
            }
        }
    }

    @Test fun `facing heading is undefined when the side of the screen points at the sky`() {
        // Portrait layout with the phone turned on its side (rotation locked): nothing is "ahead".
        val r = phone(0.0, pitch = 90.0, roll = 90.0)
        assertThat(GeoMath.facingHeadingDegrees(r, displayRotation = 0)).isNull()
    }

    @Test fun `arrow rotation takes the short way round however many turns have built up`() {
        // Two laps in, the reading goes from 350° to 0°: that is 10° on, not 350° back.
        assertThat(GeoMath.nearestEquivalentAngle(710f, 0f)).isWithin(0.01f).of(720f)
        assertThat(GeoMath.nearestEquivalentAngle(3610f, 5f)).isWithin(0.01f).of(3605f)
        assertThat(GeoMath.nearestEquivalentAngle(-710f, 0f)).isWithin(0.01f).of(-720f)
        assertThat(GeoMath.nearestEquivalentAngle(0f, 350f)).isWithin(0.01f).of(-10f)
    }

    /**
     * World-from-device rotation matrix (row-major, as SensorManager builds it) for a user facing
     * [heading], holding the phone tipped [pitch]° up from flat toward them, twisted [roll]° like a
     * steering wheel, with the screen drawn at [displayRotation] (Surface.ROTATION_*).
     */
    private fun phone(heading: Double, pitch: Double, roll: Double = 0.0, displayRotation: Int = 0): FloatArray {
        val t = Math.toRadians(pitch)
        val p = Math.toRadians(roll)
        val h = Math.toRadians(heading)
        // The screen's own axes, as (east, north, up), for a user facing north.
        val right = doubleArrayOf(cos(p), sin(p) * cos(t), sin(p) * sin(t))
        val up = doubleArrayOf(-sin(p), cos(p) * cos(t), cos(p) * sin(t))
        val normal = doubleArrayOf(0.0, -sin(t), cos(t))
        // Turn clockwise to face [heading].
        fun turn(v: DoubleArray) =
            doubleArrayOf(v[0] * cos(h) + v[1] * sin(h), -v[0] * sin(h) + v[1] * cos(h), v[2])
        fun neg(v: DoubleArray) = DoubleArray(3) { -v[it] }
        val (r, u, n) = Triple(turn(right), turn(up), turn(normal))
        // In landscape the screen's right and up edges are different edges of the device.
        val (x, y) = when (displayRotation) {
            1 -> u to neg(r)
            2 -> neg(r) to neg(u)
            3 -> neg(u) to r
            else -> r to u
        }
        return floatArrayOf(
            x[0].toFloat(), y[0].toFloat(), n[0].toFloat(),
            x[1].toFloat(), y[1].toFloat(), n[1].toFloat(),
            x[2].toFloat(), y[2].toFloat(), n[2].toFloat(),
        )
    }

    private fun angularGap(a: Double, b: Double) = abs(((a - b) % 360 + 540) % 360 - 180)
}
