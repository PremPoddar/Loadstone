package com.prempoddar.loadstone.data.location

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * An accuracy log for testing: every raw fix and the estimate it led to, one line each, so a walk
 * can be replayed afterwards to see exactly where the estimate went wrong. Off unless switched on
 * in Settings; it stays on the phone until shared.
 */
object FixLog {
    @Volatile var enabled = false

    fun file(context: Context) = File(File(context.filesDir, "logs").apply { mkdirs() }, "accuracy-log.csv")

    @Synchronized fun write(context: Context, line: String) {
        if (!enabled) return
        val f = file(context)
        if (!f.exists()) f.writeText("elapsed_ms,kind,lat,lon,accuracy_m,speed_mps,altitude_m,est_accuracy_m,settled,moving\n")
        f.appendText(line + "\n")
    }

    fun raw(context: Context, t: Long, lat: Double, lon: Double, acc: Float, speed: Float?, alt: Double?) =
        write(context, String.format(Locale.ROOT, "%d,raw,%.7f,%.7f,%.1f,%s,%s,,,", t, lat, lon, acc, speed?.let { "%.2f".format(Locale.ROOT, it) } ?: "", alt?.let { "%.1f".format(Locale.ROOT, it) } ?: ""))

    fun estimate(context: Context, t: Long, fix: com.prempoddar.loadstone.core.geo.PinFix) =
        write(context, String.format(Locale.ROOT, "%d,estimate,%.7f,%.7f,,,%s,%.1f,%b,%b", t, fix.latitude, fix.longitude, fix.altitudeMeters?.let { "%.1f".format(Locale.ROOT, it) } ?: "", fix.estAccuracyMeters, fix.settled, fix.moving))

    /** A labelled moment, such as "saved" or "arrived", to find in the log. */
    fun mark(context: Context, label: String) = write(context, "${android.os.SystemClock.elapsedRealtime()},mark:$label,,,,,,,,")

    fun clear(context: Context) { file(context).delete() }
}
