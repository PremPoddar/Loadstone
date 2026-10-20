package com.prempoddar.loadstone.share

import com.google.common.truth.Truth.assertThat
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrCodeTest {
    @Test fun aPinsCodeScansBackToThePin() {
        val link = AppLinks.shareUrl(19.07609, 72.877426, "Car park, level 2", "Pillar C4")
        val bmp = QrCode.bitmap(link, 400)
        val px = IntArray(400 * 400).also { bmp.getPixels(it, 0, 400, 0, 0, 400, 400) }
        val text = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(400, 400, px)))).text
        val pin = QrCode.read(text)!!
        assertThat(pin.lat).isWithin(1e-6).of(19.07609)
        assertThat(pin.lon).isWithin(1e-6).of(72.877426)
        assertThat(pin.name).isEqualTo("Car park, level 2")
        assertThat(pin.note).isEqualTo("Pillar C4")
    }

    @Test fun otherCodesAreNotPins() {
        assertThat(QrCode.read("https://example.com/menu")).isNull()
        assertThat(QrCode.read("just some text")).isNull()
        assertThat(QrCode.read(null)).isNull()
    }
}
