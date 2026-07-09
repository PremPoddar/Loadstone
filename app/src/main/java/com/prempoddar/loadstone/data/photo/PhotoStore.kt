package com.prempoddar.loadstone.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Creates app-scoped image files and content URIs for the camera. No permission needed. */
object PhotoStore {
    private fun dir(context: Context): File =
        File(context.getExternalFilesDir(null), "pin_photos").apply { mkdirs() }

    /** A fresh, empty JPEG file the camera can write into. */
    fun newPhotoFile(context: Context): File =
        File(dir(context), "pin_${System.currentTimeMillis()}.jpg")

    /** A FileProvider content URI granting the camera app write access to [file]. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
