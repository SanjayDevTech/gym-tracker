package com.example.gymtracker.ui.screens.plans

import com.example.gymtracker.data.MuscleGroup
import com.example.gymtracker.data.db.SessionVolume
import com.example.gymtracker.ui.format.formatDuration
import com.example.gymtracker.ui.format.plural

/**
 * Pure folds over [SessionVolume] rows shared by the plan list and the Plan editor.
 * Kept free of Android types so they can be unit-tested.
 */
object PlanStats {
    /**
     * A plan's one-line caption, shared by the Train row and the Plan editor header:
     * `6 exercises · 18 sets · Chest, Shoulders · ~52 min`. [sets] and [muscles] are optional
     * (the row shows muscles, the editor sets); at most two muscle groups are named.
     */
    fun metaLine(
        exercises: Int,
        typicalMillis: Long?,
        sets: Int? = null,
        muscles: List<MuscleGroup> = emptyList(),
    ): String = buildList {
        add(if (exercises == 0) "No exercises" else "$exercises ${plural(exercises, "exercise")}")
        sets?.let { add("$it ${plural(it, "set")}") }
        if (muscles.isNotEmpty()) add(muscles.take(2).joinToString(", ") { it.label })
        typicalMillis?.let { add("~" + formatDuration(it)) }
    }.joinToString(" · ")

    /** How many recent sessions a plan row's sparkline draws. */
    const val SPARK_POINTS = 8

    /**
     * Median wall-clock length of the timed sessions in [sessions]. Sessions logged
     * before v5 have no start time and are ignored; null when none is timed. An even count
     * averages the middle two.
     */
    fun medianDurationMillis(sessions: Collection<SessionVolume>): Long? {
        val durations = sessions
            .mapNotNull { s -> s.startedAtMillis?.let { s.loggedAtMillis - it } }
            .filter { it >= 0 }
            .sorted()
        if (durations.isEmpty()) return null
        val mid = durations.size / 2
        return if (durations.size % 2 == 1) durations[mid] else (durations[mid - 1] + durations[mid]) / 2
    }

    /** The last [SPARK_POINTS] session volumes per plan, oldest first. Rows arrive oldest first. */
    fun recentVolumesByPlan(sessions: List<SessionVolume>): Map<Long, List<Double>> =
        sessions
            .filter { it.planId != null }
            .groupBy { it.planId!! }
            .mapValues { (_, list) ->
                list.sortedWith(compareBy({ it.dateEpochDay }, { it.loggedAtMillis }))
                    .takeLast(SPARK_POINTS)
                    .map { it.volumeKg }
            }

    /** Volume and completed sets over the 7 days ending [today] (inclusive). */
    fun lastSevenDays(sessions: Collection<SessionVolume>, today: Long): Pair<Double, Int> {
        val window = sessions.filter { it.dateEpochDay in (today - 6)..today }
        return window.sumOf { it.volumeKg } to window.sumOf { it.doneSets }
    }
}
