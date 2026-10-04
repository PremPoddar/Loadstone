package com.prempoddar.loadstone.ui.navigate

import android.hardware.SensorManager
import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.data.pin.Pin
import org.junit.Test

class NavigateStateTest {
    private val pin = Pin(id = 1, name = "Car", latitude = 37.0, longitude = -122.0, accuracyMeters = 4f)

    /** 100 m due south of the pin, so the pin lies due north. */
    private val southOfPin = NavFix(latitude = 37.0 - 100 / 111_195.0, longitude = -122.0, accuracyMeters = 5f)

    private fun state(
        fix: NavFix? = southOfPin,
        heading: Double? = 0.0,
        accuracyClass: Int? = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
        compassAvailable: Boolean = true,
    ) = navigateState(pin, fix, heading, accuracyClass, compassAvailable)

    @Test fun `arrow points at the pin relative to the way the user faces`() {
        // Pin due north, user facing east: it is a quarter turn to their left.
        assertThat(state(heading = 90.0).arrowAngle!!.toDouble()).isWithin(0.5).of(270.0)
    }

    @Test fun `distance shows before the compass has reported`() {
        val s = state(heading = null)
        assertThat(s.pin).isEqualTo(pin)
        assertThat(s.distanceMeters!!).isWithin(1.0).of(100.0)
        assertThat(s.arrowAngle).isNull()
    }

    @Test fun `distance still shows on a phone with no compass`() {
        val s = state(heading = null, accuracyClass = null, compassAvailable = false)
        assertThat(s.distanceMeters!!).isWithin(1.0).of(100.0)
        assertThat(s.compassAvailable).isFalse()
    }

    @Test fun `a fix too coarse to navigate by does not point the arrow straight ahead`() {
        val s = state(fix = southOfPin.copy(accuracyMeters = 80f), heading = 90.0)
        assertThat(s.arrowAngle).isNull()
        assertThat(s.distanceMeters).isNull()
        assertThat(s.waitingForFix).isTrue()
    }

    @Test fun `waits for a fix before the first one arrives`() {
        val s = state(fix = null)
        assertThat(s.waitingForFix).isTrue()
        assertThat(s.arrowAngle).isNull()
    }

    @Test fun `an uncalibrated compass is flagged`() {
        assertThat(state(accuracyClass = SensorManager.SENSOR_STATUS_UNRELIABLE).compassNeedsCalibration).isTrue()
        assertThat(state(accuracyClass = SensorManager.SENSOR_STATUS_ACCURACY_LOW).compassNeedsCalibration).isTrue()
        assertThat(state(accuracyClass = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM).compassNeedsCalibration).isFalse()
    }
}
