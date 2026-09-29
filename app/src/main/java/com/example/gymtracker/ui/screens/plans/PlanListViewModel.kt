package com.example.gymtracker.ui.screens.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MuscleGroup
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.SessionDraftEntity
import com.example.gymtracker.data.toMuscleGroups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** A plan plus the at-a-glance facts the home card surfaces. */
data class PlanCard(
    val plan: PlanEntity,
    val exerciseCount: Int,
    val muscleGroups: List<MuscleGroup>,
    val lastTrainedEpochDay: Long?,
)

data class PlanListState(
    val plans: List<PlanCard> = emptyList(),
    val inProgressDraft: SessionDraftEntity? = null,
)

class PlanListViewModel(
    private val repository: GymRepository,
) : ViewModel() {

    val state: StateFlow<PlanListState> = combine(
        repository.observePlans(),
        repository.observeAllPlanned(),
        repository.observeExercises(),
        repository.observeSessions(),
        repository.observeSessionDraft(),
    ) { plans, planned, exercises, sessions, draft ->
        val exById = exercises.associateBy { it.id }
        val plannedByPlan = planned.groupBy { it.planId }
        val lastTrainedByPlan = sessions
            .filter { it.planId != null }
            .groupBy { it.planId!! }
            .mapValues { (_, list) -> list.maxOf { it.dateEpochDay } }

        val cards = plans.map { plan ->
            val rows = plannedByPlan[plan.id].orEmpty()
            val muscles = rows.flatMap { exById[it.exerciseId]?.primaryMuscles.orEmpty() }
            PlanCard(
                plan = plan,
                exerciseCount = rows.size,
                muscleGroups = muscles.toMuscleGroups(),
                lastTrainedEpochDay = lastTrainedByPlan[plan.id],
            )
        }
        PlanListState(plans = cards, inProgressDraft = draft)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanListState(),
        )

    fun createPlan(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertPlan(
                PlanEntity(
                    clientUuid = UUID.randomUUID().toString(),
                    name = name.trim().ifBlank { "Untitled plan" },
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
            onCreated(id)
        }
    }

    fun deletePlan(id: Long) {
        viewModelScope.launch { repository.deletePlan(id) }
    }
}
