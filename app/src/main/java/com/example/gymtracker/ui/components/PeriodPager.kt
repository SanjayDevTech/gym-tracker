package com.example.gymtracker.ui.components

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.padding
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.theme.Radius
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * `‹ 21 – 27 Nov ›` — Foodprint's date pill, exactly: a compact white pill sized to its label,
 * with 40dp chevron keys either side. It sits in the page header beside the title, as Today's does. `›` is disabled at the current period
 * ([canGoNext] false) and `‹` at the period holding the first logged session ([canGoPrevious]
 * false). The label crossfades.
 */
@Composable
fun PeriodPager(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
    canGoPrevious: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(cardColor()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillArrow(Lucide.ChevronLeft, "Previous period", canGoPrevious) { haptics.light(); onPrevious() }
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                fadeIn(tween(200, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(200, easing = FastOutSlowInEasing))
            },
            label = "period",
            modifier = Modifier.weight(1f, fill = false),
        ) { text ->
            Text(
                text,
                style = AppText.label,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(vertical = 10.dp, horizontal = 2.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        PillArrow(Lucide.ChevronRight, "Next period", canGoNext) { haptics.light(); onNext() }
    }
}

/** A 40dp key holding an 18dp chevron; faded when disabled — Foodprint's `PillArrow`. */
@Composable
private fun PillArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(40.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
            }
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp),
        )
    }
}

private val DayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy")
private val DayMonth = DateTimeFormatter.ofPattern("d MMM")
private val WeekdayDayMonthYear = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
private val WeekdayDayMonth = DateTimeFormatter.ofPattern("EEE d MMM")
private val MonthOnly = DateTimeFormatter.ofPattern("MMM")
private val MonthShortYear = DateTimeFormatter.ofPattern("MMM ''yy")

/**
 * `17 – 23 Nov 2026`, `27 Oct – 23 Nov 2026`, `20 Dec 2025 – 18 Jan 2026`; a one-day range reads
 * `Mon 23 Nov 2026`. Dashes are en dashes with spaces, as in the plan.
 */
fun periodLabel(range: ClosedRange<Long>, thisYear: Int = LocalDate.now().year): String {
    val start = LocalDate.ofEpochDay(range.start)
    val end = LocalDate.ofEpochDay(range.endInclusive)
    // Long ranges read in months, so the pill stays short: `Sep – Nov`, `Nov '25 – Nov`.
    if (range.endInclusive - range.start > 31) {
        val from = start.format(if (start.year != thisYear) MonthShortYear else MonthOnly)
        val to = end.format(if (end.year != thisYear) MonthShortYear else MonthOnly)
        return if (from == to) from else "$from – $to"
    }
    // Within this year the year is noise, as in Foodprint's `27 Nov`.
    if (start.year == thisYear && end.year == thisYear) {
        return when {
            start == end -> end.format(WeekdayDayMonth)
            start.month != end.month -> "${start.format(DayMonth)} – ${end.format(DayMonth)}"
            else -> "${start.dayOfMonth} – ${end.format(DayMonth)}"
        }
    }
    return when {
        start == end -> end.format(WeekdayDayMonthYear)
        start.year != end.year -> "${start.format(DayMonthYear)} – ${end.format(DayMonthYear)}"
        start.month != end.month -> "${start.format(DayMonth)} – ${end.format(DayMonthYear)}"
        else -> "${start.dayOfMonth} – ${end.format(DayMonthYear)}"
    }
}
