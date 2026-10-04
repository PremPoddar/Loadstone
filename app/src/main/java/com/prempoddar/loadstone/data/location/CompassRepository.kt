package com.prempoddar.loadstone.data.location

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Surface
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.core.geo.HeadingFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits a smoothed heading of the direction the user faces while looking at the screen, from the
 * rotation-vector sensor. Smoothing is [HeadingFilter]'s job; see there for why a single fixed
 * strength made the arrow either shiver or lag. It is relative to magnetic north and needs no location, so the compass
 * is live as soon as the screen opens; add [declinationDegrees] for true north.
 */
@Singleton
class CompassRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager: SensorManager
        get() = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** The gyro-fused rotation vector, or on phones without a gyro the magnetometer-only one. */
    private fun rotationSensor(): Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    /** False on phones with no magnetometer, which cannot point an arrow at all. */
    fun hasCompass(): Boolean = rotationSensor() != null

    fun headings(): Flow<Heading> = callbackFlow {
        val sensor = rotationSensor()
        if (sensor == null) {
            channel.close()
            return@callbackFlow
        }
        val mainHandler = Handler(Looper.getMainLooper())

        // The arrow is drawn relative to the top of the screen, which is a different edge of the
        // device in landscape. Track it, including 180° flips that don't recreate the activity.
        val displayManager = context.getSystemService(DisplayManager::class.java)
        fun readRotation() =
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        var displayRotation = readRotation()
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) displayRotation = readRotation()
            }
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
        }

        val rotationMatrix = FloatArray(9)
        val filter = HeadingFilter()

        val listener = object : SensorEventListener {
            var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                // Phone on its side with rotation locked: nothing is "ahead", so hold the last heading.
                val facing = GeoMath.facingHeadingDegrees(rotationMatrix, displayRotation) ?: return
                trySend(Heading(filter.update(facing).toFloat(), currentAccuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                currentAccuracy = accuracy
            }
        }

        displayManager.registerDisplayListener(displayListener, mainHandler)
        // The game-rate stream (~50 Hz) costs little and gives the filter far more to work with:
        // noise averages away over many small steps instead of being locked in by each slow one,
        // so the arrow is both steadier at rest and quicker into a turn.
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME, mainHandler)
        awaitClose {
            sensorManager.unregisterListener(listener)
            displayManager.unregisterDisplayListener(displayListener)
        }
    }.conflate() // a busy collector gets the latest heading, never a backlog of stale ones

    companion object {
        /** Degrees to add to a magnetic heading at this position to get a true-north one. */
        fun declinationDegrees(latitude: Double, longitude: Double, altitudeMeters: Double): Float =
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitudeMeters.toFloat(),
                System.currentTimeMillis(),
            ).declination
    }
}
