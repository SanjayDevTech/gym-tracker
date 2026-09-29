package com.example.gymtracker.domain

import java.time.LocalDate
import java.time.ZoneId

enum class AnalyticsWindow(val label: String) {
    Session("Session"),
    Week("Week"),
    Month("Month"),
    ThreeMonth("3M"),
    Year("Year"),
}

/** Returns the inclusive epoch-day range [start..today] for this window, anchored at today. */
fun AnalyticsWindow.rangeEpochDays(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): ClosedRange<Long> {
    val end = today.toEpochDay()
    val start = when (this) {
        AnalyticsWindow.Session -> end                          // today only
        AnalyticsWindow.Week -> today.minusDays(6).toEpochDay()
        AnalyticsWindow.Month -> today.minusDays(29).toEpochDay()
        AnalyticsWindow.ThreeMonth -> today.minusDays(89).toEpochDay()
        AnalyticsWindow.Year -> today.minusDays(364).toEpochDay()
    }
    return start..end
}
