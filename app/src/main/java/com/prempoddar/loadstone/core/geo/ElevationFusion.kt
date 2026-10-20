package com.prempoddar.loadstone.core.geo

import kotlin.math.pow

/**
 * A live elevation that is both steady and right.
 *
 * GPS height wanders by several metres from one second to the next, so on its own it can't show
 * you climbing a flight of stairs. A barometer can: air pressure changes smoothly with height and
 * resolves a metre or less. But pressure also drifts with the weather, so it can't say how high
 * you are, only how much you have moved. Each covers the other's weakness: the barometer carries
 * every change from moment to moment, and GPS slowly pulls the total back to the true height.
 * Without a barometer, GPS readings are simply smoothed.
 */
class ElevationFusion {
    var meters: Double? = null
        private set
    private var lastBaro: Double? = null
    private var hasBarometer = false

    fun onPressure(hPa: Float) {
        hasBarometer = true
        val alt = pressureAltitude(hPa)
        val last = lastBaro
        lastBaro = alt
        val current = meters ?: return
        if (last != null) meters = current + (alt - last)
    }

    /** A sea-level GPS height and its one-sigma accuracy, if known. */
    fun onGps(altitude: Double, accuracy: Float? = null) {
        val current = meters
        if (current == null) { meters = altitude; return }
        // Trust a precise reading more; with a barometer, GPS only nudges the level.
        val base = if (hasBarometer) 0.03 else 0.2
        val gain = if (accuracy != null && accuracy > 0f) (base * (5.0 / accuracy).coerceIn(0.3, 2.0)).coerceAtMost(0.5) else base
        meters = current + gain * (altitude - current)
    }

    companion object {
        /** Height in the standard atmosphere for a pressure; only its changes are used. */
        fun pressureAltitude(hPa: Float): Double = 44_330.0 * (1.0 - (hPa / 1013.25).toDouble().pow(1.0 / 5.255))
    }
}
