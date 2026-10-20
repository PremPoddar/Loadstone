package com.prempoddar.loadstone.data.pin

import android.content.Context
import com.prempoddar.loadstone.data.photo.PhotoStore
import com.prempoddar.loadstone.share.PinFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backing up and moving pins: one zip with every pin and its photos and voice notes, for a new
 * phone or after reinstalling; and GPX or KML for other map apps.
 */
@Singleton
class PinBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pins: PinRepository,
) {
    private val voiceDir get() = File(context.filesDir, "voice").apply { mkdirs() }

    /** Writes everything to [out]; returns how many pins. */
    suspend fun backup(out: OutputStream): Int = withContext(Dispatchers.IO) {
        val all = pins.all()
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(LIST)); zip.write(PinFiles.toJson(all).toByteArray()); zip.closeEntry()
            fun add(folder: String, path: String) {
                val f = File(path).takeIf { it.isFile } ?: return
                zip.putNextEntry(ZipEntry("$folder/${f.name}")); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
            all.forEach { p -> p.photoPaths.forEach { add("photos", it) }; p.voiceNotes.forEach { add("voice", it) } }
        }
        all.size
    }

    data class Restored(val added: Int, val alreadyHere: Int)

    /** Reads a backup; pins already on the phone are left as they are. */
    suspend fun restore(input: InputStream): Restored = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var list: String? = null
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                when {
                    e.name == LIST -> list = zip.readBytes().toString(Charsets.UTF_8)
                    // Only plain file names in the two known folders: nothing in a backup can
                    // write anywhere else on the phone.
                    !e.isDirectory && (e.name.startsWith("photos/") || e.name.startsWith("voice/")) -> {
                        val folder = e.name.substringBefore('/')
                        val name = File(e.name).name.takeIf { it.isNotBlank() && !it.startsWith(".") } ?: continue
                        File(staging, folder).apply { mkdirs() }.resolve(name).outputStream().use { zip.copyTo(it) }
                    }
                }
            }
        }
        val incoming = PinFiles.fromJson(list ?: error("This isn't a Loadstone backup"))
        val existing = pins.all()
        var added = 0; var skipped = 0
        for (p in incoming) {
            if (existing.any { PinFiles.samePin(it, p) }) { skipped++; continue }
            fun place(folder: String, into: File, name: String): String? {
                val from = File(staging, "$folder/$name").takeIf { it.isFile } ?: return null
                val to = File(into, "r${System.nanoTime()}_$name")
                from.copyTo(to, overwrite = true)
                return to.absolutePath
            }
            pins.upsert(
                p.copy(
                    id = 0,
                    photoPaths = p.photoPaths.mapNotNull { place("photos", PhotoStore.dir(context), it) },
                    voiceNotes = p.voiceNotes.mapNotNull { place("voice", voiceDir, it) },
                ),
            )
            added++
        }
        staging.deleteRecursively()
        Restored(added, skipped)
    }

    suspend fun export(out: OutputStream, kml: Boolean): Int = withContext(Dispatchers.IO) {
        val all = pins.all()
        out.bufferedWriter().use { it.write(if (kml) PinFiles.toKml(all) else PinFiles.toGpx(all)) }
        all.size
    }

    /** Adds the places in another app's GPX or KML file as pins; returns how many. */
    suspend fun import(input: InputStream): Int = withContext(Dispatchers.IO) {
        val places = PinFiles.readPlaces(input.readBytes())
        places.forEach { pl ->
            pins.upsert(
                Pin(
                    name = pl.name.take(80), note = pl.note?.take(500), latitude = pl.latitude, longitude = pl.longitude,
                    // How well another app measured it is unknown; start modest and let a visit sharpen it.
                    accuracyMeters = IMPORTED_ACCURACY, elevationMeters = pl.elevation,
                ),
            )
        }
        places.size
    }

    companion object {
        private const val LIST = "pins.json"
        const val IMPORTED_ACCURACY = 10f
    }
}
