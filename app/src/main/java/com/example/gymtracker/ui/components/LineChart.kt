package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Minimal Canvas line chart. Optional `referenceBand` is rendered as a translucent horizontal band.
 */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    referenceBand: ClosedRange<Float>? = null,
) {
    if (values.isEmpty()) return
    val color = MaterialTheme.colorScheme.primary
    val band = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
    val axis = MaterialTheme.colorScheme.outline
    val maxY = values.max().coerceAtLeast(1f)
    val minY = values.min().coerceAtMost(0f)
    val span = (maxY - minY).coerceAtLeast(1f)

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val pad = 6f
        val w = size.width - pad * 2f
        val h = size.height - pad * 2f
        fun yFor(v: Float) = pad + (1f - (v - minY) / span) * h

        referenceBand?.let { rb ->
            val y1 = yFor(rb.endInclusive)
            val y2 = yFor(rb.start)
            drawRect(
                color = band,
                topLeft = Offset(pad, y1),
                size = androidx.compose.ui.geometry.Size(w, y2 - y1),
            )
        }
        drawLine(axis, Offset(pad, pad + h), Offset(pad + w, pad + h), 1f)

        if (values.size == 1) {
            drawCircle(color, 4f, Offset(pad + w / 2f, yFor(values[0])))
            return@Canvas
        }
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = pad + (i.toFloat() / (values.size - 1)) * w
            val y = yFor(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2f))
        values.forEachIndexed { i, v ->
            val x = pad + (i.toFloat() / (values.size - 1)) * w
            drawCircle(color, 3f, Offset(x, yFor(v)))
        }
    }
}
