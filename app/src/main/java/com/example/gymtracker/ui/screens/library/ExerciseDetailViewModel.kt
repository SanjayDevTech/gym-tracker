package com.example.gymtracker.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.ExerciseSetWithDate
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.domain.ExerciseProgress
import com.example.gymtracker.domain.ProgressMetric
import com.example.gymtracker.domain.ProgressPoint
import com.example.gymtracker.domain.ProgressionRule
import com.example.gymtracker.domain.toSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A record's value and the first day it was reached. */
data class RecordFigure(val value: Float, val dateEpochDay: Long)

/** One logged set as the RECENT list shows it. */
data class RecentSet(val repsDone: Int?, val weightKg: Float?, val setType: SetType)

/** One session's completed sets for this exercise (grouped by session, dated). */
data class RecentSession(val dateEpochDay: Long, val sets: List<RecentSet>)

data class ExerciseDetailState(
    /** Null when the exercise does not exist (deleted); the page then shows its top bar only. */
    val exercise: ExerciseEntity?,
    /** Any completed scoring set at all — false shows the empty state. */
    val hasHistory: Boolean,
    /** Any completed working set with load: weight metrics are offered, hero = est. 1RM. */
    val weighted: Boolean,
    val series: Map<ProgressMetric, List<ProgressPoint>>,
    val records: Map<ProgressMetric, RecordFigure>,
    val sessionCount: Int,
    val recent: List<RecentSession>,
    val readyToProgress: Boolean,
    /** The plan target's rep range when the exercise is in exactly one plan; else no band. */
    val repTarget: IntRange?,
    /** Plans this exercise is in — Delete removes it from them (the confirm says how many). */
    val planCount: Int,
)

class ExerciseDetailViewModel(
    private val repository: GymRepository,
    private val exerciseId: Long,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    /**
     * One history query, with dates, feeds everything: charts, records, the grouped RECENT list
     * and the progression check. Null until the first emission.
     */
    val state: StateFlow<ExerciseDetailState?> = combine(
        repository.observeExercise(exerciseId),
        repository.observeExerciseSetsWithDate(exerciseId),
        repository.observeAllPlanned(),
    ) { exercise, history, planned -> build(exercise, history, planned) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Soft delete (custom only — the menu is never shown for built-ins). `logged_sets`
     * references exercises with RESTRICT, so a hard delete isn't possible; history and records
     * still resolve the name by id. The exercise also leaves every plan, in the same transaction.
     * App scope, because the page pops straight after.
     */
    fun delete() {
        val ex = state.value?.exercise ?: return
        if (!ex.isCustom) return
        applicationScope.launch { repository.softDeleteExercise(ex.id) }
    }

    private fun build(
        exercise: ExerciseEntity?,
        history: List<ExerciseSetWithDate>,
        planned: List<PlannedExerciseEntity>,
    ): ExerciseDetailState {
        val samples = history.map { it.toSample() }
        val series = ProgressMetric.entries.associateWith { ExerciseProgress.series(samples, it) }
        // A record's date is the first day the series reached its maximum.
        val records = series.mapNotNull { (metric, points) ->
            val best = points.maxOfOrNull { it.value } ?: return@mapNotNull null
            metric to RecordFigure(best, points.first { it.value == best }.dateEpochDay)
        }.toMap()

        // History is ascending by date, then log time, then set index: grouping keeps both orders.
        val bySession = history.groupBy { it.sessionId }
        val recent = bySession.values.reversed().mapNotNull { sets ->
            val done = sets.filter { it.isCompleted }
            if (done.isEmpty()) null
            else RecentSession(done.first().dateEpochDay, done.map { RecentSet(it.repsDone, it.weightKg, it.setType) })
        }.take(RECENT_SESSIONS)

        val plans = planned.filter { it.exerciseId == exerciseId }
        val planCount = plans.map { it.planId }.distinct().size
        val target = plans.firstOrNull()?.takeIf { planCount == 1 }
        val ready = target != null && ProgressionRule.readyToProgress(
            plan = target,
            recentSetsBySession = bySession.values.reversed().map { sets ->
                sets.mapIndexed { i, s ->
                    LoggedSetEntity(
                        sessionId = s.sessionId,
                        exerciseId = exerciseId,
                        setIndex = i,
                        repsDone = s.repsDone,
                        weightKg = s.weightKg,
                        isCompleted = s.isCompleted,
                        setType = s.setType,
                    )
                }
            },
            n = 2,
        )

        return ExerciseDetailState(
            exercise = exercise,
            hasHistory = series.getValue(ProgressMetric.Reps).isNotEmpty(),
            weighted = series.getValue(ProgressMetric.OneRepMax).isNotEmpty(),
            series = series,
            records = records,
            sessionCount = bySession.size,
            recent = recent,
            readyToProgress = ready,
            repTarget = target?.let { it.targetRepsLow..it.targetRepsHigh },
            planCount = planCount,
        )
    }

    private companion object {
        const val RECENT_SESSIONS = 5
    }
}
