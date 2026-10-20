package com.prempoddar.loadstone.share

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** QR codes for pins: the same link "Share" sends, so any camera opens it too. */
object QrCode {
    fun bitmap(text: String, size: Int = 720): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
        val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /** A pin from a scanned code, or null if it isn't a Loadstone link. */
    fun read(raw: String?): AppLinks.Parsed? = raw?.let { runCatching { AppLinks.parse(Uri.parse(it.trim())) }.getOrNull() }
}
