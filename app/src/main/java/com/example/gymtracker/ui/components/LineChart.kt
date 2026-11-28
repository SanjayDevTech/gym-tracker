package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.gymtracker.domain.ProgressPoint
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/** Line chart geometry: 180dp plot + 22dp axis. Sizes, not gaps. */
private val PlotHeight = 180.dp
private val AxisHeight = 22.dp

/**
 * `LineChart`, Foodprint's look: 4 faint hairline grid lines, a 3dp ink stroke over a 10%→0 wash,
 * 3dp ink dots; y labels (max on the top line, min on the bottom) and x labels (first,
 * middle and last date) in [AppText.bodyS]. Points sit at their dates, so gaps in training read
 * as gaps. The last point is ringed in the accent and carries its value.
 *
 * [band] draws a `tertiary`@12% target band with [bandCaption] above it if there is room, else
 * below, clamped inside the plot (food lesson 4).
 */
@Composable
fun LineChart(
    points: List<ProgressPoint>,
    formatValue: (Float) -> String,
    formatDate: (Long) -> String,
    modifier: Modifier = Modifier,
    band: ClosedRange<Float>? = null,
    bandCaption: String? = null,
    description: String? = null,
) {
    if (points.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val line = scheme.onSurface
    val accent = scheme.tertiary
    val ringFill = scheme.surfaceContainerLowest
    val grid = scheme.onSurface.copy(alpha = 0.08f)
    val bandFill = scheme.tertiary.copy(alpha = 0.12f)
    val bandText = scheme.tertiary
    val axisText = scheme.onSurfaceVariant
    val valueText = scheme.onSurface
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The axis grows with font scale: 22dp is a minimum (house rule 9).
    val axisDp = with(density) {
        maxOf(AxisHeight.toPx(), measurer.measure("0", AppText.bodyS).size.height.toFloat()).toDp()
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(PlotHeight + axisDp)
            .semantics { if (description != null) contentDescription = description },
    ) {
        val plotH = PlotHeight.toPx()
        val labelGap = Spacing.s.toPx()
        val inset = 6.dp.toPx() // keeps the ring and dots inside the canvas

        val values = points.map { it.value }
        var lo = minOf(values.min(), band?.start ?: Float.MAX_VALUE)
        var hi = maxOf(values.max(), band?.endInclusive ?: -Float.MAX_VALUE)
        if (hi - lo < 1e-3f) { lo -= 1f; hi += 1f }
        val pad = (hi - lo) * 0.12f
        lo -= pad
        hi += pad

        val yStyle = AppText.bodyS.copy(color = axisText)
        val topLabel = measurer.measure(formatValue(values.max()), yStyle)
        val bottomLabel = measurer.measure(formatValue(values.min()), yStyle)
        val left = maxOf(topLabel.size.width, bottomLabel.size.width) + labelGap
        val right = size.width - inset
        val top = inset
        val bottom = plotH - inset
        fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)

        val firstDay = points.first().dateEpochDay
        val lastDay = points.last().dateEpochDay
        val spanDays = (lastDay - firstDay).toFloat()
        fun x(day: Long) =
            if (spanDays <= 0f) (left + right) / 2f else left + inset + (day - firstDay) / spanDays * (right - left - inset)

        // Grid: four lines, evenly spaced across the plot.
        for (i in 0..3) {
            val gy = top + (bottom - top) * i / 3f
            drawLine(grid, Offset(left, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
        }
        // y labels: the extremes of the data, on their own heights.
        drawText(topLabel, topLeft = Offset(0f, (y(values.max()) - topLabel.size.height / 2f).coerceIn(0f, plotH - topLabel.size.height)))
        drawText(bottomLabel, topLeft = Offset(0f, (y(values.min()) - bottomLabel.size.height / 2f).coerceIn(0f, plotH - bottomLabel.size.height)))

        if (band != null) {
            val bandTop = y(band.endInclusive)
            val bandBottom = y(band.start)
            drawRect(bandFill, Offset(left, bandTop), Size(size.width - left, bandBottom - bandTop))
            if (bandCaption != null) {
                val t = measurer.measure(bandCaption, AppText.bodyS.copy(color = bandText))
                val gap = Spacing.xs.toPx()
                val above = bandTop - gap - t.size.height
                val ty = (if (above >= 0f) above else bandBottom + gap).coerceIn(0f, (plotH - t.size.height).coerceAtLeast(0f))
                drawText(t, topLeft = Offset(left + labelGap, ty))
            }
        }

        val pts = points.map { Offset(x(it.dateEpochDay), y(it.value)) }
        if (pts.size > 1) {
            val stroke = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val wash = Path().apply {
                addPath(stroke)
                lineTo(pts.last().x, bottom)
                lineTo(pts.first().x, bottom)
                close()
            }
            drawPath(wash, Brush.verticalGradient(listOf(line.copy(alpha = 0.10f), Color.Transparent), startY = top, endY = bottom))
            drawPath(stroke, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        pts.forEach { drawCircle(line, 3.dp.toPx(), it) }

        // Last point: ringed, with its value above (below if there's no room).
        val last = pts.last()
        val ringR = 6.dp.toPx()
        drawCircle(ringFill, ringR, last)
        drawCircle(accent, ringR, last, style = Stroke(width = 2.5.dp.toPx()))
        val lastLabel = measurer.measure(formatValue(values.last()), AppText.label.copy(color = valueText))
        val ly = (last.y - ringR - Spacing.xs.toPx() - lastLabel.size.height).let { if (it < 0f) last.y + ringR + Spacing.xs.toPx() else it }
        val lx = (last.x - lastLabel.size.width / 2f).coerceIn(left, (size.width - lastLabel.size.width).coerceAtLeast(left))
        drawText(lastLabel, topLeft = Offset(lx, ly.coerceAtMost(plotH - lastLabel.size.height)))

        // x labels: first, middle and last date.
        val axisY = plotH
        val xStyle = AppText.bodyS.copy(color = axisText)
        val dates = when {
            spanDays <= 0f -> listOf(firstDay)
            spanDays < 2f -> listOf(firstDay, lastDay)
            else -> listOf(firstDay, (firstDay + lastDay) / 2, lastDay)
        }
        dates.forEach { d ->
            val t = measurer.measure(formatDate(d), xStyle)
            val tx = (x(d) - t.size.width / 2f).coerceIn(left, (size.width - t.size.width).coerceAtLeast(left))
            drawText(t, topLeft = Offset(tx, axisY + (size.height - axisY - t.size.height) / 2f))
        }
    }
}
