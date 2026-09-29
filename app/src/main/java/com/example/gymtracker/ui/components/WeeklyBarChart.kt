package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

data class BarDatum(val label: String, val value: Float)

/**
 * Canvas bar chart. `referenceBand` is rendered as a translucent horizontal band
 * (e.g. 10..20 sets/week).
 */
@Composable
fun WeeklyBarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    referenceBand: ClosedRange<Float>? = null,
    yMax: Float? = null,
) {
    if (data.isEmpty()) return
    val bar = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    val band = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val resolvedMax = yMax ?: data.maxOf { it.value }.coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val pad = 8f
        val barAreaH = size.height - pad - 18f
        val bw = (size.width - pad * 2f) / data.size

        referenceBand?.let { rb ->
            val y1 = (1 - rb.endInclusive / resolvedMax).coerceIn(0f, 1f) * barAreaH + pad
            val y2 = (1 - rb.start / resolvedMax).coerceIn(0f, 1f) * barAreaH + pad
            drawRect(
                color = band,
                topLeft = Offset(pad, y1),
                size = Size(size.width - pad * 2f, y2 - y1),
            )
        }
        drawLine(
            axis,
            Offset(pad, barAreaH + pad),
            Offset(size.width - pad, barAreaH + pad),
            1f,
        )
        data.forEachIndexed { i, datum ->
            val h = (datum.value / resolvedMax).coerceIn(0f, 1f) * barAreaH
            val x = pad + i * bw + bw * 0.15f
            val w = bw * 0.7f
            drawRect(
                color = bar,
                topLeft = Offset(x, barAreaH + pad - h),
                size = Size(w, h),
            )
            val layout = measurer.measure(datum.label, style = labelStyle)
            drawText(
                textLayoutResult = layout,
                brush = SolidColor(labelColor),
                topLeft = Offset(x + (w - layout.size.width) / 2f, barAreaH + pad + 2f),
            )
        }
    }
}
