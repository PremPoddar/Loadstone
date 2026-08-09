package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationAveragingTest {
    private fun fix(lat: Double, lon: Double, acc: Float, t: Long = 0) =
        RawFix(lat, lon, acc, t)

    @Test fun `no samples yields null`() {
        assertThat(LocationAveraging().current()).isNull()
    }

    @Test fun `single fix returns that position`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0, -122.0, 10f))
        val c = avg.current()!!
        assertThat(c.latitude).isWithin(1e-9).of(37.0)
        assertThat(c.sampleCount).isEqualTo(1)
    }

    @Test fun `estimate improves as the hardware accuracy improves`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0, -122.0, 30f, 0))
        val first = avg.current()!!.estAccuracyMeters
        listOf(20f, 12f, 8f, 6f).forEachIndexed { i, acc ->
            avg.add(fix(37.0, -122.0, acc, (i + 1) * 1000L))
        }
        val later = avg.current()!!.estAccuracyMeters
        assertThat(later).isLessThan(first)
    }

    @Test fun `noisy fixes keep the estimate honest, not falsely tight`() {
        val avg = LocationAveraging()
        // Consistently poor ~40 m fixes scattered ~30 m around a point.
        val offsets = listOf(0.0, 0.0002, -0.0002, 0.0001, -0.0001, 0.00025)
        offsets.forEachIndexed { i, dLat ->
            avg.add(fix(37.0 + dLat, -122.0, 40f, i * 1000L))
        }
        // Must not collapse to single digits just because samples piled up.
        assertThat(avg.current()!!.estAccuracyMeters).isGreaterThan(15f)
    }

    @Test fun `estimate never claims tighter than 3 meters`() {
        val avg = LocationAveraging()
        repeat(10) { avg.add(fix(37.0, -122.0, 4f, it * 1000L)) }
        assertThat(avg.current()!!.estAccuracyMeters).isAtLeast(3f)
    }

    @Test fun `tighter fixes pull the estimate more than loose ones`() {
        val avg = LocationAveraging()
        avg.add(fix(37.00000, -122.0, 40f, 0))     // loose
        avg.add(fix(37.00005, -122.0, 4f, 1000))   // tight, ~5.5 m away (within threshold)
        val c = avg.current()!!
        assertThat(c.latitude).isGreaterThan(37.00003)
    }

    @Test fun `a lone outlier does not wipe the accumulated average`() {
        val avg = LocationAveraging()
        repeat(6) { avg.add(fix(37.0000, -122.0, 8f, it * 1000L)) }
        avg.add(fix(37.0100, -122.0, 8f, 6000L)) // ~1.1 km jump, single outlier
        // Still centered on the original spot; the outlier was skipped, not blended or reset.
        assertThat(avg.current()!!.latitude).isWithin(1e-4).of(37.0000)
    }

    @Test fun `sustained movement rebuilds at the new location`() {
        val avg = LocationAveraging()
        repeat(5) { avg.add(fix(37.0000, -122.0, 8f, it * 1000L)) }
        // Six consecutive fixes ~100 m away: real walking.
        repeat(6) { avg.add(fix(37.0009, -122.0, 8f, (5 + it) * 1000L)) }
        assertThat(avg.current()!!.latitude).isGreaterThan(37.0007)
    }

    @Test fun `stale samples age out of the window`() {
        val avg = LocationAveraging(windowMs = 10_000)
        avg.add(fix(37.0000, -122.0, 8f, 0))
        avg.add(fix(37.0000, -122.0, 8f, 20_000)) // 20 s later; first sample evicted
        assertThat(avg.current()!!.sampleCount).isEqualTo(1)
    }

    @Test fun `coarse network fixes are rejected outright`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0, -122.0, 800f))   // cell-tower style guess
        assertThat(avg.current()).isNull()
    }

    @Test fun `a coarse fix cannot drag a converged pin off the spot`() {
        val avg = LocationAveraging()
        repeat(8) { avg.add(fix(37.00000, -122.0, 5f, it * 1000L)) }
        val before = avg.current()!!.latitude
        // ~600 m away, the kind of Wi-Fi fix that used to shift the saved pin
        avg.add(fix(37.00540, -122.0, 300f, 9000L))
        assertThat(avg.current()!!.latitude).isWithin(1e-6).of(before)
    }

    @Test fun `borderline fixes are still accepted`() {
        val avg = LocationAveraging()
        avg.add(fix(37.0, -122.0, LocationAveraging.MAX_USABLE_ACCURACY))
        assertThat(avg.current()).isNotNull()
    }

    @Test fun `walking up to the spot does not drag the pin back along the approach`() {
        val avg = LocationAveraging()
        val lat = 37.42150
        // 45 s of walking in, one fix per second, ending exactly on the spot.
        repeat(46) { i ->
            val remainingMeters = (45 - i) * 1.3
            avg.add(fix(lat - remainingMeters / 111_195.0, -122.0, 6f, i * 1000L))
        }
        // A few seconds standing still, then save.
        repeat(4) { i -> avg.add(fix(lat, -122.0, 5f, (46 + i) * 1000L)) }

        val c = avg.current()!!
        val errorMeters = GeoMath.distanceMeters(c.latitude, c.longitude, lat, -122.0)
        assertThat(errorMeters).isLessThan(6.0)
    }

    @Test fun `averages altitude when present`() {
        val avg = LocationAveraging()
        avg.add(RawFix(37.0, -122.0, 8f, 0, altitudeMeters = 100.0))
        avg.add(RawFix(37.0, -122.0, 8f, 1000, altitudeMeters = 120.0))
        assertThat(avg.current()!!.altitudeMeters!!).isWithin(0.001).of(110.0)
    }
}
