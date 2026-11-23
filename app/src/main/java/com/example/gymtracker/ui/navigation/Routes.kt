package com.example.gymtracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object PlansRoute
@Serializable object AnalyticsRoute
@Serializable object SettingsRoute

@Serializable object LibraryRoute
@Serializable data class ExerciseDetailRoute(val exerciseId: Long)

@Serializable data class PlanEditorRoute(val planId: Long)
@Serializable object ImportPlanRoute

@Serializable object HistoryRoute
@Serializable data class SessionDetailRoute(val sessionId: Long)

@Serializable data class SessionLoggerRoute(val planId: Long? = null)

/** Shown after Finish, replacing the logger in the stack (§4.5). */
@Serializable data class SessionSummaryRoute(val sessionId: Long)

/**
 * Who opened the exercise picker (§4.13), which decides its behaviour:
 * [Session] multi-select with autofocused search (logger); [Plan] multi-select with `IN PLAN`
 * marks for [ExercisePickerRoute.planId]; [Single] one tap returns (Import's Map).
 */
enum class PickerMode { Session, Plan, Single }

/** Full-page exercise picker. Returns a `LongArray` of ids under [PICKED_EXERCISES_KEY] on the
 *  previous back-stack entry's `savedStateHandle`, in pick order. */
@Serializable data class ExercisePickerRoute(val mode: PickerMode, val planId: Long? = null)
