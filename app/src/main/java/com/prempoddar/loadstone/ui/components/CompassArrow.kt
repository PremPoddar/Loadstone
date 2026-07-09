package com.prempoddar.loadstone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Large follow-back compass. The needle rotates to [angleDegrees] (0 = dead
 * ahead) taking the shortest path, and turns green when the user has arrived.
 */
@Composable
fun CompassArrow(
    angleDegrees: Float,
    arrived: Boolean,
    modifier: Modifier = Modifier,
) {
    // Unwrap the target angle so the needle never spins the long way around.
    var cumulative by remember { mutableFloatStateOf(angleDegrees) }
    LaunchedEffect(angleDegrees) {
        val delta = ((angleDegrees - cumulative + 540f) % 360f) - 180f
        cumulative += delta
    }
    val animated by animateFloatAsState(cumulative, tween(320), label = "needle")

    val amber = Color(0xFFFFB300)
    val green = Color(0xFF2E9E5B)
    val needleColor by animateColorAsState(if (arrived) green else amber, tween(500), label = "needle-color")
    val tail = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val bezel = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = min(size.width, size.height) / 2

            drawCircle(bezel, radius = r * 0.98f, center = c, style = Stroke(width = 3f))

            // Cardinal + minor ticks around the bezel.
            for (i in 0 until 72) {
                val major = i % 18 == 0
                val angle = Math.toRadians(i * 5.0)
                val outer = r * 0.98f
                val inner = r * (if (major) 0.86f else 0.92f)
                val sinA = sin(angle).toFloat()
                val cosA = cos(angle).toFloat()
                drawLine(
                    color = if (major) tickColor else tickColor.copy(alpha = 0.4f),
                    start = Offset(c.x + sinA * inner, c.y - cosA * inner),
                    end = Offset(c.x + sinA * outer, c.y - cosA * outer),
                    strokeWidth = if (major) 4f else 2f,
                )
            }

            // Needle, rotated to the target angle.
            rotate(animated, pivot = c) {
                val tip = Offset(c.x, c.y - r * 0.72f)
                val baseL = Offset(c.x - r * 0.16f, c.y)
                val baseR = Offset(c.x + r * 0.16f, c.y)
                val head = Path().apply {
                    moveTo(tip.x, tip.y); lineTo(baseL.x, baseL.y); lineTo(baseR.x, baseR.y); close()
                }
                drawPath(head, needleColor)
                // Muted tail.
                val tailPath = Path().apply {
                    moveTo(c.x, c.y + r * 0.62f); lineTo(baseL.x, baseL.y); lineTo(baseR.x, baseR.y); close()
                }
                drawPath(tailPath, tail)
            }
            drawCircle(needleColor, radius = r * 0.06f, center = c)
        }
    }
}
