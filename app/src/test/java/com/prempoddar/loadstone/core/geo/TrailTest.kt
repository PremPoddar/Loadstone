package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrailTest {
    // Metres to degrees near Mumbai.
    private fun p(eastM: Double, northM: Double) = TrailPoint(19.0 + northM / 111_320.0, 72.8 + eastM / (111_320.0 * Math.cos(Math.toRadians(19.0))))

    /** An L-shaped walk: 100 m north from the pin, then 100 m east. */
    private val walk = (0..20).map { p(0.0, it * 5.0) } + (1..20).map { p(it * 5.0, 100.0) }

    @Test fun storesAndReadsBack() {
        val back = Trail.decode(Trail.encode(walk))
        assertThat(back.size).isEqualTo(walk.size)
        assertThat(GeoMath.distanceMeters(back[7].latitude, back[7].longitude, walk[7].latitude, walk[7].longitude)).isLessThan(0.2)
        assertThat(Trail.decode("")).isEmpty()
    }

    @Test fun thinsStraightStretchesButKeepsTheCorner() {
        val thin = Trail.simplify(walk)
        assertThat(thin).hasSize(3)
        assertThat(GeoMath.distanceMeters(thin[1].latitude, thin[1].longitude, p(0.0, 100.0).latitude, p(0.0, 100.0).longitude)).isLessThan(1.0)
        assertThat(Trail.length(thin)).isWithin(1.0).of(200.0)
    }

    @Test fun pointsAlongThePathRatherThanStraightAtThePin() {
        // At the far end, the pin is south-west, but the way back starts by heading west.
        val here = p(100.0, 100.0)
        val g = Trail.guide(walk, here.latitude, here.longitude)!!
        val bearing = GeoMath.initialBearingDegrees(here.latitude, here.longitude, g.target.latitude, g.target.longitude)
        assertThat(bearing).isWithin(3.0).of(270.0)
        assertThat(g.remainingMeters).isWithin(1.0).of(200.0)
    }

    @Test fun afterTheCornerItHeadsSouthToThePin() {
        val here = p(0.0, 60.0)
        val g = Trail.guide(walk, here.latitude, here.longitude)!!
        val bearing = GeoMath.initialBearingDegrees(here.latitude, here.longitude, g.target.latitude, g.target.longitude)
        assertThat(bearing).isWithin(3.0).of(180.0)
        assertThat(g.remainingMeters).isWithin(1.0).of(60.0)
    }

    @Test fun strayingFarSendsYouBackToThePath() {
        val here = p(50.0, 40.0) // 50 m east of the north leg
        val g = Trail.guide(walk, here.latitude, here.longitude)!!
        assertThat(g.offTrailMeters).isWithin(1.0).of(50.0)
        val bearing = GeoMath.initialBearingDegrees(here.latitude, here.longitude, g.target.latitude, g.target.longitude)
        assertThat(bearing).isWithin(3.0).of(270.0)
    }

    @Test fun recordsAPointEveryFewMetresAndSkipsRoughFixes() {
        val r = TrailRecorder()
        fun fix(northM: Double, acc: Float = 4f) = p(0.0, northM).let { PinFix(it.latitude, it.longitude, acc, 1) }
        assertThat(r.add(fix(0.0))).isTrue()
        assertThat(r.add(fix(2.0))).isFalse()
        assertThat(r.add(fix(6.0, acc = 40f))).isFalse()
        assertThat(r.add(fix(6.0))).isTrue()
        assertThat(r.trail).hasSize(2)
    }
}
