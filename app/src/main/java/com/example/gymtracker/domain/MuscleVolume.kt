package com.example.gymtracker.domain

import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.VolumeMetric

data class SessionWindowSet(
    val exerciseId: Long,
    val repsDone: Int?,
    val weightKg: Float?,
    val isCompleted: Boolean,
    /** Warm-up sets are excluded from volume/tonnage — they're prep, not training stimulus. */
    val isWarmup: Boolean = false,
)

data class SessionWindow(
    val dateEpochDay: Long,
    val sets: List<SessionWindowSet>,
)

data class ExerciseMuscleMap(
    val primary: List<Muscle>,
    val secondary: List<Muscle>,
)

object MuscleVolume {
    /** Share of a set's volume credited to each secondary muscle (primary muscles get 1.0). */
    const val SECONDARY_WEIGHT = 0.3f

    /**
     * Per-muscle volume over completed working sets within the window, measured by [metric]:
     *
     * - [VolumeMetric.Sets]: each completed working set contributes 1.0 (primary) / [SECONDARY_WEIGHT] (secondary),
     *   regardless of load — so bodyweight work counts fully.
     * - [VolumeMetric.Tonnage]: each set contributes repsDone × weightKg × (1.0 | [SECONDARY_WEIGHT]); sets with a
     *   null repsDone or null weightKg (bodyweight) contribute 0 and are surfaced separately via
     *   [bodyweightSetCountsForWindow].
     *
     * Warm-up and incomplete sets never contribute under either metric.
     */
    fun computeForWindow(
        sessions: List<SessionWindow>,
        windowEpochDays: ClosedRange<Long>,
        muscleMap: Map<Long, ExerciseMuscleMap>,
        metric: VolumeMetric = VolumeMetric.Sets,
    ): Map<Muscle, Float> {
        val acc = mutableMapOf<Muscle, Float>()
        for (s in sessions) {
            if (s.dateEpochDay !in windowEpochDays) continue
            for (set in s.sets) {
                if (!set.isCompleted) continue
                if (set.isWarmup) continue
                val map = muscleMap[set.exerciseId] ?: continue
                val value: Float = when (metric) {
                    VolumeMetric.Sets -> 1f
                    VolumeMetric.Tonnage -> {
                        val reps = set.repsDone ?: continue
                        val weight = set.weightKg ?: continue
                        reps * weight
                    }
                }
                map.primary.forEach { m -> acc[m] = (acc[m] ?: 0f) + value }
                map.secondary.forEach { m -> acc[m] = (acc[m] ?: 0f) + value * SECONDARY_WEIGHT }
            }
        }
        return acc
    }

    fun bodyweightSetCountsForWindow(
        sessions: List<SessionWindow>,
        windowEpochDays: ClosedRange<Long>,
        muscleMap: Map<Long, ExerciseMuscleMap>,
    ): Map<Muscle, Int> {
        val acc = mutableMapOf<Muscle, Int>()
        for (s in sessions) {
            if (s.dateEpochDay !in windowEpochDays) continue
            for (set in s.sets) {
                if (!set.isCompleted) continue
                if (set.isWarmup) continue
                if (set.weightKg != null) continue
                val map = muscleMap[set.exerciseId] ?: continue
                map.primary.forEach { m -> acc[m] = (acc[m] ?: 0) + 1 }
                map.secondary.forEach { m -> acc[m] = (acc[m] ?: 0) + 1 }
            }
        }
        return acc
    }

    /**
     * The weekly-volume guideline band (decision 11): 10–20 working sets per muscle per week.
     * Shown on Progress only when the range is WEEK.
     */
    const val GUIDELINE_LOW_SETS = 10
    const val GUIDELINE_HIGH_SETS = 20

    /**
     * Working sets per muscle per week over [windowEpochDays] — [VolumeMetric.Sets] volume
     * (secondary muscles credited [SECONDARY_WEIGHT]) divided by the window's length in weeks.
     * For a 7-day window this is simply that week's working sets, the figure the 10–20 band
     * reads against.
     */
    fun weeklyWorkingSets(
        sessions: List<SessionWindow>,
        windowEpochDays: ClosedRange<Long>,
        muscleMap: Map<Long, ExerciseMuscleMap>,
    ): Map<Muscle, Float> {
        val days = (windowEpochDays.endInclusive - windowEpochDays.start + 1).coerceAtLeast(1L)
        val weeks = days / 7f
        return computeForWindow(sessions, windowEpochDays, muscleMap, VolumeMetric.Sets)
            .mapValues { it.value / weeks }
    }

    /** Where a weekly set count sits against the 10–20 band, judged on the rounded figure shown. */
    fun guideline(weeklySets: Float): Guideline {
        val shown = kotlin.math.round(weeklySets).toInt()
        return when {
            shown < GUIDELINE_LOW_SETS -> Guideline.Below
            shown > GUIDELINE_HIGH_SETS -> Guideline.Above
            else -> Guideline.Within
        }
    }

    enum class Guideline { Below, Within, Above }

    /** Normalize values to 0..1 by dividing by the max in the map. */
    fun normalize(values: Map<Muscle, Float>): Map<Muscle, Float> {
        val max = values.values.maxOrNull() ?: 0f
        if (max <= 0f) return emptyMap()
        return values.mapValues { (it.value / max).coerceIn(0f, 1f) }
    }
}
