package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CourseCorrectionTest {
    private fun fix(northMeters: Double, moving: Boolean = true, acc: Float = 4f) =
        PinFix(19.0 + northMeters / 111_000.0, 72.8, acc, sampleCount = 10, moving = moving)

    /** Walk due north in 10 m strides while the compass claims [compass]. */
    private fun walkNorth(c: CourseCorrection, compass: Double, strides: Int) {
        for (i in 0..strides) c.update(fix(i * 10.0), compass, i * 1000L)
    }

    @Test fun learnsACompassThatReadsOff() {
        val c = CourseCorrection()
        walkNorth(c, compass = 20.0, strides = 30) // the compass says 20° while you walk due north
        assertThat(c.apply(20.0)).isWithin(2.0).of(0.0)
    }

    @Test fun ignoresDisagreementsTooBigToBeMagnetic() {
        val c = CourseCorrection()
        walkNorth(c, compass = 90.0, strides = 30) // phone held sideways, not a bad compass
        assertThat(c.offset).isEqualTo(0.0)
    }

    @Test fun neverCorrectsBeyondTheLimit() {
        val c = CourseCorrection()
        walkNorth(c, compass = 315.0, strides = 200) // 45° out: within trust, beyond the cap
        assertThat(c.offset).isAtMost(CourseCorrection.MAX_OFFSET)
    }

    @Test fun learnsNothingStandingStill() {
        val c = CourseCorrection()
        repeat(20) { c.update(fix(0.0, moving = false), 20.0, it * 1000L) }
        assertThat(c.offset).isEqualTo(0.0)
    }

    @Test fun wrapsAnglesTheShortWay() {
        assertThat(CourseCorrection.wrap(350.0)).isEqualTo(-10.0)
        assertThat(CourseCorrection.wrap(-190.0)).isEqualTo(170.0)
    }
}
