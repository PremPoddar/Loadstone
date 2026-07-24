package com.prempoddar.loadstone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Radar-style accuracy gauge. The glowing ring's radius maps to the estimated
 * accuracy, so it visibly tightens as the fix converges. A slow sweep signals
 * that measurement is ongoing; the color shifts from amber to green on lock.
 */
@Composable
fun AccuracyDial(
    accuracyMeters: Float?,
    stabilized: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val amber = Color(0xFFFFB300)
    val green = Color(0xFF2E9E5B)
    val ringColor by animateColorAsState(if (stabilized) green else amber, tween(600), label = "ring")

    // Map accuracy (2..40 m) to a radius fraction, kept large enough that the ring
    // always encircles the centered readout rather than shrinking over the text.
    val target = accuracyMeters?.let {
        val clamped = it.coerceIn(2f, 40f)
        0.60f + (clamped - 2f) / (40f - 2f) * 0.34f // 0.60 .. 0.94
    } ?: 0.94f
    val radiusFraction by animateFloatAsState(target, tween(700), label = "radius")

    val transition = rememberInfiniteTransition(label = "sweep")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweepAngle",
    )
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(260.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val maxR = min(size.width, size.height) / 2 * 0.94f

            // Static target grid.
            for (ring in 1..3) {
                drawCircle(gridColor, radius = maxR * ring / 3f, center = c, style = Stroke(width = 2f))
            }

            // Sweeping measurement line (only while not locked).
            if (!stabilized) {
                rotate(sweep, pivot = c) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.55f)),
                            startX = c.x, endX = c.x + maxR,
                        ),
                        start = c, end = Offset(c.x + maxR, c.y), strokeWidth = 6f,
                    )
                }
            }

            // The accuracy ring: glowing halo + crisp stroke.
            val r = maxR * radiusFraction
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ringColor.copy(alpha = 0.22f), Color.Transparent),
                    center = c, radius = r * 1.5f,
                ),
                radius = r * 1.5f, center = c,
            )
            drawCircle(ringColor, radius = r, center = c, style = Stroke(width = 8f))
            drawCircle(ringColor, radius = 5f, center = c)
        }
        content()
    }
}
