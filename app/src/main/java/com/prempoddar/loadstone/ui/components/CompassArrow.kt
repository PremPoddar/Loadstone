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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Minimal follow-back pointer: a single thick, rounded arrow that points to the pin
 * (0° = dead ahead) and settles with a confident, critically-damped spring. A short
 * arc near the tip widens as GPS accuracy worsens — "where the pin might be."
 */
@Composable
fun CompassArrow(
    angleDegrees: Float,
    arrived: Boolean,
    accuracyMeters: Float?,
    modifier: Modifier = Modifier,
) {
    var cumulative by remember { mutableFloatStateOf(angleDegrees) }
    LaunchedEffect(angleDegrees) {
        val delta = ((angleDegrees - cumulative + 540f) % 360f) - 180f
        cumulative += delta
    }
    val animated by animateFloatAsState(
        targetValue = cumulative,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "arrow",
    )

    val restColor = MaterialTheme.colorScheme.onSurface
    val arrivedColor = MaterialTheme.colorScheme.primary
    val color by animateColorAsState(if (arrived) arrivedColor else restColor, tween(500), label = "arrowColor")
    val arcColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    // Wider arc for weaker GPS: map ~3..50 m to ~16..92 degrees of sweep.
    val arcSweep by animateFloatAsState(
        targetValue = accuracyMeters?.let { 16f + (it.coerceIn(3f, 50f) - 3f) / 47f * 76f } ?: 24f,
        animationSpec = tween(500),
        label = "arcSweep",
    )

    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = min(size.width, size.height) / 2
            val stroke = Stroke(width = r * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)

            rotate(animated, pivot = c) {
                val tipY = c.y - r * 0.55f
                val tailY = c.y + r * 0.55f
                val headHalf = r * 0.34f
                val headDrop = r * 0.30f

                val arrow = Path().apply {
                    moveTo(c.x, tailY)
                    lineTo(c.x, tipY)
                    moveTo(c.x - headHalf, tipY + headDrop)
                    lineTo(c.x, tipY)
                    lineTo(c.x + headHalf, tipY + headDrop)
                }
                drawPath(arrow, color, style = stroke)

                if (!arrived && accuracyMeters != null) {
                    val arcR = r * 0.74f
                    drawArc(
                        color = arcColor,
                        startAngle = 270f - arcSweep / 2f,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = Offset(c.x - arcR, c.y - arcR),
                        size = Size(arcR * 2, arcR * 2),
                        style = Stroke(width = r * 0.05f, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}
