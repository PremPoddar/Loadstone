package com.prempoddar.loadstone.ui.components

import com.prempoddar.loadstone.ui.design.tap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.share.AppLinks
import com.prempoddar.loadstone.share.QrCode

/** A pin as a QR code, for someone standing next to you to scan. */
@Composable
fun QrDialog(pin: Pin, onDismiss: () -> Unit) {
    val image = remember(pin) {
        QrCode.bitmap(AppLinks.shareUrl(pin.latitude, pin.longitude, pin.name, pin.note)).asImageBitmap()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pin.name) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    image, "QR code for ${pin.name}", filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(Color.White).padding(8.dp),
                )
                Text(
                    "Scan it in Loadstone, or with any camera to open the pin.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = tap(onDismiss)) { Text("Done") } },
    )
}
