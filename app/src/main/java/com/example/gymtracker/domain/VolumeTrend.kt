package com.example.gymtracker.domain

import com.example.gymtracker.data.VolumeMetric

/** One ISO week's training volume (Progress bar chart). [changeVsPrior] is null without a base. */
data class WeekVolume(
    val weekStart: Long,
    val total: Float,
    /** Average of the four weeks before this one — the dashed reference for this bar. */
    val priorAverage: Float,
    /** (total − priorAverage) / priorAverage, or null when the four weeks before had nothing. */
    val changeVsPrior: Float?,
)

/**
 * Headline training volume over time — each completed working set counted **once** (no
 * per-muscle split, unlike [MuscleVolume]). Warm-ups and unticked sets never count; tonnage
 * includes failure sets (they are work) and skips bodyweight sets.
 */
object VolumeTrend {
    /** Weeks averaged for the reference line and every "vs 4-wk" figure. */
    const val REFERENCE_WEEKS = 4

    private fun SessionWindowSet.volume(metric: VolumeMetric): Float {
        if (!isCompleted || isWarmup) return 0f
        return when (metric) {
            VolumeMetric.Sets -> 1f
            VolumeMetric.Tonnage -> {
                val reps = repsDone ?: return 0f
                val weight = weightKg ?: return 0f
                reps * weight
            }
        }
    }

    /** Total volume of the sessions dated inside [range]. */
    fun total(sessions: List<SessionWindow>, range: ClosedRange<Long>, metric: VolumeMetric): Float =
        sessions.filter { it.dateEpochDay in range }
            .sumOf { s -> s.sets.sumOf { it.volume(metric).toDouble() } }
            .toFloat()

    /**
     * What [period] "should" hold judged by the four weeks before it: the weekly average of the
     * 28 days preceding the period, scaled to the period's length. Null when those weeks are empty.
     */
    fun referenceFor(sessions: List<SessionWindow>, period: ClosedRange<Long>, metric: VolumeMetric): Float? {
        val days = REFERENCE_WEEKS * 7L
        val before = (period.start - days)..(period.start - 1)
        val weekly = total(sessions, before, metric) / REFERENCE_WEEKS
        if (weekly <= 0f) return null
        val periodDays = period.endInclusive - period.start + 1
        return weekly * periodDays / 7f
    }

    /** Relative change of [value] against [reference]; null when there is no reference. */
    fun change(value: Float, reference: Float?): Float? =
        reference?.takeIf { it > 0f }?.let { (value - it) / it }

    /**
     * One [WeekVolume] per ISO week from the week of [firstEpochDay] to the week of
     * [lastEpochDay], each compared with the average of the four weeks before it. [sessions] must
     * reach four weeks before the first bar for the early bars' comparisons to be complete.
     */
    fun weeklyBars(
        sessions: List<SessionWindow>,
        firstEpochDay: Long,
        lastEpochDay: Long,
        metric: VolumeMetric,
    ): List<WeekVolume> {
        val firstWeek = WeeklyTarget.weekStart(firstEpochDay)
        val lastWeek = WeeklyTarget.weekStart(lastEpochDay)
        if (lastWeek < firstWeek) return emptyList()
        val totals = HashMap<Long, Float>()
        for (s in sessions) {
            val week = WeeklyTarget.weekStart(s.dateEpochDay)
            totals[week] = (totals[week] ?: 0f) + s.sets.sumOf { it.volume(metric).toDouble() }.toFloat()
        }
        return generateSequence(firstWeek) { it + 7 }.takeWhile { it <= lastWeek }.map { week ->
            val prior = (1..REFERENCE_WEEKS).map { totals[week - it * 7L] ?: 0f }.average().toFloat()
            val total = totals[week] ?: 0f
            WeekVolume(week, total, prior, change(total, prior))
        }.toList()
    }
}
