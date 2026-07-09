package com.prempoddar.loadstone.core.format

import com.prempoddar.loadstone.data.settings.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

/** Formats a distance in meters into a short, human-readable string. */
object UnitFormatter {
    fun formatDistance(meters: Double, system: UnitSystem): String = when (system) {
        UnitSystem.METRIC ->
            if (meters < 1000) "${meters.roundToInt()} m"
            else String.format(Locale.US, "%.1f km", meters / 1000.0)

        UnitSystem.IMPERIAL -> {
            val feet = meters * 3.28084
            if (feet < 5280) "${feet.roundToInt()} ft"
            else String.format(Locale.US, "%.1f mi", feet / 5280.0)
        }
    }

    /** Elevation is always shown in plain meters or feet (never km/mi). */
    fun formatElevation(meters: Double, system: UnitSystem): String = when (system) {
        UnitSystem.METRIC -> "${meters.roundToInt()} m"
        UnitSystem.IMPERIAL -> "${(meters * 3.28084).roundToInt()} ft"
    }
}
