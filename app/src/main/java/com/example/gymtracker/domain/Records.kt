package com.example.gymtracker.domain

import com.example.gymtracker.data.db.ExerciseSetWithDate
import com.example.gymtracker.data.db.LoggedSetEntity

/** The highest-priority record an exercise broke in a session; [delta] vs the baseline (null = first log). */
data class RecordBroken(
    val type: PrType,
    val value: Float,
    val delta: Float?,
)

/** One exercise's records in one session, judged against its pre-session baseline (§6.5). */
data class SessionRecords(
    /** Set index (position in the session's list) → the records that set broke. */
    val prSets: Map<Int, Set<PrType>>,
    /** The best record broken, in [PrType] order; null when none was. */
    val best: RecordBroken?,
) {
    val hasPr: Boolean get() = prSets.isNotEmpty()
}

/**
 * PR detection shared by the logger (live), the Session summary and Session detail. Est. 1RM,
 * heaviest, best set volume, and max reps (bodyweight work only) against the exercise's records
 * from the baseline history — each caller decides which history that is.
 */
object Records {

    /** The bar a session's sets must clear: the all-time records of [history]. */
    fun baseline(history: List<LoggedSetSample>): PersonalRecords = ExerciseProgress.records(history)

    /** Which of [sets] broke a record vs [baseline], and the best record broken. */
    fun forSession(baseline: PersonalRecords, sets: List<LoggedSetSample>): SessionRecords {
        val prSets = ExerciseProgress.sessionPrs(baseline, sets)
        return SessionRecords(prSets, bestOf(prSets, baseline, sets))
    }

    /** [forSession] against the records of [history]. */
    fun forSession(history: List<LoggedSetSample>, sets: List<LoggedSetSample>): SessionRecords =
        forSession(baseline(history), sets)

    /** A session's PR count: exercises with at least one record, not records or sets. */
    fun prCount(exercises: Iterable<SessionRecords>): Int = exercises.count { it.hasPr }

    private fun bestOf(
        prSets: Map<Int, Set<PrType>>,
        baseline: PersonalRecords,
        sets: List<LoggedSetSample>,
    ): RecordBroken? {
        val type = prSets.values.flatten().minByOrNull { it.ordinal } ?: return null
        val session = ExerciseProgress.records(sets)
        val (value, previous) = when (type) {
            PrType.OneRepMax -> session.estimatedOneRepMaxKg to baseline.estimatedOneRepMaxKg
            PrType.Weight -> session.heaviestWeightKg to baseline.heaviestWeightKg
            PrType.Volume -> session.bestSetVolumeKg to baseline.bestSetVolumeKg
            PrType.Reps -> session.maxReps?.toFloat() to baseline.maxReps?.toFloat()
        }
        return RecordBroken(type, value ?: return null, previous?.let { value - it })
    }
}

/** A history row as a record sample. */
fun ExerciseSetWithDate.toSample(): LoggedSetSample =
    LoggedSetSample(dateEpochDay, repsDone, weightKg, isCompleted, setType)

/** A saved set as a record sample; the date doesn't matter within one session. */
fun LoggedSetEntity.toSample(): LoggedSetSample =
    LoggedSetSample(0L, repsDone, weightKg, isCompleted, setType)
