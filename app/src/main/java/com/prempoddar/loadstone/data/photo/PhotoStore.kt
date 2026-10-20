package com.prempoddar.loadstone.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Creates app-scoped image files and content URIs for the camera. No permission needed. */
object PhotoStore {
    fun dir(context: Context): File =
        File(context.getExternalFilesDir(null), "pin_photos").apply { mkdirs() }

    /** A fresh, empty JPEG file the camera can write into. */
    fun newPhotoFile(context: Context): File =
        File(dir(context), "pin_${System.currentTimeMillis()}.jpg")

    /** A FileProvider content URI granting the camera app write access to [file]. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Copies pictures chosen from the gallery into the app, shrunk to at most 2048 pixels across
     * (plenty for a pin, and quick to show). Returns the new files' paths; ones that fail are skipped.
     */
    fun importAll(context: Context, uris: List<Uri>): List<String> = uris.mapIndexedNotNull { i, uri ->
        runCatching {
            val file = File(dir(context), "pin_${System.currentTimeMillis()}_$i.jpg")
            if (android.os.Build.VERSION.SDK_INT < 28) {
                // Older Android can't shrink and keep the photo's turn in one step: keep it as it is.
                context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
                return@runCatching file.absolutePath
            }
            val bitmap = android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
                d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val scale = maxOf(info.size.width, info.size.height) / 2048f
                if (scale > 1) d.setTargetSize((info.size.width / scale).toInt(), (info.size.height / scale).toInt())
            }
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            file.absolutePath
        }.getOrNull()
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
