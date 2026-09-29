package com.example.gymtracker.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.domain.ExerciseProgress
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.PersonalRecords
import com.example.gymtracker.domain.ProgressMetric
import com.example.gymtracker.domain.ProgressPoint
import com.example.gymtracker.domain.ProgressionRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class ExerciseDetailState(
    val exercise: ExerciseEntity? = null,
    val recentSets: List<LoggedSetEntity> = emptyList(),
    val readyToProgress: Boolean = false,
    val records: PersonalRecords = PersonalRecords(),
    val seriesByMetric: Map<ProgressMetric, List<ProgressPoint>> = emptyMap(),
    val sessionCount: Int = 0,
    val hasWeightedHistory: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModel(
    private val repository: GymRepository,
    private val exerciseId: Long,
) : ViewModel() {

    private val idFlow = MutableStateFlow(exerciseId)

    val state: StateFlow<ExerciseDetailState> = idFlow
        .flatMapLatest { id ->
            combine(
                repository.observeExercise(id),
                flow { emit(repository.recentSetsForExercise(id, limit = 60)) },
                repository.observeExerciseSetsWithDate(id),
            ) { exercise, sets, history ->
                val samples = history.map {
                    LoggedSetSample(
                        dateEpochDay = it.dateEpochDay,
                        repsDone = it.repsDone,
                        weightKg = it.weightKg,
                        isCompleted = it.isCompleted,
                        setType = it.setType,
                    )
                }
                val series = ProgressMetric.entries.associateWith { ExerciseProgress.series(samples, it) }
                ExerciseDetailState(
                    exercise = exercise,
                    recentSets = sets,
                    readyToProgress = computeReady(sets),
                    records = ExerciseProgress.records(samples),
                    seriesByMetric = series,
                    sessionCount = history.map { it.sessionId }.distinct().size,
                    hasWeightedHistory = samples.any {
                        it.isCompleted && it.setType != SetType.Warmup && (it.weightKg ?: 0f) > 0f
                    },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseDetailState(),
        )

    /** Group flat sets by session, newest first; pass last N to ProgressionRule. */
    private fun computeReady(sets: List<LoggedSetEntity>): Boolean {
        if (sets.isEmpty()) return false
        val grouped = sets.groupBy { it.sessionId }.values.toList()
        // Use a default plan: targetRepsHigh = 12 (the library doesn't carry a plan natively).
        val syntheticPlan = PlannedExerciseEntity(
            id = 0L,
            planId = 0L,
            exerciseId = exerciseId,
            sortIndex = 0,
            targetSets = grouped.firstOrNull()?.size ?: 3,
            targetRepsLow = 8,
            targetRepsHigh = 12,
        )
        return ProgressionRule.readyToProgress(syntheticPlan, grouped, n = 2)
    }
}
