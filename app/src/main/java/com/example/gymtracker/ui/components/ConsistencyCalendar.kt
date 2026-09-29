package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.gymtracker.domain.DateUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders the last `weeks` weeks as a 6-row × `weeks`-column dot grid. Filled = session that day,
 * outlined = rest day, empty = nothing. Today is the rightmost column, top row = Monday.
 * Sundays are omitted — gyms are closed, so there's no Sunday row.
 */
private const val GymDaysPerWeek = 6 // Mon–Sat; Sunday is skipped
@Composable
fun ConsistencyCalendar(
    sessionDays: Set<Long>,
    restDays: Set<Long> = emptySet(),
    weeks: Int = 12,
    modifier: Modifier = Modifier,
) {
    val filled = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val muted = MaterialTheme.colorScheme.surfaceVariant
    val today = LocalDate.now(ZoneId.systemDefault())
    val startCol = DateUtil.startOfWeek(today.toEpochDay() - (weeks - 1) * 7L, DayOfWeek.MONDAY)

    Canvas(modifier = modifier) {
        val cellW = size.width / weeks
        val cellH = size.height / GymDaysPerWeek
        val dot = minOf(cellW, cellH) * 0.4f
        for (col in 0 until weeks) {
            for (row in 0 until GymDaysPerWeek) {
                val day = startCol + col * 7L + row.toLong()
                val cx = col * cellW + cellW / 2f
                val cy = row * cellH + cellH / 2f
                val center = Offset(cx, cy)
                when {
                    day in sessionDays -> drawCircle(filled, dot, center)
                    day in restDays -> drawCircle(outline, dot, center, style = Stroke(width = 1.2f))
                    else -> drawCircle(muted, dot * 0.6f, center)
                }
            }
        }
    }
}
