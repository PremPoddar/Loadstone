package com.prempoddar.loadstone.data.location

/**
 * A smoothed compass heading.
 *
 * @param magneticDegrees the direction the user faces while looking at the screen, 0–360,
 *   relative to magnetic north. Add [CompassRepository.declinationDegrees] for true north.
 * @param accuracyClass one of the SensorManager.SENSOR_STATUS_* values;
 *   low values mean the user should figure-8 calibrate.
 */
data class Heading(
    val magneticDegrees: Float,
    val accuracyClass: Int,
)
