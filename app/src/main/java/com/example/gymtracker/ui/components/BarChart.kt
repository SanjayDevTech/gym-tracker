package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/** §2 `BarChart` geometry: 150dp plot + 18dp axis. Sizes, not gaps. */
private val PlotHeight = 150.dp
private val AxisHeight = 18.dp
private val CapHeight = 2.dp
private val MaxCorner = 4.dp

/** One bar. [label] is drawn under it when set (callers thin labels so they never collide). */
@Immutable
data class BarDatum(
    val value: Float,
    val label: String? = null,
    /** Period still running (the current week): drawn outline-only until it ends. */
    val inProgress: Boolean = false,
)

/**
 * §2 `BarChart` — food's stacked-bar styling: bars in `inversePrimary` (deep volt) with a 2dp
 * `primary` top cap (S4-b), corner `min(w/2, 4dp)`; an optional dashed `tertiary` [reference]
 * line with its [referenceCaption] above the line if there is room, else below, clamped inside the
 * plot (food lesson 4). Tapping a bar selects it ([onSelect], tap again to clear); the selected
 * bar carries a [TooltipTag] with [tagFor]'s text.
 */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    modifier: Modifier = Modifier,
    reference: Float? = null,
    referenceCaption: String? = null,
    selected: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    tagFor: (Int) -> String? = { null },
    description: String? = null,
) {
    if (bars.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val barColor = scheme.inversePrimary
    val capColor = scheme.primary
    val refColor = scheme.tertiary
    val axisColor = scheme.onSurfaceVariant
    val baseColor = scheme.outlineVariant
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val plotPx = with(density) { PlotHeight.toPx() }
        // The axis grows with font scale (house rule 7): 18dp is a minimum.
        val axisPx = maxOf(with(density) { AxisHeight.toPx() }, measurer.measure("0", AppText.bodyS).size.height.toFloat())
        val maxValue = maxOf(bars.maxOf { it.value }, reference ?: 0f).takeIf { it > 0f } ?: 1f
        val top = maxValue * 1.1f
        val slot = widthPx / bars.size
        val barW = slot * 0.6f
        fun barLeft(i: Int) = i * slot + (slot - barW) / 2f
        fun yFor(v: Float) = plotPx * (1f - v / top)

        val sel = selected?.takeIf { it in bars.indices }
        val tag = sel?.let(tagFor)
        TagOverlay(
            tagText = tag,
            anchorX = sel?.let { barLeft(it) + barW / 2f } ?: 0f,
            anchorY = sel?.let { yFor(bars[it].value) - with(density) { CapHeight.toPx() } } ?: 0f,
        ) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(PlotHeight + with(density) { axisPx.toDp() })
                    .semantics { if (description != null) contentDescription = description }
                    .then(
                        if (onSelect != null) {
                            Modifier.pointerInput(bars.size, sel) {
                                detectTapGestures { p ->
                                    val i = (p.x / slot).toInt().coerceIn(0, bars.lastIndex)
                                    onSelect(if (i == sel) null else i)
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                val cap = CapHeight.toPx()
                val corner = minOf(barW / 2f, MaxCorner.toPx())
                // Baseline.
                drawRect(baseColor, Offset(0f, plotPx - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
                bars.forEachIndexed { i, bar ->
                    if (bar.value <= 0f) return@forEachIndexed
                    val left = barLeft(i)
                    val y = yFor(bar.value).coerceAtMost(plotPx - cap)
                    val shape = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left, top = y, right = left + barW, bottom = plotPx,
                                topLeftCornerRadius = CornerRadius(corner),
                                topRightCornerRadius = CornerRadius(corner),
                            ),
                        )
                    }
                    if (bar.inProgress) {
                        drawPath(shape, barColor, style = Stroke(width = 1.5.dp.toPx()))
                        clipPath(shape) { drawRect(capColor, Offset(left, y), Size(barW, cap)) }
                    } else {
                        clipPath(shape) {
                            drawRect(capColor, Offset(left, y), Size(barW, plotPx - y))
                            drawRect(barColor, Offset(left, y + cap), Size(barW, plotPx - y - cap))
                        }
                    }
                }
                if (reference != null && reference > 0f) {
                    val y = yFor(reference)
                    drawLine(
                        refColor,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    )
                    if (referenceCaption != null) {
                        val text = measurer.measure(referenceCaption, AppText.bodyS.copy(color = refColor))
                        val gap = Spacing.xs.toPx()
                        val above = y - gap - text.size.height
                        val ty = (if (above >= 0f) above else y + gap)
                            .coerceIn(0f, (plotPx - text.size.height).coerceAtLeast(0f))
                        drawText(text, topLeft = Offset(size.width - text.size.width, ty))
                    }
                }
                bars.forEachIndexed { i, bar ->
                    val label = bar.label ?: return@forEachIndexed
                    val text = measurer.measure(label, AppText.bodyS.copy(color = axisColor))
                    val x = (barLeft(i) + barW / 2f - text.size.width / 2f)
                        .coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f))
                    drawText(text, topLeft = Offset(x, plotPx + (axisPx - text.size.height) / 2f))
                }
            }
        }
    }
}
