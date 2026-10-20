package com.prempoddar.loadstone.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.prempoddar.loadstone.MainActivity
import com.prempoddar.loadstone.R
import com.prempoddar.loadstone.core.format.UnitFormatter
import com.prempoddar.loadstone.core.geo.GeoMath
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.settings.UnitSystem
import com.prempoddar.loadstone.guide.GuideService

/**
 * The pinned spot (or the newest) on the home screen, with how far it was when Loadstone last
 * knew where you were; tap to find it. Android doesn't let a widget follow you without "all the
 * time" location, so the distance is refreshed whenever the app is used.
 */
class PinWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val (pin, here, unit) = Snapshot.read(context)
        ids.forEach { manager.updateAppWidget(it, views(context, pin, here, unit)) }
    }

    companion object {
        /** Redraws every Loadstone widget with [pin], seen from [here] if known. */
        fun update(context: Context, pin: Pin?, here: Pair<Double, Double>?, unit: UnitSystem) {
            Snapshot.write(context, pin, here, unit)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PinWidget::class.java))
            if (ids.isNotEmpty()) ids.forEach { manager.updateAppWidget(it, views(context, Snapshot.read(context).first, here, unit)) }
        }

        private fun views(context: Context, pin: Snapshot.SavedPin?, here: Pair<Double, Double>?, unit: UnitSystem) =
            RemoteViews(context.packageName, R.layout.widget_pin).apply {
                if (pin == null) {
                    setTextViewText(R.id.widget_name, "No pins yet")
                    setTextViewText(R.id.widget_detail, "Tap to drop your first one")
                    setOnClickPendingIntent(R.id.widget_root, open(context, Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_DROP)))
                } else {
                    setTextViewText(R.id.widget_name, pin.name)
                    val detail = here?.let { (lat, lon) ->
                        val d = GeoMath.distanceMeters(lat, lon, pin.latitude, pin.longitude)
                        val b = GeoMath.initialBearingDegrees(lat, lon, pin.latitude, pin.longitude)
                        "${UnitFormatter.formatDistance(d, unit)} ${GuideService.compassPoint(b)} · tap to find"
                    } ?: "Tap to find it"
                    setTextViewText(R.id.widget_detail, detail)
                    setOnClickPendingIntent(
                        R.id.widget_root,
                        open(context, Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_FIND).putExtra(MainActivity.EXTRA_PIN, pin.id)),
                    )
                }
            }

        private fun open(context: Context, intent: Intent) = PendingIntent.getActivity(
            context, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** What the widget shows, kept so it can draw without the database. */
internal object Snapshot {
    data class SavedPin(val id: Long, val name: String, val latitude: Double, val longitude: Double)

    private fun prefs(c: Context) = c.getSharedPreferences("widget", Context.MODE_PRIVATE)

    fun write(c: Context, pin: Pin?, here: Pair<Double, Double>?, unit: UnitSystem) {
        prefs(c).edit().apply {
            if (pin == null) remove("id") else {
                putLong("id", pin.id); putString("name", pin.name)
                putString("lat", pin.latitude.toString()); putString("lon", pin.longitude.toString())
            }
            here?.let { putString("hlat", it.first.toString()); putString("hlon", it.second.toString()) }
            putString("unit", unit.name)
        }.apply()
    }

    fun read(c: Context): Triple<SavedPin?, Pair<Double, Double>?, UnitSystem> {
        val p = prefs(c)
        val pin = if (p.contains("id")) SavedPin(p.getLong("id", 0), p.getString("name", "").orEmpty(), p.getString("lat", "0")!!.toDouble(), p.getString("lon", "0")!!.toDouble()) else null
        val here = p.getString("hlat", null)?.toDoubleOrNull()?.let { lat -> p.getString("hlon", null)?.toDoubleOrNull()?.let { lat to it } }
        val unit = runCatching { UnitSystem.valueOf(p.getString("unit", "METRIC")!!) }.getOrDefault(UnitSystem.METRIC)
        return Triple(pin, here, unit)
    }
}
