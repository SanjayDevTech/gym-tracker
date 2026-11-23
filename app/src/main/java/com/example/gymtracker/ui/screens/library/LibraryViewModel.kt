package com.example.gymtracker.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.ui.components.ExerciseFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LibraryState(
    val filters: ExerciseFilters,
    val exercises: List<ExerciseEntity>,
    /** Every exercise in the library, before search and filters. */
    val total: Int,
)

class LibraryViewModel(
    repository: GymRepository,
) : ViewModel() {
    /** Bound to the search field synchronously so the cursor never jumps (state lags a frame). */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val filters = MutableStateFlow(ExerciseFilters())

    /** Null until the first Room emission (house rule 8). */
    val state: StateFlow<LibraryState?> = combine(
        repository.observeExercises(),
        _query,
        filters,
    ) { all, q, f ->
        val needle = q.trim()
        LibraryState(
            filters = f,
            exercises = all.filter { f.matches(it) && (needle.isEmpty() || it.name.contains(needle, ignoreCase = true)) },
            total = all.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(q: String) { _query.value = q }
    fun setFilters(value: ExerciseFilters) { filters.value = value }

    /** The empty state's action: back to the whole library. */
    fun clearFilters() {
        _query.value = ""
        filters.value = ExerciseFilters()
    }
}
