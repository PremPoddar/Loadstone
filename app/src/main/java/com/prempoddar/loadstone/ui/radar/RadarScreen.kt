package com.prempoddar.loadstone.ui.radar

import com.prempoddar.loadstone.ui.design.tap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prempoddar.loadstone.core.format.UnitFormatter
import com.prempoddar.loadstone.core.geo.Radar
import com.prempoddar.loadstone.ui.LocalUnitSystem
import com.prempoddar.loadstone.ui.components.visual
import com.prempoddar.loadstone.ui.permission.rememberLocationPermissionState
import kotlin.math.hypot

/**
 * Your pins around you, as a radar: true direction and scaled distance, with rings to read them
 * by. North up, or turning with you. Pinch or the buttons to change the range; tap a pin to go.
 * No map tiles, so nothing is fetched from anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(onOpenPin: (Long) -> Unit, onBack: () -> Unit, viewModel: RadarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val permission = rememberLocationPermissionState()
    LaunchedEffect(permission.hasPermission) { if (permission.hasPermission) viewModel.start() }
    val unit = LocalUnitSystem.current
    var facingUp by rememberSaveable { mutableStateOf(false) }
    // Null means automatic: fitted to the nearest few pins.
    var chosenRange by rememberSaveable { mutableStateOf<Double?>(null) }
    val here = state.here
    val distances = remember(state.pins, here?.latitude, here?.longitude) {
        if (here == null) emptyList() else state.pins.map { com.prempoddar.loadstone.core.geo.GeoMath.distanceMeters(here.latitude, here.longitude, it.latitude, it.longitude) }
    }
    val range = chosenRange ?: Radar.autoRange(distances)
    fun step(by: Int) {
        val i = Radar.RANGES.indexOf(range).takeIf { it >= 0 } ?: Radar.RANGES.indexOfFirst { it >= range }
        chosenRange = Radar.RANGES[(i + by).coerceIn(0, Radar.RANGES.lastIndex)]
    }
    val facing = if (facingUp) state.heading ?: 0.0 else 0.0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Around you") },
                navigationIcon = { IconButton(onClick = tap(onBack)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                SegmentedButton(!facingUp, { facingUp = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("North up") }
                SegmentedButton(facingUp, { facingUp = true }, SegmentedButtonDefaults.itemShape(1, 2), enabled = state.heading != null) { Text("Turn with me") }
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                if (here == null) {
                    Text(if (permission.hasPermission) "Finding where you are…" else "Loadstone needs your location for this", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    RadarFace(state, facing, range, unit, onOpenPin, onStep = ::step)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(onClick = tap { step(-1) }) { Icon(Icons.Filled.Remove, "Closer in") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Edge: ${UnitFormatter.formatDistance(range, unit)}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (chosenRange == null) "Fitted to your nearest pins" else "Tap here to fit your nearest pins",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.pointerInput(Unit) { detectTapGestures { chosenRange = null } },
                    )
                }
                FilledTonalIconButton(onClick = tap { step(1) }) { Icon(Icons.Filled.Add, "Further out") }
            }
            val beyond = distances.count { it > range }
            if (beyond > 0) {
                Text(
                    "$beyond ${if (beyond == 1) "pin is" else "pins are"} further away, shown on the edge",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun RadarFace(state: RadarUiState, facing: Double, range: Double, unit: com.prempoddar.loadstone.data.settings.UnitSystem, onOpenPin: (Long) -> Unit, onStep: (Int) -> Unit) {
    val here = state.here ?: return
    val ring = MaterialTheme.colorScheme.outlineVariant
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    val face = MaterialTheme.colorScheme.surfaceContainer
    val you = MaterialTheme.colorScheme.primary
    val north = MaterialTheme.colorScheme.error
    val text = rememberTextMeasurer()
    val blips = remember(state.pins, here, facing, range) {
        Radar.layout(here.latitude, here.longitude, facing, range, state.pins.map { Radar.Target(it.id, it.latitude, it.longitude) })
    }
    val byId = remember(state.pins) { state.pins.associateBy { it.id } }
    Canvas(
        Modifier.fillMaxSize()
            .pointerInput(blips) {
                detectTapGestures { o ->
                    val r = size.width / 2f
                    blips.minByOrNull { hypot(o.x - (r + it.x * r * 0.92).toFloat(), o.y - (r + it.y * r * 0.92).toFloat()) }
                        ?.takeIf { hypot(o.x - (r + it.x * r * 0.92).toFloat(), o.y - (r + it.y * r * 0.92).toFloat()) < 28.dp.toPx() }
                        ?.let { onOpenPin(it.id) }
                }
            }
            .pointerInput(Unit) {
                // Spreading the fingers moves in to a closer range, pinching moves out, a step at a time.
                var pinch = 1f
                detectTransformGestures { _, _, zoom, _ ->
                    pinch *= zoom
                    if (pinch > 1.4f) { onStep(-1); pinch = 1f } else if (pinch < 0.7f) { onStep(1); pinch = 1f }
                }
            },
    ) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.width / 2 * 0.92f
        drawCircle(face, size.width / 2, c)
        // Rings at a quarter, half, three quarters and the edge, each with its distance.
        for (k in 1..4) {
            drawCircle(ring, r * k / 4, c, style = Stroke(1.dp.toPx()))
            // Labelled down the lower-right diagonal, out of the way of north and most pins.
            val d = UnitFormatter.formatDistance(range * k / 4, unit)
            val at = r * k / 4 * 0.7071f
            drawText(text, d, Offset(c.x + at + 2.dp.toPx(), c.y + at - 2.dp.toPx()), style = TextStyle(fontSize = 10.sp, color = label))
        }
        // North, wherever it is when the radar turns with you.
        // In the margin outside the rings, so it never covers a pin on the edge.
        rotate(-facing.toFloat(), c) {
            val top = c.y - size.width / 2 + 1.dp.toPx()
            val base = c.y - r - 1.dp.toPx()
            drawPath(Path().apply { moveTo(c.x, top); lineTo(c.x - 6.dp.toPx(), base); lineTo(c.x + 6.dp.toPx(), base); close() }, north)
        }
        blips.forEach { b ->
            val pin = byId[b.id] ?: return@forEach
            val p = Offset(c.x + (b.x * r).toFloat(), c.y + (b.y * r).toFloat())
            val accent = pin.category.visual().accent
            if (b.beyond) {
                drawCircle(accent.copy(alpha = 0.5f), 6.dp.toPx(), p)
            } else {
                drawCircle(Color.White, 9.dp.toPx(), p)
                drawCircle(accent, 7.dp.toPx(), p)
            }
            val name = text.measure(pin.name, TextStyle(fontSize = 11.sp, color = label), maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = androidx.compose.ui.unit.Constraints(maxWidth = 110.dp.roundToPx()))
            drawText(name, topLeft = Offset((p.x - name.size.width / 2).coerceIn(0f, size.width - name.size.width), p.y + 10.dp.toPx()))
        }
        // You, pointing the way the phone faces.
        val h = (state.heading ?: 0.0) - facing
        rotate(h.toFloat(), c) {
            drawPath(Path().apply { moveTo(c.x, c.y - 12.dp.toPx()); lineTo(c.x - 8.dp.toPx(), c.y + 9.dp.toPx()); lineTo(c.x, c.y + 4.dp.toPx()); lineTo(c.x + 8.dp.toPx(), c.y + 9.dp.toPx()); close() }, if (state.heading != null) you else you.copy(alpha = 0.5f))
        }
    }
}
