package com.example.gymtracker.ui.screens.plans.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Meter
import com.example.gymtracker.ui.components.atoms.SectionHead
import com.example.gymtracker.ui.components.atoms.StatColumns
import com.example.gymtracker.ui.components.atoms.StatItem
import com.example.gymtracker.ui.components.atoms.StatusCell
import com.example.gymtracker.ui.components.atoms.ValueText
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.grouped
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.screens.plans.WeekSummary
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

private val DAY_INITIALS = listOf("M", "T", "W", "T", "F", "S", "S")

/** A cell wide enough for its day initial at any font scale; a size, not a gap. */
private val CellSlot = 14.dp

/**
 * Week hero — design-plan §3 `WeekHero` (decision 7). Sessions this week against the weekly
 * target as a `numberXL` count, seven `StatusCell`s Mon–Sun (today's initial in primary), a 2dp
 * meter to the target — `tertiary` while short of it, `primary` once met (the rest meter's rule,
 * §6.3) — then weeks on target of the last 12, 7-day volume and sets.
 */
@Composable
fun WeekHero(
    week: WeekSummary,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val met = week.sessions >= week.target
    Column(modifier.fillMaxWidth()) {
        SectionHead(title = "This week") {
            Eyebrow("Target ${week.target}")
        }
        Spacer(Modifier.height(Spacing.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ValueText(
                    value = week.sessions.toString(),
                    unit = "of ${week.target}",
                    style = AppText.numberXL,
                )
                Text(
                    plural(week.sessions, "session"),
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.m))
            DayStrip(week)
        }
        Spacer(Modifier.height(Spacing.m))
        Meter(
            fraction = week.sessions.toFloat() / week.target.coerceAtLeast(1),
            height = 2.dp,
            color = if (met) scheme.primary else scheme.tertiary,
        )
        Spacer(Modifier.height(Spacing.l))
        StatColumns(
            items = listOf(
                if (week.onTarget.of == 0) {
                    StatItem("On target", EM_DASH)
                } else {
                    StatItem("On target", week.onTarget.met.toString(), "of ${week.onTarget.of} wk")
                },
                StatItem("Volume · 7d", formatVolumeNumber(week.volumeKg7d, unit), unit.label),
                StatItem("Sets · 7d", grouped(week.sets7d)),
            ),
        )
    }
}

@Composable
private fun DayStrip(week: WeekSummary) {
    val scheme = MaterialTheme.colorScheme
    val described = "${week.trainedDays.size} of 7 days trained this week"
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = described },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        DAY_INITIALS.forEachIndexed { i, initial ->
            Column(
                modifier = Modifier.widthIn(min = CellSlot),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                StatusCell(if (i in week.trainedDays) scheme.primary else scheme.outlineVariant)
                Text(
                    initial,
                    style = AppText.eyebrow,
                    color = if (i == week.todayIndex) scheme.primary else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
