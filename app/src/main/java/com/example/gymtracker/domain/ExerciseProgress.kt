package com.example.gymtracker.domain

import com.example.gymtracker.data.SetType

/** A single logged set tagged with the day it was performed — the input to PR + progress math. */
data class LoggedSetSample(
    val dateEpochDay: Long,
    val repsDone: Int?,
    val weightKg: Float?,
    val isCompleted: Boolean,
    val setType: SetType,
)

/** All-time bests for one exercise. Null where there's no qualifying history. */
data class PersonalRecords(
    val estimatedOneRepMaxKg: Float? = null,
    val heaviestWeightKg: Float? = null,
    val bestSetVolumeKg: Float? = null,
    val maxReps: Int? = null,
)

/** The metric a progress chart plots over time. */
enum class ProgressMetric(val label: String) {
    OneRepMax("Est. 1RM"),
    Heaviest("Heaviest"),
    Volume("Volume"),
    Reps("Reps"),
}

/** One charted point: the best [value] reached on [dateEpochDay]. */
data class ProgressPoint(val dateEpochDay: Long, val value: Float)

/** A kind of record a set can break, in display-priority order (highest first). */
enum class PrType(val label: String) {
    OneRepMax("1RM"),
    Weight("Weight"),
    Volume("Volume"),
    Reps("Reps"),
}

object OneRepMax {
    /**
     * Epley estimate: 1RM ≈ w × (1 + reps/30). A single rep is its own 1RM; higher rep sets
     * extrapolate upward. The standard formula used by Hevy/Strong-style trackers.
     */
    fun epley(weightKg: Float, reps: Int): Float =
        if (reps <= 1) weightKg else weightKg * (1f + reps / 30f)
}

object ExerciseProgress {

    /** Sets that count toward records/progress: completed, not a warm-up, with real reps. */
    private fun List<LoggedSetSample>.scoring(): List<LoggedSetSample> =
        filter { it.isCompleted && it.setType != SetType.Warmup && (it.repsDone ?: 0) > 0 }

    private val LoggedSetSample.isWeighted: Boolean
        get() = (weightKg ?: 0f) > 0f

    fun records(sets: List<LoggedSetSample>): PersonalRecords {
        val scoring = sets.scoring()
        if (scoring.isEmpty()) return PersonalRecords()
        val weighted = scoring.filter { it.isWeighted }
        return PersonalRecords(
            estimatedOneRepMaxKg = weighted.maxOfOrNull { OneRepMax.epley(it.weightKg!!, it.repsDone!!) },
            heaviestWeightKg = weighted.maxOfOrNull { it.weightKg!! },
            bestSetVolumeKg = weighted.maxOfOrNull { it.weightKg!! * it.repsDone!! },
            maxReps = scoring.maxOfOrNull { it.repsDone!! },
        )
    }

    /**
     * One point per session date = the best value of [metric] achieved that day, ascending by date.
     * Weight-based metrics ignore bodyweight sets; [ProgressMetric.Reps] counts every scoring set.
     */
    fun series(sets: List<LoggedSetSample>, metric: ProgressMetric): List<ProgressPoint> {
        val scoring = when (metric) {
            ProgressMetric.Reps -> sets.scoring()
            else -> sets.scoring().filter { it.isWeighted }
        }
        if (scoring.isEmpty()) return emptyList()
        return scoring.groupBy { it.dateEpochDay }
            .map { (day, daySets) ->
                ProgressPoint(
                    dateEpochDay = day,
                    value = daySets.maxOf { s ->
                        when (metric) {
                            ProgressMetric.OneRepMax -> OneRepMax.epley(s.weightKg!!, s.repsDone!!)
                            ProgressMetric.Heaviest -> s.weightKg!!
                            ProgressMetric.Volume -> s.weightKg!! * s.repsDone!!
                            ProgressMetric.Reps -> s.repsDone!!.toFloat()
                        }
                    },
                )
            }
            .sortedBy { it.dateEpochDay }
    }

    /**
     * Live PR detection for the set logger. Given the pre-session [baseline] records and the
     * current session's [sets] for one exercise (index = set position), returns which set earns
     * each [PrType] — the single best scoring set that strictly beats the baseline. At most one
     * set per type; a set may win several. An empty/absent baseline means any first scoring set
     * is a record (all-time first log), matching Hevy-style behaviour.
     */
    fun sessionPrs(baseline: PersonalRecords, sets: List<LoggedSetSample>): Map<Int, Set<PrType>> {
        val scoring = sets.withIndex().filter { (_, s) ->
            s.isCompleted && s.setType != SetType.Warmup && (s.repsDone ?: 0) > 0
        }
        if (scoring.isEmpty()) return emptyMap()

        val result = HashMap<Int, MutableSet<PrType>>()
        fun award(type: PrType, baselineValue: Float?, valueOf: (LoggedSetSample) -> Float?) {
            val best = scoring
                .mapNotNull { (i, s) -> valueOf(s)?.let { i to it } }
                .maxByOrNull { it.second } ?: return
            if (baselineValue == null || best.second > baselineValue) {
                result.getOrPut(best.first) { mutableSetOf() }.add(type)
            }
        }

        fun LoggedSetSample.weightedKg(): Float? = weightKg?.takeIf { it > 0f }

        award(PrType.OneRepMax, baseline.estimatedOneRepMaxKg) { s ->
            s.weightedKg()?.let { OneRepMax.epley(it, s.repsDone!!) }
        }
        award(PrType.Weight, baseline.heaviestWeightKg) { s -> s.weightedKg() }
        award(PrType.Volume, baseline.bestSetVolumeKg) { s -> s.weightedKg()?.let { it * s.repsDone!! } }
        // A reps PR is only meaningful when the work is bodyweight — otherwise the weight metrics
        // carry the signal and a "reps" crown would just be noise.
        if (scoring.none { it.value.weightedKg() != null }) {
            award(PrType.Reps, baseline.maxReps?.toFloat()) { s -> s.repsDone!!.toFloat() }
        }
        return result
    }
}
