package com.prempoddar.loadstone.guide

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.prempoddar.loadstone.MainActivity
import com.prempoddar.loadstone.R
import com.prempoddar.loadstone.core.format.UnitFormatter
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.core.geo.Trail
import com.prempoddar.loadstone.core.geo.TrailPoint
import com.prempoddar.loadstone.core.geo.TrailRecorder
import com.prempoddar.loadstone.data.location.LocationRepository
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinRepository
import com.prempoddar.loadstone.data.settings.SettingsRepository
import com.prempoddar.loadstone.data.settings.UnitSystem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

/**
 * Keeps working with the screen off, shown as a notification, for one pin at a time:
 *
 * - **Guide**: the notification says how far the pin is and which way, and the phone buzzes when
 *   you arrive. With a recorded path, the distance is along the path.
 * - **Record**: the path you walk away from the pin is saved with it, so it can be retraced.
 *
 * Both run on the same estimates the screens use. Started only from the app on screen, so it
 * needs no background location permission.
 */
@AndroidEntryPoint
class GuideService : Service() {
    @Inject lateinit var locations: LocationRepository
    @Inject lateinit var pins: PinRepository
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    /** The path being recorded, if any, and its pin. */
    private var recording: Pair<Long, TrailRecorder>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pinId = intent?.getLongExtra(EXTRA_PIN, -1L) ?: -1L
        when (intent?.action) {
            ACTION_STOP -> { finish(); return START_NOT_STICKY }
            ACTION_GUIDE, ACTION_RECORD -> if (pinId > 0) begin(pinId, intent.action == ACTION_RECORD)
            else -> { finish(); return START_NOT_STICKY }
        }
        return START_NOT_STICKY
    }

    private fun begin(pinId: Long, record: Boolean) {
        ensureChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification("Starting…", null),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        _active.value = Active(pinId, guiding = !record, recording = record)
        // Switching from recording to guiding (or to another pin) keeps the path recorded so far.
        recording?.let { (id, r) -> val t = r.trail.toList(); CoroutineScope(Dispatchers.IO).launch { store(id, t) } }
        recording = null
        job?.cancel()
        job = scope.launch {
            val pin = pins.get(pinId) ?: run { finish(); return@launch }
            val unit = settings.settings.first().unitSystem
            if (record) record(pin, unit) else guide(pin, unit)
        }
    }

    private suspend fun record(pin: Pin, unit: UnitSystem) {
        // A new recording starts at the pin and replaces any earlier path.
        val recorder = TrailRecorder(listOf(TrailPoint(pin.latitude, pin.longitude)))
        recording = pin.id to recorder
        var saved = 0
        locations.estimatedFixes().catch { }.collect { fix ->
            if (recorder.add(fix) && recorder.trail.size - saved >= SAVE_EVERY) {
                saved = recorder.trail.size
                store(pin.id, recorder.trail)
            }
            val length = Trail.length(recorder.trail)
            update("Recording your way from ${pin.name}", "${UnitFormatter.formatDistance(length, unit)} recorded · tap Stop when you're done")
        }
    }

    private suspend fun store(pinId: Long, trail: List<TrailPoint>) {
        pins.get(pinId)?.let { pins.upsert(it.copy(trail = Trail.simplify(trail))) }
    }

    private suspend fun guide(pin: Pin, unit: UnitSystem) {
        locations.estimatedFixes().catch { }.collect { fix ->
            val straight = GeoMath.distanceMeters(fix.latitude, fix.longitude, pin.latitude, pin.longitude)
            val path = Trail.guide(pin.trail, fix.latitude, fix.longitude)
            val toward = path?.target ?: TrailPoint(pin.latitude, pin.longitude)
            val bearing = GeoMath.initialBearingDegrees(fix.latitude, fix.longitude, toward.latitude, toward.longitude)
            val arrived = straight <= max(ARRIVE_METERS, pin.accuracyMeters.toDouble()) && fix.estAccuracyMeters <= 20f
            if (arrived) {
                buzz()
                update("You're at ${pin.name}", "Look around: it's within ${UnitFormatter.formatDistance(max(ARRIVE_METERS, pin.accuracyMeters.toDouble()), unit)}")
                _active.value = null
                job?.cancel()
                stopForegroundKeepingNote()
                return@collect
            }
            val left = path?.remainingMeters ?: straight
            update(
                "${pin.name} · ${UnitFormatter.formatDistance(left, unit)}" + if (path != null) " along your path" else "",
                "Head ${compassPoint(bearing)}" + if (fix.estAccuracyMeters > 15f) " · GPS is rough here (±${fix.estAccuracyMeters.toInt()} m)" else "",
            )
        }
    }

    private fun buzz() {
        val v = ContextCompat.getSystemService(this, Vibrator::class.java) ?: return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120, 80, 300), -1))
    }

    private fun finish() {
        job?.cancel()
        _active.value = null
        // Keep everything walked, including the stretch since the last save.
        recording?.let { (pinId, recorder) ->
            val trail = recorder.trail.toList()
            CoroutineScope(Dispatchers.IO).launch { store(pinId, trail) }
        }
        recording = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Arrived: the note stays so you can see it, but nothing runs any more. */
    private fun stopForegroundKeepingNote() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        _active.value = null
        super.onDestroy()
    }

    private fun update(title: String, text: String?) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(title, text))
    }

    private fun notification(title: String, text: String?): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, GuideService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_guide)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(_active.value != null)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .apply { if (_active.value != null) addAction(0, "Stop", stop) }
            .build()
    }

    private fun ensureChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Guiding and recording", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Distance and direction to a pin with the screen off, and path recording"
            },
        )
    }

    /** What's running, for the screens to show. */
    data class Active(val pinId: Long, val guiding: Boolean, val recording: Boolean)

    companion object {
        private const val CHANNEL = "guide"
        private const val NOTIFICATION_ID = 7
        private const val EXTRA_PIN = "pin"
        private const val ACTION_GUIDE = "guide"
        private const val ACTION_RECORD = "record"
        private const val ACTION_STOP = "stop"
        private const val SAVE_EVERY = 6
        private const val ARRIVE_METERS = 6.0

        private val _active = MutableStateFlow<Active?>(null)
        val active: StateFlow<Active?> = _active.asStateFlow()

        fun guide(context: Context, pinId: Long) = start(context, ACTION_GUIDE, pinId)
        fun record(context: Context, pinId: Long) = start(context, ACTION_RECORD, pinId)
        fun stop(context: Context) {
            context.startService(Intent(context, GuideService::class.java).setAction(ACTION_STOP))
        }

        private fun start(context: Context, action: String, pinId: Long) {
            ContextCompat.startForegroundService(context, Intent(context, GuideService::class.java).setAction(action).putExtra(EXTRA_PIN, pinId))
        }

        /** "north-east" and so on, for a bearing; the notification can't show an arrow that turns with you. */
        fun compassPoint(bearing: Double): String {
            val names = listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")
            return names[(((GeoMath.normalize360(bearing) + 22.5) / 45).toInt()) % 8]
        }
    }
}
