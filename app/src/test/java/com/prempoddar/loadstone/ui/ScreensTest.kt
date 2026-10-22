package com.prempoddar.loadstone.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
import com.prempoddar.loadstone.data.settings.UnitSystem
import com.prempoddar.loadstone.ui.components.AccuracyDial
import com.prempoddar.loadstone.ui.components.CompassArrow
import com.prempoddar.loadstone.ui.design.Appearance
import com.prempoddar.loadstone.ui.design.AppearanceScreen
import com.prempoddar.loadstone.ui.design.BigButton
import com.prempoddar.loadstone.ui.radar.RadarFace
import com.prempoddar.loadstone.ui.radar.RadarUiState
import com.prempoddar.loadstone.ui.theme.LOADSTONE_BRAND
import com.prempoddar.loadstone.ui.theme.LoadstoneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Draws the app's main parts at phone size into build/screens, for the README and to look over
 * without a phone. The parts are the app's own; the sample pins and readings are made up.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
open class ScreensTest {
    /** Where the pictures go; the tablet runs below draw the same screens into their own folders. */
    protected open val out = "build/screens"

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val dark = Appearance(mode = Appearance.Mode.DARK)
    private val light = Appearance(mode = Appearance.Mode.LIGHT)

    private fun shoot(name: String, a: Appearance, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { LoadstoneTheme(a) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) { content() } } }
        repeat(10) { Thread.sleep(80); rule.mainClock.advanceTimeBy(300) }
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bmp)) }
        File(out).apply { mkdirs() }.resolve("$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Composable
    private fun Page(eyebrow: String, title: String, body: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(32.dp))
            body()
        }
    }

    @Test fun finding() = shoot("finding-dark", dark) {
        Page("FINDING", "Car") {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) { CompassArrow(32f, arrived = false, accuracyMeters = 3f, modifier = Modifier.fillMaxSize()) }
            Spacer(Modifier.height(28.dp))
            Text("42 m", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("Level 2, by the lift · ±3 m", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Test fun dropping() = shoot("drop-light", light) {
        Page("DROP A PIN", "Hold still a moment") {
            AccuracyDial(accuracyMeters = 3.4f, stabilized = true, modifier = Modifier.size(280.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("±3 m", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Ready to save", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(36.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { BigButton("Save this spot", onClick = {}) }
        }
    }

    private fun pin(id: Long, name: String, northM: Double, eastM: Double, cat: PinCategory) =
        Pin(id = id, name = name, latitude = 19.0 + northM / 111_320.0, longitude = 72.8 + eastM / 105_200.0, accuracyMeters = 4f, category = cat)

    @Test fun radar() = shoot("radar-dark", dark) {
        val state = RadarUiState(
            pins = listOf(pin(1, "Car", 60.0, -30.0, PinCategory.CAR), pin(2, "Home", -150.0, 120.0, PinCategory.HOME), pin(3, "Tent", 20.0, 170.0, PinCategory.TENT)),
            here = PinFix(19.0, 72.8, 4f, 10), heading = 30.0,
        )
        Page("NEARBY", "Radar") { Box(Modifier.size(380.dp)) { RadarFace(state, 0.0, 200.0, UnitSystem.METRIC, {}, {}) } }
    }

    @Test fun appearance() = shoot("appearance-dark", dark) { AppearanceScreen(dark, LOADSTONE_BRAND, {}, {}) }
}
