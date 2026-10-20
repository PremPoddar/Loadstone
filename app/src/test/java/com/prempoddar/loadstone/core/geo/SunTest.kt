package com.prempoddar.loadstone.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class SunTest {
    private fun local(ms: Long, zone: String) = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.of(zone))

    @Test fun mumbaiSunsetInMarch() {
        // Published tables: 21 March 2025 in Mumbai, sunrise about 06:38 and sunset about 18:48 IST;
        // the formula is good to a few minutes, plenty for a daylight warning.
        val noon = ZonedDateTime.of(2025, 3, 21, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
        val set = local(Sun.sunsetMs(19.076, 72.8777, noon)!!, "Asia/Kolkata")
        assertThat(set.hour * 60 + set.minute).isIn(com.google.common.collect.Range.closed(18 * 60 + 44, 18 * 60 + 52))
        val rise = local(Sun.sunriseMs(19.076, 72.8777, noon)!!, "Asia/Kolkata")
        assertThat(rise.hour * 60 + rise.minute).isIn(com.google.common.collect.Range.closed(6 * 60 + 32, 6 * 60 + 46))
    }

    @Test fun londonMidsummerSunset() {
        // 21 June 2025, London: sunset about 21:21 BST.
        val noon = ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, ZoneId.of("Europe/London")).toInstant().toEpochMilli()
        val set = local(Sun.sunsetMs(51.5074, -0.1278, noon)!!, "Europe/London")
        assertThat(set.hour * 60 + set.minute).isIn(com.google.common.collect.Range.closed(21 * 60 + 17, 21 * 60 + 25))
    }

    @Test fun noSunsetInPolarSummer() {
        val noon = ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertThat(Sun.sunsetMs(78.2, 15.6, noon)).isNull()
    }

    @Test fun warnsOnlyWhenTheWalkRunsIntoDark() {
        val noon = ZonedDateTime.of(2025, 3, 21, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
        val set = Sun.sunsetMs(19.076, 72.8777, noon)!!
        val hourBefore = set - 60 * 60_000
        assertThat(Sun.warning(19.076, 72.8777, 5_000.0, hourBefore)).isNotNull() // 64 min walk, 60 min of light
        assertThat(Sun.warning(19.076, 72.8777, 1_000.0, hourBefore)).isNull() // 13 min walk
        assertThat(Sun.warning(19.076, 72.8777, 5_000.0, noon)).isNull()
        assertThat(Sun.warning(19.076, 72.8777, 5_000.0, set + 60_000)).isNull() // already dark
    }
}
