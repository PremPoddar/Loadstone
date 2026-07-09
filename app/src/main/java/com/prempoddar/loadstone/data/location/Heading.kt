package com.prempoddar.loadstone.data.location

/**
 * A smoothed compass heading.
 *
 * @param trueNorthDegrees where the top of the phone points, 0–360, relative to
 *   true north (magnetic declination already applied when a location is known).
 * @param accuracyClass one of the SensorManager.SENSOR_STATUS_ACCURACY_* values;
 *   low values mean the user should figure-8 calibrate.
 */
data class Heading(
    val trueNorthDegrees: Float,
    val accuracyClass: Int,
)
