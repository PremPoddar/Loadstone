package com.prempoddar.loadstone.ui.drop

import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.data.pin.PinCategory
import org.junit.Test

/**
 * Saving a loose fix bakes its error into the pin permanently, which is what made a saved spot
 * read tens of meters away on returning. Saving stays blocked until the estimate is tight.
 */
class DropUiStateTest {

    private fun stateWith(accuracy: Float) =
        DropUiState(fix = PinFix(37.0, -122.0, accuracy, sampleCount = 10))

    @Test fun `cannot save without a fix`() {
        assertThat(DropUiState().canSave).isFalse()
    }

    @Test fun `cannot save a loose fix`() {
        assertThat(stateWith(45f).canSave).isFalse()
        assertThat(stateWith(21f).canSave).isFalse()
    }

    @Test fun `can save once the estimate is tight enough`() {
        assertThat(stateWith(DropPinViewModel.SAVE_MAX_ACCURACY).canSave).isTrue()
        assertThat(stateWith(4f).canSave).isTrue()
    }

    @Test fun `cannot save while a save is already in flight`() {
        assertThat(stateWith(4f).copy(saving = true).canSave).isFalse()
    }

    @Test fun `the pin is saved where the user tapped, not where they drift to while naming it`() {
        val onTheSpot = PinFix(37.0, -122.0, 4f, sampleCount = 20)
        val locked = DropUiState(fix = onTheSpot).lock()!!
        // Strolling off while typing a name: the live estimate follows (about 33 m here).
        val whileNaming = locked.copy(fix = PinFix(37.0003, -122.0, 4f, sampleCount = 25))

        val pin = whileNaming.pinToSave("Car", null, PinCategory.CAR, emptyList())!!

        assertThat(pin.latitude).isEqualTo(37.0)
        assertThat(pin.longitude).isEqualTo(-122.0)
        assertThat(pin.accuracyMeters).isEqualTo(4f)
    }

    @Test fun `a loose estimate cannot be locked in`() {
        assertThat(stateWith(45f).lock()).isNull()
    }

    @Test fun `nothing is saved unless a spot was locked in`() {
        assertThat(stateWith(4f).pinToSave("Car", null, PinCategory.CAR, emptyList())).isNull()
    }
}
