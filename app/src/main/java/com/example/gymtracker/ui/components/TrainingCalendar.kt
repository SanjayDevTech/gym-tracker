package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.StatusCell
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One calendar column. A size, not a gap. */
private val ColumnWidth = 24.dp

private val DayInitials = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * §2 `TrainingCalendar` (rewrite of `ConsistencyCalendar`): [weeks] ISO weeks as rows, oldest on
 * top, ending with the week of [endEpochDay]. Seven columns Monday–Sunday (Sunday included) of
 * [StatusCell]s — trained days in `primary`, others the empty `outlineVariant`. Eyebrow day
 * initials on top, a month initial on the left of the row where a month starts, and on the right
 * a `✓` in `primary` for every week in [metWeeks] (decision 7). The running week shows `·` until
 * it meets the target. Days after [today] are not drawn.
 */
@Composable
fun TrainingCalendar(
    sessionDays: Set<Long>,
    metWeeks: Set<Long>,
    endEpochDay: Long,
    today: Long,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
) {
    val scheme = MaterialTheme.colorScheme
    val lastWeek = WeeklyTarget.weekStart(endEpochDay)
    val firstWeek = lastWeek - (weeks - 1) * 7L
    val rows = (0 until weeks).map { firstWeek + it * 7L }
    val shownTrained = rows.sumOf { w -> (0 until 7).count { (w + it) in sessionDays && w + it <= today } }
    val shownMet = rows.count { it in metWeeks }

    Column(
        modifier.clearAndSetSemantics {
            contentDescription = "Last $weeks weeks: $shownTrained training days, $shownMet weeks on target"
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row {
            Box(Modifier.width(ColumnWidth))
            DayInitials.forEach { d ->
                Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) { Eyebrow(d) }
            }
        }
        rows.forEachIndexed { index, week ->
            val monthStart = (0 until 7).map { week + it }.firstOrNull { LocalDate.ofEpochDay(it).dayOfMonth == 1 }
            val monthLabel = when {
                monthStart != null -> LocalDate.ofEpochDay(monthStart).month
                index == 0 -> LocalDate.ofEpochDay(week).month
                else -> null
            }?.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(ColumnWidth)) { if (monthLabel != null) Eyebrow(monthLabel) }
                for (d in 0 until 7) {
                    val day = week + d
                    Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
                        if (day <= today) {
                            StatusCell(if (day in sessionDays) scheme.primary else scheme.outlineVariant)
                        }
                    }
                }
                Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
                    when {
                        week in metWeeks -> Text("✓", style = AppText.label, color = scheme.primary)
                        week <= today && today < week + 7 -> Text("·", style = AppText.label, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
