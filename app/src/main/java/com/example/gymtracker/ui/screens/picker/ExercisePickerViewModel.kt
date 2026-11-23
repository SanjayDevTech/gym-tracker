package com.example.gymtracker.ui.screens.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.ui.components.ExerciseFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class PickerUiState(
    val query: String,
    val filters: ExerciseFilters,
    /** Last 8 exercises used; hidden while searching or filtering. */
    val recent: List<ExerciseEntity>,
    val results: List<ExerciseEntity>,
    val total: Int,
    val inPlan: Set<Long>,
    /** Selection in pick order. */
    val selected: List<Long>,
) {
    val searching: Boolean get() = query.isNotBlank() || filters.isActive
}

class ExercisePickerViewModel(
    repository: GymRepository,
    planId: Long?,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(ExerciseFilters())
    private val selected = MutableStateFlow<List<Long>>(emptyList())

    private val recentIds = flow { emit(repository.recentExerciseIds(RECENT_COUNT)) }
    private val inPlan = planId?.let { id ->
        repository.observePlanWithExercises(id).map { p -> p?.planned?.map { it.planned.exerciseId }?.toSet().orEmpty() }
    } ?: flowOf(emptySet())

    private val inputs = combine(query, filters, selected) { q, f, s -> Triple(q, f, s) }

    /** Null until the first Room emission (house rule 8). */
    val state: StateFlow<PickerUiState?> = combine(
        repository.observeExercises(),
        recentIds,
        inPlan,
        inputs,
    ) { all, recent, planned, (q, f, s) ->
        val byId = all.associateBy { it.id }
        val needle = q.trim()
        PickerUiState(
            query = q,
            filters = f,
            recent = recent.mapNotNull { byId[it] },
            results = all.filter { f.matches(it) && (needle.isEmpty() || it.name.contains(needle, ignoreCase = true)) },
            total = all.size,
            inPlan = planned,
            selected = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) { query.value = value }
    fun setFilters(value: ExerciseFilters) { filters.value = value }
    fun clearSearch() { query.value = ""; filters.value = ExerciseFilters() }

    fun toggle(id: Long) = selected.update { if (id in it) it - id else it + id }

    /** A just-created exercise: select it and clear the search that found nothing. */
    fun onCreated(id: Long) {
        clearSearch()
        selected.update { if (id in it) it else it + id }
    }

    companion object {
        const val RECENT_COUNT = 8
    }
}
