package com.example.gymtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyTargetTest {
    // Monday 23 Nov 2026 starts the "current" week in these tests.
    private val monday = LocalDate.of(2026, 11, 23).toEpochDay()
    private val sunday = monday + 6
    private fun weeksAgo(n: Int, dayOffset: Int = 0) = monday - 7L * n + dayOffset

    @Test
    fun weekStartIsMonday() {
        assertEquals(monday, WeeklyTarget.weekStart(monday))
        assertEquals(monday, WeeklyTarget.weekStart(sunday))
        assertEquals(monday - 7, WeeklyTarget.weekStart(monday - 1)) // the Sunday before
    }

    @Test
    fun sessionsInWeekCountsSessionsNotDays() {
        val days = listOf(monday, monday, monday + 3, sunday, monday - 1, sunday + 1)
        assertEquals(4, WeeklyTarget.sessionsInWeek(days, weekOf = monday + 2))
        assertEquals(1, WeeklyTarget.sessionsInWeek(days, weekOf = monday - 1))
        assertEquals(0, WeeklyTarget.sessionsInWeek(emptyList(), weekOf = monday))
    }

    @Test
    fun weeksMeetingTargetIncludesAWeekInProgressOnceMet() {
        val days = listOf(weeksAgo(1), weeksAgo(1, 2), weeksAgo(2), monday, monday + 1)
        assertEquals(setOf(weeksAgo(1), monday), WeeklyTarget.weeksMeetingTarget(days, target = 2))
    }

    @Test
    fun weeksOnTargetIsEmptyUntilAFullWeekHasPassed() {
        assertEquals(WeeksOnTarget(0, 0), WeeklyTarget.weeksOnTarget(emptyList(), 3, today = monday))
        val thisWeekOnly = listOf(monday, monday + 1, monday + 2)
        assertEquals(WeeksOnTarget(0, 0), WeeklyTarget.weeksOnTarget(thisWeekOnly, 3, today = sunday))
    }

    @Test
    fun weeksOnTargetExcludesTheCurrentWeekAndWeeksBeforeTheFirstSession() {
        val days = listOf(
            weeksAgo(3), weeksAgo(3, 2), // met (2)
            weeksAgo(2),                 // missed
            weeksAgo(1), weeksAgo(1, 4), // met
            monday, monday + 1,          // met, but in progress: not counted
        )
        assertEquals(WeeksOnTarget(met = 2, of = 3), WeeklyTarget.weeksOnTarget(days, 2, today = monday + 3))
    }

    @Test
    fun weeksOnTargetLooksBackOnlyLastWeeks() {
        // Two sessions every week for 20 weeks, the week 15 back missed.
        val days = (1..20).flatMap { n -> if (n == 15) listOf(weeksAgo(n)) else listOf(weeksAgo(n), weeksAgo(n, 1)) }
        assertEquals(WeeksOnTarget(12, 12), WeeklyTarget.weeksOnTarget(days, 2, today = monday))
        assertEquals(WeeksOnTarget(19, 20), WeeklyTarget.weeksOnTarget(days, 2, today = monday, last = 52))
        assertEquals(WeeksOnTarget(4, 4), WeeklyTarget.weeksOnTarget(days, 2, today = monday, last = 4))
    }

    @Test
    fun bestRunOfWeeks() {
        assertEquals(0, WeeklyTarget.bestRunOfWeeks(emptyList(), 1))
        val days = listOf(
            weeksAgo(6), weeksAgo(5), weeksAgo(4), // run of 3
            weeksAgo(2), weeksAgo(1), monday,      // run of 3 including this week
            weeksAgo(8),                           // isolated
        )
        assertEquals(3, WeeklyTarget.bestRunOfWeeks(days, 1))
        assertEquals(0, WeeklyTarget.bestRunOfWeeks(days, 2))
        // A run crossing a year boundary stays consecutive (ISO weeks, not week-of-year numbers).
        val newYear = LocalDate.of(2026, 12, 28).toEpochDay() // Monday
        assertEquals(3, WeeklyTarget.bestRunOfWeeks(listOf(newYear - 7, newYear + 3, newYear + 7), 1))
    }
}
