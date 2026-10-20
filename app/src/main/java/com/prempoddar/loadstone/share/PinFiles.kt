package com.prempoddar.loadstone.share

import com.prempoddar.loadstone.core.geo.Trail
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins as files: Loadstone's own backup list (everything, for restoring), and GPX and KML, which
 * map and hiking apps read (places as waypoints, recorded paths as tracks).
 */
object PinFiles {
    // ---------------------------------------------------------------- backup list

    /** Everything about each pin; photos and voice notes by file name, stored beside it in the backup. */
    fun toJson(pins: List<Pin>): String = JSONObject().apply {
        put("format", "loadstone-backup")
        put("version", 1)
        put("pins", JSONArray(pins.map { p ->
            JSONObject().apply {
                put("name", p.name); put("note", p.note ?: JSONObject.NULL)
                put("lat", p.latitude); put("lon", p.longitude); put("accuracy", p.accuracyMeters.toDouble())
                put("category", p.category.name); put("elevation", p.elevationMeters ?: JSONObject.NULL)
                put("pinned", p.isPinned); put("created", p.createdAt)
                put("trail", Trail.encode(p.trail))
                put("photos", JSONArray(p.photoPaths.map { java.io.File(it).name }))
                put("voice", JSONArray(p.voiceNotes.map { java.io.File(it).name }))
            }
        }))
    }.toString(2)

    /** Pins from a backup list; photo and voice entries are bare file names for the caller to place. */
    fun fromJson(text: String): List<Pin> {
        val root = JSONObject(text)
        require(root.optString("format") == "loadstone-backup") { "This isn't a Loadstone backup" }
        val arr = root.getJSONArray("pins")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Pin(
                name = o.getString("name"), note = o.optString("note").takeIf { !o.isNull("note") && it.isNotBlank() },
                latitude = o.getDouble("lat"), longitude = o.getDouble("lon"), accuracyMeters = o.getDouble("accuracy").toFloat(),
                category = runCatching { PinCategory.valueOf(o.getString("category")) }.getOrDefault(PinCategory.PIN),
                elevationMeters = if (o.isNull("elevation")) null else o.optDouble("elevation"),
                isPinned = o.optBoolean("pinned"), createdAt = o.optLong("created", System.currentTimeMillis()),
                trail = Trail.decode(o.optString("trail")),
                photoPaths = o.optJSONArray("photos").strings(), voiceNotes = o.optJSONArray("voice").strings(),
            )
        }
    }

    private fun JSONArray?.strings() = if (this == null) emptyList() else (0 until length()).map { getString(it) }

    /** True if [a] and [b] are the same saved pin, so restoring twice doesn't double them. */
    fun samePin(a: Pin, b: Pin) = a.createdAt == b.createdAt && a.name == b.name &&
        kotlin.math.abs(a.latitude - b.latitude) < 1e-6 && kotlin.math.abs(a.longitude - b.longitude) < 1e-6

    // ---------------------------------------------------------------- GPX and KML

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun iso(ms: Long) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))
    private fun num(d: Double) = "%.7f".format(Locale.ROOT, d)

    fun toGpx(pins: List<Pin>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"Loadstone\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        pins.forEach { p ->
            append("  <wpt lat=\"${num(p.latitude)}\" lon=\"${num(p.longitude)}\">")
            p.elevationMeters?.let { append("<ele>${"%.1f".format(Locale.ROOT, it)}</ele>") }
            append("<time>${iso(p.createdAt)}</time><name>${esc(p.name)}</name>")
            p.note?.let { append("<desc>${esc(it)}</desc>") }
            append("<type>${p.category.name.lowercase()}</type></wpt>\n")
        }
        pins.filter { it.trail.size >= 2 }.forEach { p ->
            append("  <trk><name>${esc("Path from " + p.name)}</name><trkseg>")
            p.trail.forEach { append("<trkpt lat=\"${num(it.latitude)}\" lon=\"${num(it.longitude)}\"/>") }
            append("</trkseg></trk>\n")
        }
        append("</gpx>\n")
    }

    fun toKml(pins: List<Pin>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>Loadstone pins</name>\n")
        pins.forEach { p ->
            append("  <Placemark><name>${esc(p.name)}</name>")
            p.note?.let { append("<description>${esc(it)}</description>") }
            append("<Point><coordinates>${num(p.longitude)},${num(p.latitude)}${p.elevationMeters?.let { "," + "%.1f".format(Locale.ROOT, it) } ?: ""}</coordinates></Point></Placemark>\n")
        }
        pins.filter { it.trail.size >= 2 }.forEach { p ->
            append("  <Placemark><name>${esc("Path from " + p.name)}</name><LineString><coordinates>")
            append(p.trail.joinToString(" ") { "${num(it.longitude)},${num(it.latitude)}" })
            append("</coordinates></LineString></Placemark>\n")
        }
        append("</Document></kml>\n")
    }

    /** A place read from another app's file, before it becomes a pin. */
    data class Place(val name: String, val note: String?, val latitude: Double, val longitude: Double, val elevation: Double?)

    /** Places from a GPX or KML file: waypoints and placemark points. Paths in them are skipped. */
    fun readPlaces(bytes: ByteArray): List<Place> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // Files from elsewhere: no outside entities or DTDs, which XML parsers otherwise fetch.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            isExpandEntityReferences = false
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        fun Element.child(tag: String) = getElementsByTagName(tag).item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
        val out = ArrayList<Place>()
        val wpts = doc.getElementsByTagName("wpt")
        for (i in 0 until wpts.length) {
            val e = wpts.item(i) as Element
            val lat = e.getAttribute("lat").toDoubleOrNull() ?: continue
            val lon = e.getAttribute("lon").toDoubleOrNull() ?: continue
            out += Place(e.child("name") ?: "Place ${out.size + 1}", e.child("desc") ?: e.child("cmt"), lat, lon, e.child("ele")?.toDoubleOrNull())
        }
        val marks = doc.getElementsByTagName("Placemark")
        for (i in 0 until marks.length) {
            val e = marks.item(i) as Element
            val point = e.getElementsByTagName("Point").item(0) as? Element ?: continue
            val parts = point.getElementsByTagName("coordinates").item(0)?.textContent?.trim()?.split(',') ?: continue
            val lon = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: continue
            val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: continue
            out += Place(e.child("name") ?: "Place ${out.size + 1}", e.child("description"), lat, lon, parts.getOrNull(2)?.trim()?.toDoubleOrNull())
        }
        return out.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
    }

}
