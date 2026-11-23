package com.example.gymtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WindowTest {
    private val today = LocalDate.of(2026, 11, 23)
    private val t = today.toEpochDay()

    @Test
    fun `offset zero ends today`() {
        assertEquals(t..t, AnalyticsWindow.Today.rangeEpochDays(today))
        assertEquals((t - 6)..t, AnalyticsWindow.Week.rangeEpochDays(today))
        assertEquals((t - 29)..t, AnalyticsWindow.Month.rangeEpochDays(today))
        assertEquals((t - 89)..t, AnalyticsWindow.ThreeMonth.rangeEpochDays(today))
        assertEquals((t - 364)..t, AnalyticsWindow.Year.rangeEpochDays(today))
    }

    @Test
    fun `offset steps back whole periods with no gap or overlap`() {
        val current = AnalyticsWindow.Week.rangeEpochDays(today, offset = 0)
        val previous = AnalyticsWindow.Week.rangeEpochDays(today, offset = 1)
        assertEquals(current.start - 1, previous.endInclusive)
        assertEquals((t - 13)..(t - 7), previous)
        assertEquals((t - 1)..(t - 1), AnalyticsWindow.Today.rangeEpochDays(today, offset = 1))
        assertEquals((t - 89)..(t - 60), AnalyticsWindow.Month.rangeEpochDays(today, offset = 2))
    }

    @Test
    fun `negative offset clamps to the current period`() {
        assertEquals(
            AnalyticsWindow.Week.rangeEpochDays(today),
            AnalyticsWindow.Week.rangeEpochDays(today, offset = -3),
        )
    }
}
