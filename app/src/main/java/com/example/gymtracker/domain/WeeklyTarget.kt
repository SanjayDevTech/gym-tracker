package com.example.gymtracker.domain

/**
 * The weekly target — "3 of 4 this week", "on target 9 of 12 weeks". Replaces the
 * daily streak, which punished rest days and read 0 every morning before the day's workout.
 *
 * Weeks are ISO weeks, Monday start, keyed by the epoch day of their Monday. Every function takes
 * session dates as epoch days, **one entry per session** (two sessions on one day are two towards
 * the target), so callers pass `sessions.map { it.dateEpochDay }` unchanged.
 */
object WeeklyTarget {
    const val MIN_TARGET = 1
    const val MAX_TARGET = 7
    const val DEFAULT_TARGET = 3

    /** Epoch day of the Monday that starts [epochDay]'s ISO week. */
    fun weekStart(epochDay: Long): Long = DateUtil.startOfWeek(epochDay)

    /** Sessions logged in the ISO week containing [weekOf] (default: this week). */
    fun sessionsInWeek(sessionDays: Collection<Long>, weekOf: Long = DateUtil.todayEpochDay()): Int {
        val start = weekStart(weekOf)
        return sessionDays.count { it in start until start + 7 }
    }

    /** Monday of every week whose session count reached [target] — e.g. to mark weeks on a
     *  calendar. A week still in progress is included as soon as it has reached the target. */
    fun weeksMeetingTarget(sessionDays: Collection<Long>, target: Int): Set<Long> =
        sessionDays.groupingBy(::weekStart).eachCount()
            .filterValues { it >= target }
            .keys

    /**
     * How many of the last [last] **completed** weeks met [target]. The current week is not
     * counted — it cannot have failed yet — and weeks before the first logged session are not
     * counted either, so a new user reads "2 of 3", not "2 of 12".
     *
     * [WeeksOnTarget.of] is 0 until a full week has passed since the first session; show "—"
     * then.
     */
    fun weeksOnTarget(
        sessionDays: Collection<Long>,
        target: Int,
        today: Long = DateUtil.todayEpochDay(),
        last: Int = 12,
    ): WeeksOnTarget {
        val thisWeek = weekStart(today)
        val firstWeek = sessionDays.minOrNull()?.let(::weekStart) ?: return WeeksOnTarget(0, 0)
        val completedSinceFirst = ((thisWeek - firstWeek) / 7).toInt().coerceAtLeast(0)
        val of = minOf(last, completedSinceFirst)
        if (of == 0) return WeeksOnTarget(0, 0)
        val windowStart = thisWeek - of * 7L
        val met = weeksMeetingTarget(sessionDays, target).count { it in windowStart until thisWeek }
        return WeeksOnTarget(met = met, of = of)
    }

    /**
     * Longest run of consecutive weeks that met [target], over all history. The current week
     * joins a run only once it has met the target.
     */
    fun bestRunOfWeeks(sessionDays: Collection<Long>, target: Int): Int {
        val met = weeksMeetingTarget(sessionDays, target).sorted()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (week in met) {
            run = if (previous != null && week - previous == 7L) run + 1 else 1
            best = maxOf(best, run)
            previous = week
        }
        return best
    }
}

/**
 * Weeks in a row on target, ending with the last completed week — or with this week once it has
 * met the target, so the run never drops to zero just because a new week began.
 */
fun WeeklyTarget.currentRunOfWeeks(
    sessionDays: Collection<Long>,
    target: Int,
    today: Long = DateUtil.todayEpochDay(),
): Int {
    val met = weeksMeetingTarget(sessionDays, target)
    var week = weekStart(today)
    if (week !in met) week -= 7
    var run = 0
    while (week in met) {
        run++
        week -= 7
    }
    return run
}

/** [met] of the last [of] completed weeks reached the target. [of] = 0 means "not enough history". */
data class WeeksOnTarget(val met: Int, val of: Int)
