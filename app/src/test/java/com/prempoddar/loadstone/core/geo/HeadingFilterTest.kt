package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class HeadingFilterTest {

    private fun settleAt(degrees: Double): HeadingFilter =
        HeadingFilter().apply { repeat(200) { update(degrees) } }

    @Test fun `the first reading is reported as it stands`() {
        assertThat(HeadingFilter().update(123.0)).isWithin(0.001).of(123.0)
    }

    @Test fun `sensor noise is damped to a shiver you cannot see`() {
        val f = settleAt(90.0)
        val rng = Random(3)
        var worst = 0.0
        repeat(300) {
            val out = f.update(90.0 + rng.nextDouble(-1.0, 1.0))
            worst = maxOf(worst, HeadingFilter.angularDistance(out, 90.0))
        }
        assertThat(worst).isLessThan(0.5)
    }

    @Test fun `a real turn is followed promptly`() {
        val f = settleAt(0.0)
        // Quarter turn at the rate a person actually turns: ~90 degrees over half a second of
        // samples at the game rate.
        repeat(25) { f.update(90.0) }
        assertThat(HeadingFilter.angularDistance(f.update(90.0), 90.0)).isLessThan(3.0)
    }

    @Test fun `the north wrap is not a jump`() {
        val f = settleAt(359.0)
        var previous = 359.0
        repeat(40) {
            val out = f.update(3.0)
            // Every step must be a small one: going the long way round would show up here.
            assertThat(HeadingFilter.angularDistance(out, previous)).isLessThan(10.0)
            previous = out
        }
        assertThat(HeadingFilter.angularDistance(previous, 3.0)).isLessThan(1.0)
    }

    @Test fun `noise does not accumulate into drift`() {
        val f = settleAt(200.0)
        val rng = Random(11)
        repeat(2000) { f.update(200.0 + rng.nextDouble(-2.0, 2.0)) }
        assertThat(HeadingFilter.angularDistance(f.update(200.0), 200.0)).isLessThan(1.0)
    }

    @Test fun `angular distance takes the short way round`() {
        assertThat(HeadingFilter.angularDistance(350.0, 10.0)).isWithin(1e-9).of(20.0)
        assertThat(HeadingFilter.angularDistance(10.0, 350.0)).isWithin(1e-9).of(20.0)
        assertThat(abs(HeadingFilter.angularDistance(0.0, 180.0))).isWithin(1e-9).of(180.0)
    }
}
