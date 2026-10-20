package com.prempoddar.loadstone.core.geo

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Sunrise and sunset for a place and day, worked out on the phone (the NOAA solar equations,
 * good to a minute or so away from the poles), for warning that daylight is running out.
 */
object Sun {
    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI

    /** Sunset on the UTC day containing [nearMs], in epoch ms; null where the sun doesn't set or rise that day. */
    fun sunsetMs(latitude: Double, longitude: Double, nearMs: Long): Long? = event(latitude, longitude, nearMs, rising = false)

    fun sunriseMs(latitude: Double, longitude: Double, nearMs: Long): Long? = event(latitude, longitude, nearMs, rising = true)

    private fun event(lat: Double, lon: Double, nearMs: Long, rising: Boolean): Long? {
        // Whole days since J2000.0: the Julian day at noon, which is when the count turns over.
        val day = floor(nearMs / 86_400_000.0) + 2440588.0 - 2451545.0 + 0.0008
        val noon = day - lon / 360
        val anomaly = (357.5291 + 0.98560028 * noon) % 360
        val m = rad(anomaly)
        val centre = 1.9148 * sin(m) + 0.0200 * sin(2 * m) + 0.0003 * sin(3 * m)
        val ecliptic = rad((anomaly + centre + 180 + 102.9372) % 360)
        val transit = 2451545.0 + noon + 0.0053 * sin(m) - 0.0069 * sin(2 * ecliptic)
        val declination = asin(sin(ecliptic) * sin(rad(23.4397)))
        // The sun's centre 0.833° below the horizon: refraction plus its radius.
        val cosHour = (sin(rad(-0.833)) - sin(rad(lat)) * sin(declination)) / (cos(rad(lat)) * cos(declination))
        if (cosHour < -1 || cosHour > 1) return null
        val hour = deg(acos(cosHour))
        val julian = if (rising) transit - hour / 360 else transit + hour / 360
        return ((julian - 2440587.5) * 86_400_000).toLong()
    }

    /** Typical walking pace for a return estimate. */
    const val WALK_MPS = 1.3

    /**
     * Whether to warn: the walk left would end after sunset (or within 15 minutes of it). Null
     * when there's nothing to say; otherwise minutes of daylight left and minutes the walk takes.
     */
    fun warning(latitude: Double, longitude: Double, distanceMeters: Double, nowMs: Long): Pair<Long, Long>? {
        if (distanceMeters < 300) return null
        val sunset = sunsetMs(latitude, longitude, nowMs) ?: return null
        val sunrise = sunriseMs(latitude, longitude, nowMs)
        if (nowMs > sunset || (sunrise != null && nowMs < sunrise)) return null // already dark: a torch, not a warning
        val daylight = (sunset - nowMs) / 60_000
        val walk = (distanceMeters / WALK_MPS / 60).toLong()
        return if (walk + 15 >= daylight) daylight to walk else null
    }

}
