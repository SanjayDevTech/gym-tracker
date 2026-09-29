package com.example.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MuscleGroup
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.db.SessionEntity
import com.example.gymtracker.data.toMuscleGroups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A logged session reduced to the facts a history card shows at a glance. */
data class SessionSummary(
    val session: SessionEntity,
    val exerciseCount: Int,
    val completedSets: Int,
    val tonnage: Float,
    val muscleGroups: List<MuscleGroup>,
    val topExerciseName: String?,
)

data class HistoryDayGroup(
    val dateEpochDay: Long,
    val sessions: List<SessionSummary>,
)

class HistoryViewModel(
    private val repository: GymRepository,
) : ViewModel() {
    val state: StateFlow<List<HistoryDayGroup>> = combine(
        repository.observeAllSessionsWithSets(),
        repository.observeExercises(),
    ) { sessions, exercises ->
        val exById = exercises.associateBy { it.id }
        sessions
            .map { sws ->
                val completed = sws.sets.filter { it.isCompleted }
                val counting = completed.filter { it.setType != SetType.Warmup }
                val tonnage = counting.sumOf { set ->
                    val reps = set.repsDone
                    val w = set.weightKg
                    if (reps != null && w != null) (reps * w).toDouble() else 0.0
                }.toFloat()
                val muscles = completed.flatMap { exById[it.exerciseId]?.primaryMuscles.orEmpty() }
                val topExerciseId = completed
                    .groupingBy { it.exerciseId }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
                SessionSummary(
                    session = sws.session,
                    exerciseCount = completed.map { it.exerciseId }.distinct().size,
                    completedSets = completed.size,
                    tonnage = tonnage,
                    muscleGroups = muscles.toMuscleGroups(),
                    topExerciseName = topExerciseId?.let { exById[it]?.name },
                )
            }
            .groupBy { it.session.dateEpochDay }
            .map { (day, list) -> HistoryDayGroup(day, list) }
            .sortedByDescending { it.dateEpochDay }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
