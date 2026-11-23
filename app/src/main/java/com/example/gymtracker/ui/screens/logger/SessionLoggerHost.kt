package com.example.gymtracker.ui.screens.logger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer

/**
 * Nav-facing entry for the logger: owns the ViewModel (scoped to the back-stack entry) and feeds
 * it the exercise picker's result. [pickedExerciseIds] is the picker's one-shot result; call
 * [onPickedHandled] once it is applied so it is not applied twice.
 */
@Composable
fun SessionLoggerHost(
    planId: Long?,
    pickedExerciseIds: List<Long>?,
    onPickedHandled: () -> Unit,
    onMinimise: () -> Unit,
    onDiscarded: () -> Unit,
    onFinished: (sessionId: Long) -> Unit,
    onAddExercises: () -> Unit,
    onOpenExercise: (exerciseId: Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SessionLoggerViewModel = viewModel(
        key = "logger-$planId",
        factory = AppViewModelFactory(
            mapOf(SessionLoggerViewModel::class.java to {
                SessionLoggerViewModel(container.repository, container.prefs, container.applicationScope, planId)
            })
        )
    )
    LaunchedEffect(pickedExerciseIds) {
        if (pickedExerciseIds != null) {
            viewModel.addExercises(pickedExerciseIds)
            onPickedHandled()
        }
    }
    LoggerScreen(
        viewModel = viewModel,
        onMinimise = onMinimise,
        onDiscarded = onDiscarded,
        onFinished = onFinished,
        onAddExercises = onAddExercises,
        onOpenExercise = onOpenExercise,
    )
}
