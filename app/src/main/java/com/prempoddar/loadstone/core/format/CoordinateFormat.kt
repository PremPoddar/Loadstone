package com.prempoddar.loadstone.core.format

import java.util.Locale

/** Formats and parses "lat, lon" coordinate strings for sharing / importing pins. */
object CoordinateFormat {
    fun format(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.5f, %.5f", lat, lon)

    /**
     * Parses a coordinate pair from free text: plain "lat, lon", a "geo:" URI, or
     * text containing such a pair. Returns null if no valid pair is found.
     */
    fun parse(text: String): Pair<Double, Double>? {
        val cleaned = text.trim().removePrefix("geo:").substringBefore("?")
        val match = Regex("(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)").find(cleaned) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }
}
