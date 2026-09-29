package com.example.gymtracker.domain

import java.time.DayOfWeek
import java.time.LocalDate

object StreakCalc {
    /**
     * Consecutive days walking back from `today` that have either a session OR are rest days
     * in the active program. The streak resets at the first non-rest day with no session.
     *
     * Sundays are skipped entirely — gyms are closed, so a Sunday neither breaks the streak nor
     * counts toward it. A run of Mon–Sat sessions stays an unbroken streak across the Sunday gap.
     */
    fun currentStreak(
        sessionDays: Set<Long>,
        today: Long,
        restDaysByEpochDay: Set<Long> = emptySet(),
    ): Int {
        var streak = 0
        var d = today
        while (true) {
            if (LocalDate.ofEpochDay(d).dayOfWeek == DayOfWeek.SUNDAY) {
                // Closed day — bridge over it without changing the count.
                d -= 1
                continue
            }
            val hasSession = d in sessionDays
            val isRest = d in restDaysByEpochDay
            if (hasSession) {
                streak += 1
            } else if (isRest) {
                if (streak == 0) {
                    // Rest day before any session — doesn't start a streak.
                    return 0
                }
                streak += 1
            } else {
                return streak
            }
            d -= 1
        }
    }
}
