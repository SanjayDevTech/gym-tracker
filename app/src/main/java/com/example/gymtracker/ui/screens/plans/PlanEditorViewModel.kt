package com.example.gymtracker.ui.screens.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.db.PlanWithExercises
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.SessionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlanEditorViewModel(
    private val repository: GymRepository,
    private val planId: Long,
    sessionDao: SessionDao,
) : ViewModel() {
    private val idFlow = MutableStateFlow(planId)

    val state: StateFlow<PlanWithExercises?> = idFlow
        .flatMapLatest { repository.observePlanWithExercises(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** Median length of this plan's timed sessions; null when none is timed. */
    val typicalDurationMillis: StateFlow<Long?> = sessionDao.observeSessionVolumes()
        .map { rows -> PlanStats.medianDurationMillis(rows.filter { it.planId == planId }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun renamePlan(newName: String) {
        val current = state.value?.plan ?: return
        val trimmed = newName.trim().ifBlank { return }
        if (trimmed == current.name) return
        viewModelScope.launch { repository.updatePlan(current.copy(name = trimmed)) }
    }

    fun updateNotes(newNotes: String) {
        val current = state.value?.plan ?: return
        viewModelScope.launch { repository.updatePlan(current.copy(notes = newNotes)) }
    }

    /** Append the picker's selection, in pick order, with type defaults. */
    fun addExercises(exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        viewModelScope.launch {
            // Read the current tail from the DB, not the (possibly not-yet-collected) state flow.
            val existing = repository.observePlanWithExercises(planId).first()?.planned.orEmpty()
            val firstIndex = (existing.maxOfOrNull { it.planned.sortIndex } ?: -1) + 1
            val rows = exerciseIds.mapIndexed { i, exerciseId ->
                val d = defaultsFor(repository.findExercise(exerciseId)?.movementType ?: MovementType.Compound)
                PlannedExerciseEntity(
                    planId = planId,
                    exerciseId = exerciseId,
                    sortIndex = firstIndex + i,
                    targetSets = d.sets,
                    targetRepsLow = d.low,
                    targetRepsHigh = d.high,
                )
            }
            repository.insertPlannedAll(rows)
        }
    }

    private data class TypeDefaults(val sets: Int, val low: Int, val high: Int)
    private fun defaultsFor(type: MovementType): TypeDefaults = when (type) {
        MovementType.Cardio -> TypeDefaults(sets = 1, low = 20, high = 30)
        MovementType.Mobility -> TypeDefaults(sets = 2, low = 30, high = 60)
        else -> TypeDefaults(sets = 3, low = 8, high = 12)
    }

    fun updatePlanned(planned: PlannedExerciseEntity) {
        viewModelScope.launch { repository.updatePlanned(planned) }
    }

    fun removePlanned(plannedId: Long) {
        viewModelScope.launch { repository.deletePlanned(plannedId) }
    }

    /** Undo of [removePlanned]: re-insert the row as it was (same id and sort index). */
    fun restorePlanned(planned: PlannedExerciseEntity) {
        viewModelScope.launch { repository.insertPlanned(planned) }
    }

    /** Whole-plan deletion; the caller confirms first. Logged sessions are kept.
     *  [onDeleted] runs after the write, so popping the screen cannot cancel it. */
    fun deletePlan(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlan(planId)
            onDeleted()
        }
    }

    /** Persist a new ordering of planned exercises (from drag-and-drop). Only rows whose
     *  sortIndex actually changed are written. */
    fun applyOrder(orderedIds: List<Long>) {
        val byId = state.value?.planned?.map { it.planned }?.associateBy { it.id } ?: return
        viewModelScope.launch {
            orderedIds.forEachIndexed { i, id ->
                val p = byId[id] ?: return@forEachIndexed
                if (p.sortIndex != i) repository.updatePlanned(p.copy(sortIndex = i))
            }
        }
    }
}
