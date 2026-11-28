package com.example.gymtracker.ui.screens.exerciseform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.CustomExerciseFields
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.plan.PlanImportExercise
import com.example.gymtracker.domain.ExerciseNameRule
import com.example.gymtracker.ui.navigation.ExerciseFormRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The form's working copy. Muscles are sets in the UI; the entity stores lists. */
data class ExerciseFormFields(
    val name: String = "",
    val movementType: MovementType = MovementType.Compound,
    val equipment: Equipment = Equipment.Other,
    val primary: Set<Muscle> = emptySet(),
    val secondary: Set<Muscle> = emptySet(),
) {
    fun toCustom(name: String) = CustomExerciseFields(
        name = name,
        movementType = movementType,
        equipment = equipment,
        // Keep the enum's anatomical order, not tap order.
        primaryMuscles = Muscle.entries.filter { it in primary },
        secondaryMuscles = Muscle.entries.filter { it in secondary },
    )
}

/** What the save did: [created] distinguishes a new exercise (whose id flows back) from an edit. */
data class SavedExercise(val id: Long, val created: Boolean)

/**
 * Custom exercise form. [exerciseId] null = create from [prefill]; otherwise edit, which
 * loads the row and refuses built-in or deleted ones ([blocked]).
 */
class ExerciseFormViewModel(
    private val repository: GymRepository,
    private val exerciseId: Long?,
    prefill: ExerciseFormFields,
) : ViewModel() {

    val editing: Boolean = exerciseId != null

    /** Null while an edit loads (top bar only). Bound to the fields directly so the cursor never jumps. */
    private val _fields = MutableStateFlow(if (exerciseId == null) prefill else null)
    val fields: StateFlow<ExerciseFormFields?> = _fields.asStateFlow()

    /** Non-deleted exercises, for the duplicate check. Null until the first emission. */
    val existing = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _blocked = MutableStateFlow(false)
    /** The exercise to edit is built-in, deleted or gone — never editable; the page closes. */
    val blocked: StateFlow<Boolean> = _blocked.asStateFlow()

    private val _saved = MutableStateFlow<SavedExercise?>(null)
    val saved: StateFlow<SavedExercise?> = _saved.asStateFlow()

    private var saving = false

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                val ex = repository.findExercise(exerciseId)
                if (ex == null || !ex.isCustom || ex.isDeleted) {
                    _blocked.value = true
                } else {
                    _fields.value = ExerciseFormFields(
                        name = ex.name,
                        movementType = ex.movementType,
                        equipment = ex.equipment,
                        primary = ex.primaryMuscles.toSet(),
                        secondary = ex.secondaryMuscles.toSet(),
                    )
                }
            }
        }
    }

    fun validate(f: ExerciseFormFields, all: List<ExerciseEntity>) =
        ExerciseNameRule.validate(f.name, all, exerciseId)

    fun edit(transform: (ExerciseFormFields) -> ExerciseFormFields) {
        _fields.update { it?.let(transform) }
    }

    fun save() {
        val f = _fields.value ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            // Re-check against a fresh read: the list behind the inline error may be a frame old.
            val result = validate(f, repository.observeExercises().first())
            if (result !is ExerciseNameRule.Result.Ok) {
                saving = false
                return@launch
            }
            val fields = f.toCustom(result.name)
            _saved.value = if (exerciseId == null) {
                SavedExercise(repository.createCustomExercise(fields), created = true)
            } else {
                if (!repository.updateCustom(exerciseId, fields)) {
                    _blocked.value = true
                    return@launch
                }
                SavedExercise(exerciseId, created = false)
            }
        }
    }
}

/** The route's prefill as form fields; unknown enum names fall back to the defaults. */
fun ExerciseFormRoute.prefill(): ExerciseFormFields = ExerciseFormFields(
    name = prefillName.orEmpty().trim(),
    movementType = prefillType.toEnum<MovementType>() ?: MovementType.Compound,
    equipment = prefillEquipment.toEnum<Equipment>() ?: Equipment.Other,
    primary = prefillPrimary.toMuscles(),
    secondary = prefillSecondary.toMuscles(),
)

/** Import's Create: the source entry's name and optional fields as a prefill. */
fun exerciseFormRouteFor(source: PlanImportExercise) = ExerciseFormRoute(
    prefillName = source.name,
    prefillType = source.movementType,
    prefillEquipment = source.equipment,
    prefillPrimary = source.primaryMuscles.joinToString(","),
    prefillSecondary = source.secondaryMuscles.joinToString(","),
)

private inline fun <reified E : Enum<E>> String?.toEnum(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name.trim() } }

private fun String?.toMuscles(): Set<Muscle> =
    this.orEmpty().split(',').mapNotNull { it.toEnum<Muscle>() }.toSet()
