package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Sparkline — 96×28dp, 2dp ink stroke, the last point a 3dp accent dot;
 * no axes, no wash. Plan rows only. Draws nothing for fewer than two points, so a plan with
 * fewer than 2 sessions shows no sparkline. A flat series sits on the vertical centre.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    if (values.size < 2) return
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val accent = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.size(width = 96.dp, height = 28.dp)) {
        val dot = 3.dp.toPx()
        val stroke = 2.dp.toPx()
        // Inset by the dot so neither the stroke nor the end dot clips at the edges.
        val left = dot
        val right = size.width - dot
        val top = dot
        val bottom = size.height - dot
        val min = values.min()
        val max = values.max()
        val span = max - min
        fun point(i: Int): Offset {
            val x = left + (right - left) * i / (values.size - 1)
            val t = if (span == 0.0) 0.5f else ((values[i] - min) / span).toFloat()
            return Offset(x, bottom - (bottom - top) * t)
        }
        val path = Path()
        values.indices.forEach { i ->
            val p = point(i)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(accent, radius = dot, center = point(values.lastIndex))
    }
}
