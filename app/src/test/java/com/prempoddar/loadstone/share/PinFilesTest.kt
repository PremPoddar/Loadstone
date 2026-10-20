package com.prempoddar.loadstone.share

import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.core.geo.TrailPoint
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinFilesTest {
    private val car = Pin(
        name = "Car <level 2>", note = "Pillar C4 & lift", latitude = 19.0760901, longitude = 72.8774262, accuracyMeters = 4.5f,
        category = PinCategory.CAR, elevationMeters = 14.2, isPinned = true, createdAt = 1_760_000_000_000,
        trail = listOf(TrailPoint(19.07609, 72.87742), TrailPoint(19.0765, 72.8779)),
        photoPaths = listOf("/data/x/pin_photos/pin_1.jpg"), voiceNotes = listOf("/data/x/voice/note_1.m4a"),
    )
    private val tent = Pin(name = "Tent", latitude = -33.9, longitude = 18.4, accuracyMeters = 6f, category = PinCategory.TENT)

    @Test fun backupKeepsEverything() {
        val back = PinFiles.fromJson(PinFiles.toJson(listOf(car, tent)))
        val c = back[0]
        assertThat(c.name).isEqualTo(car.name); assertThat(c.note).isEqualTo(car.note)
        assertThat(c.latitude).isWithin(1e-9).of(car.latitude); assertThat(c.category).isEqualTo(PinCategory.CAR)
        assertThat(c.elevationMeters).isWithin(1e-9).of(14.2); assertThat(c.isPinned).isTrue()
        assertThat(c.trail).hasSize(2); assertThat(c.photoPaths).containsExactly("pin_1.jpg"); assertThat(c.voiceNotes).containsExactly("note_1.m4a")
        assertThat(back[1].note).isNull(); assertThat(back[1].elevationMeters).isNull()
        assertThat(PinFiles.samePin(c, car)).isTrue()
    }

    @Test fun refusesOtherJson() {
        assertThat(runCatching { PinFiles.fromJson("{\"pins\":[]}") }.isFailure).isTrue()
    }

    @Test fun gpxAndKmlReadBackAsPlaces() {
        for (text in listOf(PinFiles.toGpx(listOf(car, tent)), PinFiles.toKml(listOf(car, tent)))) {
            val places = PinFiles.readPlaces(text.toByteArray())
            assertThat(places).hasSize(2) // the recorded path is a track, not a place
            assertThat(places[0].name).isEqualTo("Car <level 2>")
            assertThat(places[0].note).isEqualTo("Pillar C4 & lift")
            assertThat(places[0].latitude).isWithin(1e-6).of(19.0760901)
            assertThat(places[0].elevation!!).isWithin(0.1).of(14.2)
            assertThat(places[1].longitude).isWithin(1e-6).of(18.4)
        }
        assertThat(PinFiles.toGpx(listOf(car))).contains("<trk>")
    }

    @Test fun readsOtherAppsFiles() {
        val gpx = """<?xml version="1.0"?><gpx><wpt lat="12.5" lon="77.6"><name>Summit</name><cmt>cairn</cmt></wpt><wpt lat="bad" lon="1"/></gpx>"""
        val p = PinFiles.readPlaces(gpx.toByteArray()).single()
        assertThat(p.name).isEqualTo("Summit"); assertThat(p.note).isEqualTo("cairn")
    }
}
