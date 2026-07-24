package com.prempoddar.loadstone.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Small, consistent haptic vocabulary used across the app. */
object Haptics {
    /** Light tick for taps / navigation. */
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Subtle tick for selections (chips, toggles). */
    fun select(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Positive confirmation for saves / completed actions. */
    fun confirm(view: View) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    /** Stronger cue for arrival. */
    fun arrive(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
