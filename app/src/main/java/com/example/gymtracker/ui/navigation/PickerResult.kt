package com.example.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/** `savedStateHandle` key the exercise picker writes its result under. */
const val PICKED_EXERCISES_KEY = "picked_exercise_ids"

/** Deliver the picker's selection to the entry below it, then pop the picker. */
fun NavController.returnPickedExercises(ids: List<Long>) {
    previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISES_KEY, ids.toLongArray())
    popBackStack()
}

/** The picker result waiting on this entry, or null. Pair with [clearPickedExercises]. */
@Composable
fun NavBackStackEntry.pickedExercises(): List<Long>? {
    val ids by savedStateHandle.getStateFlow<LongArray?>(PICKED_EXERCISES_KEY, null)
        .collectAsStateWithLifecycle()
    return ids?.toList()
}

fun NavBackStackEntry.clearPickedExercises() {
    savedStateHandle.remove<LongArray>(PICKED_EXERCISES_KEY)
}
