package com.prempoddar.loadstone.share

import android.net.Uri
import java.net.URLEncoder

/**
 * Builds and parses shareable https links that open Loadstone via verified App Links.
 *
 * The [HOST] must match the domain hosting `/.well-known/assetlinks.json` and the
 * `<intent-filter android:autoVerify="true">` in the manifest. If your GitHub Pages
 * username is not "prempoddar", change [HOST] here AND the manifest host to match.
 */
object AppLinks {
    const val HOST = "prempoddar.github.io"
    const val PATH = "/loadstone/open"

    fun privacyUrl() = "https://$HOST/loadstone/privacy"

    fun shareUrl(lat: Double, lon: Double, name: String, note: String?): String {
        val n = URLEncoder.encode(name, "UTF-8")
        val d = URLEncoder.encode(note.orEmpty(), "UTF-8")
        return "https://$HOST$PATH?lat=$lat&lng=$lon&n=$n&d=$d"
    }

    data class Parsed(val lat: Double, val lon: Double, val name: String?, val note: String?)

    /** Parses coordinates (and optional name/note) from an incoming Loadstone link. */
    fun parse(uri: Uri): Parsed? {
        if (!uri.host.equals(HOST, ignoreCase = true)) return null
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
