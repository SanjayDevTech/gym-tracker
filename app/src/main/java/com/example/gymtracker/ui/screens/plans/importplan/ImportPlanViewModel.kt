package com.example.gymtracker.ui.screens.plans.importplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.plan.PlanImportExercise
import com.example.gymtracker.data.plan.PlanImportParser
import com.example.gymtracker.data.plan.PlanImportPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Per-source-exercise resolution chosen by the user. */
sealed class Resolution {
    /** Source name matched an existing exercise (or user picked Map). */
    data class Mapped(val exerciseId: Long, val exerciseName: String) : Resolution()
    /** User chose Create custom; values from the create form. */
    data class Create(
        val name: String,
        val movementType: MovementType,
        val primaryMuscles: List<Muscle>,
        val secondaryMuscles: List<Muscle>,
        val equipment: Equipment,
    ) : Resolution()
    /** User chose to skip. */
    data object Skipped : Resolution()
    /** Unresolved — waiting for user. */
    data object Pending : Resolution()
}

data class ImportEntry(
    val source: PlanImportExercise,
    val resolution: Resolution,
)

data class ImportPlanState(
    val rawText: String = "",
    val parseError: String? = null,
    val payload: PlanImportPayload? = null,
    val entries: List<ImportEntry> = emptyList(),
    val committing: Boolean = false,
    val committedPlanId: Long? = null,
)

class ImportPlanViewModel(private val repository: GymRepository) : ViewModel() {

    private val _state = MutableStateFlow(ImportPlanState())
    val state: StateFlow<ImportPlanState> = _state.asStateFlow()

    fun setRawText(text: String) {
        _state.update { it.copy(rawText = text, parseError = null) }
    }

    /** Set the source text and parse in one call (file-picker entry point). */
    fun parseText(text: String) {
        setRawText(text)
        parseAndResolve()
    }

    fun parseAndResolve() {
        val text = _state.value.rawText
        viewModelScope.launch {
            when (val result = PlanImportParser.parse(text)) {
                is PlanImportParser.Result.Error -> {
                    _state.update { it.copy(parseError = result.message, payload = null, entries = emptyList()) }
                }
                is PlanImportParser.Result.Ok -> {
                    val entries = result.payload.exercises.map { src ->
                        val existing = repository.findExerciseByName(src.name.trim())
                        val resolution = if (existing != null) {
                            Resolution.Mapped(existing.id, existing.name)
                        } else {
                            Resolution.Pending
                        }
                        ImportEntry(src, resolution)
                    }
                    _state.update {
                        it.copy(parseError = null, payload = result.payload, entries = entries)
                    }
                }
            }
        }
    }

    fun setResolution(index: Int, resolution: Resolution) {
        _state.update { s ->
            val entries = s.entries.toMutableList()
            if (index in entries.indices) entries[index] = entries[index].copy(resolution = resolution)
            s.copy(entries = entries)
        }
    }

    fun mapEntry(index: Int, exercise: ExerciseEntity) {
        setResolution(index, Resolution.Mapped(exercise.id, exercise.name))
    }

    fun skipEntry(index: Int) {
        setResolution(index, Resolution.Skipped)
    }

    fun unresolveEntry(index: Int) {
        setResolution(index, Resolution.Pending)
    }

    fun commit() {
        val s = _state.value
        val payload = s.payload ?: return
        if (s.entries.any { it.resolution is Resolution.Pending }) return
        if (s.committing) return
        _state.update { it.copy(committing = true) }
        viewModelScope.launch {
            val planId = repository.insertPlan(
                PlanEntity(
                    clientUuid = UUID.randomUUID().toString(),
                    name = payload.name.trim(),
                    notes = payload.notes,
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
            val planned = mutableListOf<PlannedExerciseEntity>()
            var sortIndex = 0
            for (entry in s.entries) {
                val exerciseId: Long = when (val r = entry.resolution) {
                    is Resolution.Mapped -> r.exerciseId
                    is Resolution.Create -> repository.insertExercise(
                        ExerciseEntity(
                            id = 0,
                            clientUuid = UUID.randomUUID().toString(),
                            name = r.name.trim(),
                            movementType = r.movementType,
                            primaryMuscles = r.primaryMuscles,
                            secondaryMuscles = r.secondaryMuscles,
                            equipment = r.equipment,
                            cues = "",
                            isCustom = true,
                            isDeleted = false,
                        )
                    )
                    Resolution.Skipped, Resolution.Pending -> continue
                }
                planned += PlannedExerciseEntity(
                    planId = planId,
                    exerciseId = exerciseId,
                    sortIndex = sortIndex++,
                    targetSets = entry.source.sets.coerceAtLeast(1),
                    targetRepsLow = entry.source.repsLow.coerceAtLeast(1),
                    targetRepsHigh = entry.source.repsHigh.coerceAtLeast(entry.source.repsLow.coerceAtLeast(1)),
                    notes = entry.source.notes,
                )
            }
            if (planned.isNotEmpty()) repository.insertPlannedAll(planned)
            _state.update { it.copy(committing = false, committedPlanId = planId) }
        }
    }
}
