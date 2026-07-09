package com.prempoddar.loadstone.core.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoordinateFormatTest {
    @Test fun `format then parse round trips`() {
        val s = CoordinateFormat.format(37.42199, -122.08405)
        val p = CoordinateFormat.parse(s)!!
        assertThat(p.first).isWithin(1e-5).of(37.42199)
        assertThat(p.second).isWithin(1e-5).of(-122.08405)
    }

    @Test fun `parses plain comma separated`() {
        val p = CoordinateFormat.parse("37.42199, -122.08405")!!
        assertThat(p.first).isWithin(1e-5).of(37.42199)
    }

    @Test fun `parses geo uri`() {
        val p = CoordinateFormat.parse("geo:37.42199,-122.08405")!!
        assertThat(p.second).isWithin(1e-5).of(-122.08405)
    }

    @Test fun `rejects garbage`() {
        assertThat(CoordinateFormat.parse("not coords")).isNull()
    }
}
