package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinPolishTest {
    // About 1.1 m per 0.00001° of latitude.
    private fun fix(northMeters: Double, acc: Float, moving: Boolean = false) =
        PinFix(19.0 + northMeters / 111_000.0, 72.8, acc, sampleCount = 10, moving = moving)

    private val saved = fix(0.0, 6f)

    @Test fun takesATighterReadingOfTheSameSpot() {
        val next = fix(1.0, 4f)
        assertThat(PinPolish.step(saved, saved, next)).isEqualTo(PinPolish.Step.Improve(next))
    }

    @Test fun ignoresReadingsThatAreNoBetter() {
        assertThat(PinPolish.step(saved, saved, fix(0.5, 5.9f))).isEqualTo(PinPolish.Step.Keep)
        assertThat(PinPolish.step(saved, fix(0.0, 3f), fix(0.5, 3.5f))).isEqualTo(PinPolish.Step.Keep)
    }

    @Test fun stopsOnceThePersonWalksOff() {
        assertThat(PinPolish.step(saved, saved, fix(15.0, 3f))).isEqualTo(PinPolish.Step.Stop)
        assertThat(PinPolish.step(saved, saved, fix(1.0, 3f, moving = true))).isEqualTo(PinPolish.Step.Stop)
    }

    @Test fun takesASettledReadingThatHasCaughtUp() {
        // Same accuracy, but settled and 2 m on: the estimate was still catching up at Save.
        val next = PinFix(19.0 + 2.0 / 111_000.0, 72.8, 6f, sampleCount = 20, settled = true)
        assertThat(PinPolish.step(saved, saved, next)).isEqualTo(PinPolish.Step.Improve(next))
        // Unsettled, it waits.
        assertThat(PinPolish.step(saved, saved, next.copy(settled = false))).isEqualTo(PinPolish.Step.Keep)
    }
}
