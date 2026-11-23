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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.HeatRamp

/**
 * Progress primitives — design-plan §2 `Meter` and `StatusCell`.
 *
 * Values animate over 450ms and interpolate the value (§1 motion table); the
 * numbers beside them carry `tnum` so they don't jitter while it runs.
 */

private const val ANIM_MS = 450

/**
 * Linear meter: 2–3dp, rounded, `outlineVariant` track. [target] draws a 2dp
 * tick at that fraction in `tertiary` (the family's goal colour).
 */
@Composable
fun Meter(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 3.dp,
    target: Float? = null,
    animate: Boolean = true,
) {
    val value by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(if (animate) ANIM_MS else 0),
        label = "meter",
    )
    val track = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
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

/**
 * Heat-ramp colour for a 0..1 volume level (§1 data palette): interpolated
 * between the five stops `outlineVariant → … → primary`.
 */
fun heatColor(level: Float): Color {
    val v = level.coerceIn(0f, 1f) * (HeatRamp.size - 1)
    val i = v.toInt().coerceAtMost(HeatRamp.size - 2)
    return lerp(HeatRamp[i], HeatRamp[i + 1], v - i)
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
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}
