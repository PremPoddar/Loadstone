package com.prempoddar.loadstone.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DROP = "drop"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val ABOUT = "about"
    const val RADAR = "radar"

    const val NAVIGATE_ARG_PIN_ID = "pinId"
    const val NAVIGATE = "navigate/{$NAVIGATE_ARG_PIN_ID}"
    fun navigate(pinId: Long) = "navigate/$pinId"
}
