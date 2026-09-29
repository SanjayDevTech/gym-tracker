package com.example.gymtracker.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class LibraryFilters(
    val query: String = "",
    val movementType: MovementType? = null,
    val equipment: Equipment? = null,
    val muscle: Muscle? = null,
)

data class LibraryState(
    val filters: LibraryFilters = LibraryFilters(),
    val exercises: List<ExerciseEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: GymRepository,
) : ViewModel() {
    private val _filters = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = _filters.asStateFlow()

    val state: StateFlow<LibraryState> = _filters
        .flatMapLatest { f ->
            combine(
                repository.searchExercises(f.query.trim()),
                MutableStateFlow(f),
            ) { rows, filt ->
                LibraryState(
                    filters = filt,
                    exercises = rows.filter { row ->
                        (filt.movementType == null || row.movementType == filt.movementType) &&
                            (filt.equipment == null || row.equipment == filt.equipment) &&
                            (filt.muscle == null ||
                                filt.muscle in row.primaryMuscles ||
                                filt.muscle in row.secondaryMuscles)
                    },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryState(),
        )

    fun setQuery(q: String) = _filters.update { it.copy(query = q) }
    fun setMovementType(value: MovementType?) = _filters.update { it.copy(movementType = value) }
    fun setEquipment(value: Equipment?) = _filters.update { it.copy(equipment = value) }
    fun setMuscle(value: Muscle?) = _filters.update { it.copy(muscle = value) }
    fun clearFilters() = _filters.update { LibraryFilters(query = it.query) }
}
