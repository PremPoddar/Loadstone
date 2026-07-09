package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationAveragingTest {
    private fun fix(lat: Double, lon: Double, acc: Float, t: Long = 0) =
        RawFix(lat, lon, acc, t)

    @Test fun `no samples yields null`() {
        assertThat(LocationAveraging().current()).isNull()
    }

    @Test fun `single fix returns that fix`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0, -122.0, 10f))
        val c = avg.current()!!
        assertThat(c.latitude).isWithin(1e-9).of(37.0)
        assertThat(c.sampleCount).isEqualTo(1)
    }

    @Test fun `accuracy tightens as consistent samples accumulate`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0, -122.0, 12f, 0))
        val first = avg.current()!!.estAccuracyMeters
        repeat(20) { i -> avg.add(fix(37.0, -122.0, 12f, (i + 1) * 1000L)) }
        val later = avg.current()!!.estAccuracyMeters
        assertThat(later).isLessThan(first)
    }

    @Test fun `tighter fixes pull the estimate more than loose ones`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0000, -122.0, 40f, 0))    // loose
        avg.add(fix(37.0010, -122.0, 4f, 1000))  // tight
        val c = avg.current()!!
        assertThat(c.latitude).isGreaterThan(37.0008)
    }

    @Test fun `fixes worse than threshold are rejected`() {
        val avg = LocationAveraging(maxAccuracyMeters = 30f)
        avg.add(fix(37.0, -122.0, 100f))
        assertThat(avg.current()).isNull()
    }

    @Test fun `sustained movement resets the average`() {
        val avg = LocationAveraging(movementResetMeters = 15.0)
        repeat(5) { avg.add(fix(37.0000, -122.0, 8f, it * 1000L)) }
        repeat(5) { avg.add(fix(37.0009, -122.0, 8f, (5 + it) * 1000L)) }
        val c = avg.current()!!
        assertThat(c.latitude).isGreaterThan(37.0007)
    }
}
