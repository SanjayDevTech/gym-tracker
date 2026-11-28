package com.example.gymtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/** The Progress range selector. Rolling windows ending today; "Today" was "Session". */
enum class AnalyticsWindow(val label: String, val days: Int) {
    Today("Today", 1),
    Week("Week", 7),
    Month("Month", 30),
    ThreeMonth("3M", 90),
    Year("Year", 365),
}

/**
 * The inclusive epoch-day range of this window, [offset] whole periods back from the one that
 * ends [today] (0 = the current period, 1 = the one before it, …). Periods are back-to-back:
 * `Week` offset 1 ends the day before offset 0 starts. Negative offsets clamp to 0.
 */
fun AnalyticsWindow.rangeEpochDays(
    today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    offset: Int = 0,
): ClosedRange<Long> {
    val end = today.toEpochDay() - offset.coerceAtLeast(0).toLong() * days
    return (end - (days - 1))..end
}

/**
 * The furthest [rangeEpochDays] offset worth paging back to: the period that holds [firstDay],
 * the earliest logged session. 0 when nothing is logged (or [firstDay] is after [today]).
 */
fun AnalyticsWindow.maxOffset(today: LocalDate, firstDay: Long?): Int {
    if (firstDay == null) return 0
    val back = today.toEpochDay() - firstDay
    return if (back <= 0) 0 else (back / days).toInt()
}
