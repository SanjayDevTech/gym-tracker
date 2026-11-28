package com.example.gymtracker.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
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
import com.example.gymtracker.ui.components.atoms.heatRamp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The day-initial column on the left. A size, not a gap. */
private val LabelWidth = 20.dp

private val DayInitials = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * [weeks] ISO weeks as columns, oldest on the left, ending with the week of [endEpochDay] — the
 * shape of Foodprint's rhythm heatmap, so it fills the card's width. Seven rows Monday–Sunday of
 * rounded square cells: trained days from the heat ramp, others its untrained stop. A month
 * initial over the column where a month starts, day initials on the left, and under each column
 * a `✓` in the positive colour for a week in [metWeeks] (`·` for the running week until it meets
 * the target). Days after [today] are not drawn.
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
    val ramp = heatRamp()
    val lastWeek = WeeklyTarget.weekStart(endEpochDay)
    val firstWeek = lastWeek - (weeks - 1) * 7L
    val columns = (0 until weeks).map { firstWeek + it * 7L }
    val shownTrained = columns.sumOf { w -> (0 until 7).count { (w + it) in sessionDays && w + it <= today } }
    val shownMet = columns.count { it in metWeeks }

    Column(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Last $weeks weeks: $shownTrained training days, $shownMet weeks on target"
            },
    ) {
        // Month initials over the first column of each month.
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(LabelWidth))
            columns.forEachIndexed { index, week ->
                val monthStart = (0 until 7).map { week + it }.firstOrNull { LocalDate.ofEpochDay(it).dayOfMonth == 1 }
                val label = when {
                    monthStart != null -> LocalDate.ofEpochDay(monthStart).month
                    index == 0 -> LocalDate.ofEpochDay(week).month
                    else -> null
                }?.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                Box(Modifier.weight(1f)) {
                    if (label != null) Text(label, style = AppText.eyebrow, color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        for (d in 0 until 7) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(LabelWidth)) {
                    if (d % 2 == 0) Eyebrow(DayInitials[d])
                }
                columns.forEach { week ->
                    val day = week + d
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day <= today) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (day in sessionDays) ramp[3] else ramp.first(),
                                        RoundedCornerShape(4.dp),
                                    ),
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(LabelWidth))
            columns.forEach { week ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        week in metWeeks -> Text("✓", style = AppText.label, color = scheme.secondary)
                        week <= today && today < week + 7 -> Text("·", style = AppText.label, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
