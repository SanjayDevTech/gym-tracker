package com.example.gymtracker.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.SectionHead
import com.example.gymtracker.ui.format.formatDuration
import com.example.gymtracker.ui.format.formatVolume
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * History — §4.8, bar destination 2 (no back arrow). Sessions by ISO week: a standing head per
 * week with its session count, then one row per session (day eyebrow · plan name · meta ·
 * trailing set count). Bar screen: no FAB, so no bottom clearance; the NavHost owns the bar inset.
 */
@Composable
fun HistoryScreen(onOpenSession: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: HistoryViewModel = viewModel(
        factory = AppViewModelFactory(
            mapOf(HistoryViewModel::class.java to {
                HistoryViewModel(container.repository, container.prefs)
            })
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar(title = "History") },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        // House rule 8: top bar only until the first emission.
        val s = state ?: return@Scaffold
        if (s.weeks.isEmpty()) {
            EmptyState(
                title = "Nothing logged yet",
                message = "Finished workouts land here.",
                modifier = Modifier.padding(padding).padding(horizontal = Spacing.gutter),
            )
            return@Scaffold
        }
        val today = DateUtil.todayEpochDay()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.s,
                bottom = Spacing.l,
            ),
        ) {
            s.weeks.forEachIndexed { weekIndex, week ->
                item(key = "week-${week.weekStartEpochDay}", contentType = "week") {
                    WeekHead(
                        weekStart = week.weekStartEpochDay,
                        today = today,
                        count = week.sessions.size,
                        modifier = if (weekIndex == 0) Modifier else Modifier.padding(top = Spacing.section),
                    )
                }
                week.sessions.forEachIndexed { i, row ->
                    // One day eyebrow per day, even when a day holds two sessions.
                    val showDay = i == 0 || week.sessions[i - 1].dateEpochDay != row.dateEpochDay
                    item(key = row.sessionId, contentType = "session") {
                        Column {
                            if (i > 0) AppDivider()
                            SessionRow(
                                row = row,
                                dayLabel = if (showDay) dayLabel(row.dateEpochDay, today) else null,
                                unit = s.unit,
                                onClick = { onOpenSession(row.sessionId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHead(weekStart: Long, today: Long, count: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SectionHead(
            title = weekLabel(weekStart, today),
            trailing = {
                Text(
                    "$count ${if (count == 1) "session" else "sessions"}",
                    style = AppText.bodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Spacer(Modifier.height(Spacing.s))
        AppDivider()
    }
}

@Composable
private fun SessionRow(
    row: HistoryRow,
    dayLabel: String?,
    unit: WeightUnit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open session", onClick = onClick)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            if (dayLabel != null) {
                Eyebrow(dayLabel)
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(
                row.planName ?: "Quick log",
                style = AppText.titleM,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(row.metaLine(unit), style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Spacing.m))
        Column(horizontalAlignment = Alignment.End) {
            Text("${row.doneSets}", style = AppText.numberL, color = scheme.onSurface)
            Text(
                if (row.doneSets == 1) "set" else "sets",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * `5 exercises · 41 min · 8 420 kg`. Duration is left out for sessions logged before v5 (§4.8:
 * "omit the duration"), volume when there is none (cardio, bodyweight-only).
 */
private fun HistoryRow.metaLine(unit: WeightUnit): String = buildList {
    add("$exerciseCount ${if (exerciseCount == 1) "exercise" else "exercises"}")
    durationMillis?.let { add(formatDuration(it)) }
    if (volumeKg > 0.0) add(formatVolume(volumeKg, unit))
}.joinToString(" · ")

/** `This week`, `Last week`, else the range: `10 – 16 Nov`, with the year when it isn't this one. */
private fun weekLabel(weekStart: Long, today: Long): String {
    val thisWeek = WeeklyTarget.weekStart(today)
    return when (weekStart) {
        thisWeek -> "This week"
        thisWeek - 7 -> "Last week"
        else -> {
            val start = LocalDate.ofEpochDay(weekStart)
            val end = start.plusDays(6)
            val withYear = end.year != LocalDate.ofEpochDay(today).year
            val endPattern = if (withYear) "d MMM yyyy" else "d MMM"
            val startPattern = if (start.month == end.month) "d" else "d MMM"
            "${start.format(fmt(startPattern))} – ${end.format(fmt(endPattern))}"
        }
    }
}

/** `Today`, `Yesterday`, else `Sunday 21`. */
private fun dayLabel(epochDay: Long, today: Long): String = when (today - epochDay) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> LocalDate.ofEpochDay(epochDay).format(fmt("EEEE d"))
}

private fun fmt(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
