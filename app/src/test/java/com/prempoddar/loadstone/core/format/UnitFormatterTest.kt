package com.prempoddar.loadstone.core.format

import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.data.settings.UnitSystem
import org.junit.Test

class UnitFormatterTest {
    @Test fun `metric under 1km shows meters`() {
        assertThat(UnitFormatter.formatDistance(42.4, UnitSystem.METRIC)).isEqualTo("42 m")
    }

    @Test fun `metric over 1km shows km with one decimal`() {
        assertThat(UnitFormatter.formatDistance(1540.0, UnitSystem.METRIC)).isEqualTo("1.5 km")
    }

    @Test fun `imperial under a mile shows feet`() {
        assertThat(UnitFormatter.formatDistance(30.0, UnitSystem.IMPERIAL)).isEqualTo("98 ft")
    }

    @Test fun `imperial over a mile shows miles`() {
        assertThat(UnitFormatter.formatDistance(3218.7, UnitSystem.IMPERIAL)).isEqualTo("2.0 mi")
    }
}
