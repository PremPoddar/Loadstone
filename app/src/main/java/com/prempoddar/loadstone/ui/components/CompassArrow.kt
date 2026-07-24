package com.prempoddar.loadstone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Large follow-back arrow. A single bold arrow points to the pin (0° = dead ahead),
 * rotating the shortest way with a confident, critically-damped spring, and turns
 * green on arrival.
 */
@Composable
fun CompassArrow(
    angleDegrees: Float,
    arrived: Boolean,
    modifier: Modifier = Modifier,
) {
    // Unwrap the target angle so the arrow never spins the long way around.
    var cumulative by remember { mutableFloatStateOf(angleDegrees) }
    LaunchedEffect(angleDegrees) {
        val delta = ((angleDegrees - cumulative + 540f) % 360f) - 180f
        cumulative += delta
    }
    // Critically damped, deliberate settle — steady, not springy or twitchy.
    val animated by animateFloatAsState(
        targetValue = cumulative,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "arrow",
    )

    val amberTop = Color(0xFFFFC64D)
    val amberBottom = Color(0xFFF59E00)
    val greenTop = Color(0xFF57C98A)
    val greenBottom = Color(0xFF2E9E5B)
    val fillTop by animateColorAsState(if (arrived) greenTop else amberTop, tween(500), label = "top")
    val fillBottom by animateColorAsState(if (arrived) greenBottom else amberBottom, tween(500), label = "bot")

    val backdrop = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val ring = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = min(size.width, size.height) / 2

            // Soft circular backdrop + thin ring for grounding.
            drawCircle(backdrop, radius = r * 0.94f, center = c)
            drawCircle(ring, radius = r * 0.94f, center = c, style = Stroke(width = 3f))

            rotate(animated, pivot = c) {
                val tipY = c.y - r * 0.66f
                val headHalf = r * 0.32f
                val headBaseY = c.y - r * 0.04f
                val shaftHalf = r * 0.12f
                val baseY = c.y + r * 0.62f

                val arrow = Path().apply {
                    moveTo(c.x, tipY)
                    lineTo(c.x + headHalf, headBaseY)
                    lineTo(c.x + shaftHalf, headBaseY)
                    lineTo(c.x + shaftHalf, baseY)
                    lineTo(c.x - shaftHalf, baseY)
                    lineTo(c.x - shaftHalf, headBaseY)
                    lineTo(c.x - headHalf, headBaseY)
                    close()
                }
                // Soft drop shadow for depth (confident, dimensional).
                val shadow = Path().apply { addPath(arrow, Offset(0f, r * 0.02f)) }
                drawPath(shadow, Color.Black.copy(alpha = 0.12f))
                // Body with a vertical gradient.
                drawPath(
                    path = arrow,
                    brush = Brush.verticalGradient(
                        colors = listOf(fillTop, fillBottom),
                        startY = tipY,
                        endY = baseY,
                    ),
                )
                // Crisp same-color rounded outline to soften the edges.
                drawPath(
                    path = arrow,
                    color = fillBottom,
                    style = Stroke(width = r * 0.03f, join = StrokeJoin.Round, cap = StrokeCap.Round),
                )
            }
        }
    }
}
