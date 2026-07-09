package com.prempoddar.loadstone.data.location

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Emits a smoothed, true-north-corrected compass heading from the rotation-vector
 * sensor. Declination is applied using the current [location] when available so
 * the arrow points at the real target rather than magnetic north.
 */
@Singleton
class CompassRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun headings(location: Location?): Flow<Heading> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            channel.close()
            return@callbackFlow
        }

        val declination = location?.let {
            GeomagneticField(
                it.latitude.toFloat(),
                it.longitude.toFloat(),
                it.altitude.toFloat(),
                System.currentTimeMillis(),
            ).declination
        } ?: 0f

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        // Circular exponential smoothing on the unit vector to avoid 359->0 jumps.
        var smoothSin = 0.0
        var smoothCos = 0.0
        var initialized = false
        val alpha = 0.15

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthRad = orientation[0].toDouble() // radians, -PI..PI, magnetic
                val s = sin(azimuthRad)
                val c = cos(azimuthRad)
                if (!initialized) {
                    smoothSin = s; smoothCos = c; initialized = true
                } else {
                    smoothSin += alpha * (s - smoothSin)
                    smoothCos += alpha * (c - smoothCos)
                }
                val magneticDeg = Math.toDegrees(atan2(smoothSin, smoothCos))
                val trueDeg = ((magneticDeg + declination) % 360 + 360) % 360
                trySend(Heading(trueDeg.toFloat(), currentAccuracy))
            }

            var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                currentAccuracy = accuracy
            }
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
