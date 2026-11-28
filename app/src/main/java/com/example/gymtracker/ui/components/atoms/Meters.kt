package com.example.gymtracker.ui.components.atoms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppTheme
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Progress primitives: `Meter` and `StatusCell`.
 *
 * Values animate over 450ms and interpolate the value; the
 * numbers beside them carry `tnum` so they don't jitter while it runs.
 */

private const val ANIM_MS = 600

/**
 * Linear meter: a soft rounded bar on a `surfaceContainerHigh` track, never thinner than 5dp so
 * it reads at a glance. [target] draws a tick at that fraction.
 */
@Composable
fun Meter(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 6.dp,
    target: Float? = null,
    animate: Boolean = true,
) {
    val value by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(if (animate) ANIM_MS else 0, easing = FastOutSlowInEasing),
        label = "meter",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val tick = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.fillMaxWidth().height(height.coerceAtLeast(5.dp))) {
        val r = size.height / 2f
        drawRoundRect(color = track, size = size, cornerRadius = CornerRadius(r, r))
        if (value > 0f) {
            drawRoundRect(
                color = color,
                // Never thinner than it is tall, so a tiny value is still a dot.
                size = Size((size.width * value).coerceAtLeast(size.height), size.height),
                cornerRadius = CornerRadius(r, r),
            )
        }
        if (target != null) {
            val w = 2.dp.toPx()
            val x = (size.width * target.coerceIn(0f, 1f) - w / 2f).coerceIn(0f, size.width - w)
            drawRect(color = tick, topLeft = Offset(x, 0f), size = Size(w, size.height))
        }
    }
}

/** The theme's training-volume ramp, untrained → most. */
@Composable
@ReadOnlyComposable
fun heatRamp(): List<Color> = AppTheme.extras.heat

/** Colour for a 0..1 volume level, interpolated along [ramp] (from [heatRamp]). */
fun heatColor(level: Float, ramp: List<Color>): Color {
    val v = level.coerceIn(0f, 1f) * (ramp.size - 1)
    val i = v.toInt().coerceAtMost(ramp.size - 2)
    return lerp(ramp[i], ramp[i + 1], v - i)
}

/**
 * A 10×10dp cell with a 3dp corner — the unit of the training calendar and the
 * week strip. Colour comes from the caller (usually [heatColor]).
 */
@Composable
fun StatusCell(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    // background(color, shape) draws the rounded rect directly. clip() + background() gave every
    // cell its own clipped graphics layer — 84 of them in the training calendar alone.
    Box(
        modifier
            .size(size)
            .background(color, StatusCellShape),
    )
}

private val StatusCellShape = RoundedCornerShape(3.dp)
