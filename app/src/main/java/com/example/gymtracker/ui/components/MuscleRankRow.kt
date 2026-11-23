package com.example.gymtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.atoms.ValueText
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** Meter thickness (§3: 2dp). A size. */
private val MeterHeight = 2.dp

/** The meter's box, tall enough for the band ticks to read. A size. */
private val MeterBox = 8.dp

/**
 * §3 `MuscleRankRow`: muscle name · 2dp heat meter · value. [fraction] is the bar's length on a
 * shared scale; its colour follows the heat ramp. [band] (two fractions on the same scale) draws
 * the decision-11 guideline as two `tertiary` ticks — pass it only in the WEEK range — and the
 * caller colours [valueColor] by where the value sits against it.
 */
@Composable
fun MuscleRankRow(
    name: String,
    value: String,
    unit: String?,
    fraction: Float,
    modifier: Modifier = Modifier,
    band: ClosedRange<Float>? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.tertiary
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "rank")
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.36f),
        )
        Spacer(Modifier.width(Spacing.m))
        Canvas(
            Modifier
                .weight(0.4f)
                .height(MeterBox),
        ) {
            val h = MeterHeight.toPx()
            val y = (size.height - h) / 2f
            val r = CornerRadius(h / 2f)
            drawRoundRect(track, Offset(0f, y), Size(size.width, h), r)
            if (f > 0f) {
                drawRoundRect(heatColor(0.25f + 0.75f * f), Offset(0f, y), Size((size.width * f).coerceAtLeast(h), h), r)
            }
            band?.let { b ->
                val w = 2.dp.toPx()
                listOf(b.start, b.endInclusive).forEach { at ->
                    val x = (size.width * at.coerceIn(0f, 1f) - w / 2f).coerceIn(0f, size.width - w)
                    drawRect(tick, Offset(x, 0f), Size(w, size.height))
                }
            }
        }
        Spacer(Modifier.width(Spacing.m))
        // Fixed share so every row's meter starts and ends at the same x.
        Box(Modifier.weight(0.24f), contentAlignment = Alignment.CenterEnd) {
            ValueText(value = value, unit = unit, style = AppText.numberM, color = valueColor)
        }
    }
}
