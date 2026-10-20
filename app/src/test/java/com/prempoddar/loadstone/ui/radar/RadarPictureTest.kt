package com.prempoddar.loadstone.ui.radar

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.core.geo.PinFix
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory
import com.prempoddar.loadstone.data.settings.UnitSystem
import com.prempoddar.loadstone.ui.design.Appearance
import com.prempoddar.loadstone.ui.theme.LoadstoneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws the radar with a few pins and saves it, to look at. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h500dp-xxhdpi")
class RadarPictureTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun pin(id: Long, name: String, northM: Double, eastM: Double, cat: PinCategory) =
        Pin(id = id, name = name, latitude = 19.0 + northM / 111_320.0, longitude = 72.8 + eastM / 105_200.0, accuracyMeters = 4f, category = cat)

    @Test fun radar() {
        val state = RadarUiState(
            pins = listOf(
                pin(1, "Car", 60.0, -30.0, PinCategory.CAR), pin(2, "Home", -150.0, 120.0, PinCategory.HOME),
                pin(3, "Tent", 20.0, 170.0, PinCategory.TENT), pin(4, "Office", 4000.0, 0.0, PinCategory.STAR),
            ),
            here = PinFix(19.0, 72.8, 4f, 10), heading = 30.0,
        )
        rule.setContent {
            LoadstoneTheme(Appearance(mode = Appearance.Mode.DARK)) {
                Box(Modifier.size(380.dp)) { RadarFace(state, 0.0, 200.0, UnitSystem.METRIC, {}, {}) }
            }
        }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File("build/screens").mkdirs()
        File("build/screens/radar.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
