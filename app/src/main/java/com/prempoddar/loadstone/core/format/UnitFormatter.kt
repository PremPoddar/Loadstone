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

    /** Splits a distance into (number, unit) for large split-typography display. */
    fun distanceParts(meters: Double, system: UnitSystem): Pair<String, String> = when (system) {
        UnitSystem.METRIC ->
            if (meters < 1000) "${meters.roundToInt()}" to "m"
            else String.format(Locale.US, "%.1f", meters / 1000.0) to "km"

        UnitSystem.IMPERIAL -> {
            val feet = meters * 3.28084
            if (feet < 5280) "${feet.roundToInt()}" to "ft"
            else String.format(Locale.US, "%.1f", feet / 5280.0) to "mi"
        }
    }

    /** Elevation is always shown in plain meters or feet (never km/mi). */
    fun formatElevation(meters: Double, system: UnitSystem): String = when (system) {
        UnitSystem.METRIC -> "${meters.roundToInt()} m"
        UnitSystem.IMPERIAL -> "${(meters * 3.28084).roundToInt()} ft"
    }
}
