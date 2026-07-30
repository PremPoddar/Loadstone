package com.prempoddar.loadstone.share

import android.net.Uri
import java.net.URLEncoder

/**
 * Builds and parses the links used to share a pin.
 *
 * Two forms are understood:
 * - **https** — `https://prempoddar.github.io/Loadstone/open?…`, what actually gets shared, because
 *   plain text links are only tappable in other apps if they are http(s). The site is served by
 *   GitHub Pages from this repo's `docs/` folder, so the path carries the repo name's capital L.
 * - **loadstone://open?…** — the custom scheme. The landing page's "Open in Loadstone" button uses
 *   it, which opens the app directly even though Android cannot verify the https link (verification
 *   requires `/.well-known/assetlinks.json` at the domain root, which a project Pages site does not
 *   serve). See `docs/README.md`.
 *
 * [HOST] and [PATH] must stay in step with the `<intent-filter>` entries in `AndroidManifest.xml`.
 */
object AppLinks {
    const val HOST = "prempoddar.github.io"
    const val PATH = "/Loadstone/open"
    const val SCHEME = "loadstone"

    fun shareUrl(lat: Double, lon: Double, name: String, note: String?): String =
        "https://$HOST$PATH?${query(lat, lon, name, note)}"

    fun privacyUrl() = "https://$HOST/Loadstone/privacy"

    private fun query(lat: Double, lon: Double, name: String, note: String?): String {
        val n = URLEncoder.encode(name, "UTF-8")
        val d = URLEncoder.encode(note.orEmpty(), "UTF-8")
        return "lat=$lat&lng=$lon&n=$n&d=$d"
    }

    data class Parsed(val lat: Double, val lon: Double, val name: String?, val note: String?)

    /** Parses a shared pin from either link form, or null if this is not a Loadstone link. */
    fun parse(uri: Uri): Parsed? {
        val recognised = when {
            uri.scheme.equals(SCHEME, ignoreCase = true) -> true
            uri.host.equals(HOST, ignoreCase = true) -> true
            else -> false
        }
        if (!recognised) return null

        val lat = uri.getQueryParameter("lat")?.toDoubleOrNull() ?: return null
        val lon = uri.getQueryParameter("lng")?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return Parsed(
            lat = lat,
            lon = lon,
            name = uri.getQueryParameter("n")?.takeIf { it.isNotBlank() },
            note = uri.getQueryParameter("d")?.takeIf { it.isNotBlank() },
        )
    }
}
