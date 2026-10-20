package com.prempoddar.loadstone.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.prempoddar.loadstone.MainActivity
import com.prempoddar.loadstone.R
import com.prempoddar.loadstone.data.pin.Pin

/** Long-press the app icon: "Find <pin>" for the pins opened most recently. */
object PinShortcuts {
    fun opened(context: Context, pin: Pin) {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_FIND)
            .putExtra(MainActivity.EXTRA_PIN, pin.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val shortcut = ShortcutInfoCompat.Builder(context, "pin-${pin.id}")
            .setShortLabel(pin.name.take(24))
            .setLongLabel("Find ${pin.name}".take(40))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_find))
            .setIntent(intent)
            .build()
        // The launcher keeps the most recent few and drops the oldest.
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
    }

    fun removed(context: Context, pinId: Long) {
        runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf("pin-$pinId")) }
    }
}
