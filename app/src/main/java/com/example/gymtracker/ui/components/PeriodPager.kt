package com.example.gymtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
 * §2 `PeriodPager` (S6-b): `‹  17 – 23 Nov 2026  ›` — 48dp chevron targets, [AppText.label]
 * centred; `›` is disabled at the current period ([canGoNext] false) and `‹` at the period
 * holding the first logged session ([canGoPrevious] false). The label crossfades 200ms;
 * no horizontal slide (§1 motion).
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
    val colors = IconButtonDefaults.iconButtonColors(
        contentColor = scheme.onSurface,
        disabledContentColor = scheme.outline,
    )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { haptics.light(); onPrevious() }, enabled = canGoPrevious, colors = colors) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous period")
        }
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                fadeIn(tween(200, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(200, easing = FastOutSlowInEasing))
            },
            modifier = Modifier.weight(1f),
            label = "period",
        ) { text ->
            Text(
                text,
                style = AppText.label,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        IconButton(onClick = { haptics.light(); onNext() }, enabled = canGoNext, colors = colors) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next period")
        }
    }
}

private val DayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy")
private val DayMonth = DateTimeFormatter.ofPattern("d MMM")
private val WeekdayDayMonthYear = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/**
 * `17 – 23 Nov 2026`, `27 Oct – 23 Nov 2026`, `20 Dec 2025 – 18 Jan 2026`; a one-day range reads
 * `Mon 23 Nov 2026`. Dashes are en dashes with spaces, as in the plan.
 */
fun periodLabel(range: ClosedRange<Long>): String {
    val start = LocalDate.ofEpochDay(range.start)
    val end = LocalDate.ofEpochDay(range.endInclusive)
    return when {
        start == end -> end.format(WeekdayDayMonthYear)
        start.year != end.year -> "${start.format(DayMonthYear)} – ${end.format(DayMonthYear)}"
        start.month != end.month -> "${start.format(DayMonth)} – ${end.format(DayMonthYear)}"
        else -> "${start.dayOfMonth} – ${end.format(DayMonthYear)}"
    }
}
