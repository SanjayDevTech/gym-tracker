package com.example.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/** `savedStateHandle` key the custom-exercise form writes a newly created id under. */
const val CREATED_EXERCISE_KEY = "created_exercise_id"

/** Deliver a newly created exercise's id to the entry below the form, then pop the form. */
fun NavController.returnCreatedExercise(id: Long) {
    previousBackStackEntry?.savedStateHandle?.set(CREATED_EXERCISE_KEY, id)
    popBackStack()
}

/** The created-exercise result waiting on this entry, or null. Pair with [clearCreatedExercise]. */
@Composable
fun NavBackStackEntry.createdExercise(): Long? {
    val id by savedStateHandle.getStateFlow<Long?>(CREATED_EXERCISE_KEY, null)
        .collectAsStateWithLifecycle()
    return id
}

fun NavBackStackEntry.clearCreatedExercise() {
    savedStateHandle.remove<Long>(CREATED_EXERCISE_KEY)
}
