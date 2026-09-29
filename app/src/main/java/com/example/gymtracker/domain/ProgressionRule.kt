package com.example.gymtracker.domain

import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity

object ProgressionRule {
    /**
     * Double-progression readiness:
     * - Warm-up sets are ignored — readiness is judged on working effort only.
     * - All working sets in the last N sessions for this exercise are `isCompleted = true`
     * - Every working set's repsDone >= targetRepsHigh
     * - The most recent session hit the ceiling *without* taking any set to failure — i.e. you
     *   have clean reps in reserve. A failure set in the latest session means you only reached
     *   the top by grinding, so hold the weight and consolidate rather than add load.
     * - Weight is non-decreasing session-over-session (within the last N)
     * Bodyweight exercises (null weightKg throughout) fall back to the rep-only rule.
     *
     * recentSetsBySession: newest session first, each inner list = that session's sets for this exercise.
     */
    fun readyToProgress(
        plan: PlannedExerciseEntity,
        recentSetsBySession: List<List<LoggedSetEntity>>,
        n: Int = 2,
    ): Boolean {
        if (recentSetsBySession.size < n) return false
        // Drop warm-up sets — they're prep, not the working effort we evaluate.
        val recent = recentSetsBySession.take(n).map { session ->
            session.filter { it.setType != SetType.Warmup }
        }

        // Every working set in every session must be completed and hit the rep ceiling.
        val allRepsOk = recent.all { sets ->
            sets.isNotEmpty() && sets.all { set ->
                set.isCompleted && (set.repsDone ?: 0) >= plan.targetRepsHigh
            }
        }
        if (!allRepsOk) return false

        // Failure gate: if the most recent session needed a set taken to failure to hit the
        // ceiling, there's no clean rep-in-reserve yet — hold the weight rather than progress.
        if (recent.first().any { it.setType == SetType.Failure }) return false

        // Bodyweight rule: skip weight check if every set across recent sessions has null weight.
        val anyWeighted = recent.flatten().any { it.weightKg != null }
        if (!anyWeighted) return true

        // Weight non-decreasing across sessions (oldest-to-newest comparison).
        val maxPerSession = recent.map { sets -> sets.maxOfOrNull { it.weightKg ?: 0f } ?: 0f }
        val oldestToNewest = maxPerSession.asReversed()
        return oldestToNewest.zipWithNext().all { (older, newer) -> newer >= older }
    }
}
