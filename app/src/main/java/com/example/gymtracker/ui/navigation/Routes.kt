package com.example.gymtracker.ui.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/*
 * Every destination. The three bar destinations ([HomeRoute], [ProgressRoute], [YouRoute]) are
 * peers; everything else is a leaf page that pushes onto whichever tab is current and hides the
 * bar. [OnboardingRoute] is the whole graph until the first run finishes.
 */

@Serializable object OnboardingRoute

// ---- Bottom-bar destinations ----------------------------------------------

/** Start destination: live session, the week, the muscle map, plans and recent sessions. */
@Serializable object HomeRoute
/** Consistency, volume, muscles over any period. */
@Serializable object ProgressRoute
/** Settings, library and data, behind a profile card. */
@Serializable object YouRoute

// ---- Leaf pages ------------------------------------------------------------

/** Every session, newest first — from Home's "Recent" and Progress. */
@Serializable object HistoryRoute
/** Plans & backup, from You. */
@Serializable object DataRoute

/** The exercise library, from Progress's top-bar action. */
@Serializable object ExercisesRoute
@Serializable data class ExerciseDetailRoute(val exerciseId: Long)

/**
 * Custom exercise form. [exerciseId] null = create, else edit (custom only).
 * The `prefill*` fields seed a new exercise — the picker's search text, or an Import entry's
 * optional fields (enum names; muscles comma-separated). A create returns the new id under
 * [CREATED_EXERCISE_KEY] on the previous entry's `savedStateHandle`.
 */
@Serializable data class ExerciseFormRoute(
    val exerciseId: Long? = null,
    val prefillName: String? = null,
    val prefillType: String? = null,
    val prefillEquipment: String? = null,
    val prefillPrimary: String? = null,
    val prefillSecondary: String? = null,
)

@Serializable data class PlanEditorRoute(val planId: Long)
@Serializable object ImportPlanRoute

/** Plan templates, from New plan and Plans & backup. */
@Serializable object TemplatesRoute
@Serializable data class TemplateDetailRoute(val templateId: String)

@Serializable data class SessionDetailRoute(val sessionId: Long)

@Serializable data class SessionLoggerRoute(val planId: Long? = null)

/** Shown after Finish, replacing the logger in the stack. */
@Serializable data class SessionSummaryRoute(val sessionId: Long)

/**
 * Who opened the exercise picker, which decides its behaviour:
 * [Session] multi-select with autofocused search (logger); [Plan] multi-select with `IN PLAN`
 * marks for [ExercisePickerRoute.planId]; [Single] one tap returns (Import's Map).
 */
// Kept: Navigation looks enum route arguments up by class name, which R8 would rename.
@Keep
enum class PickerMode { Session, Plan, Single }

/** Full-page exercise picker. Returns a `LongArray` of ids under [PICKED_EXERCISES_KEY] on the
 *  previous back-stack entry's `savedStateHandle`, in pick order. */
@Serializable data class ExercisePickerRoute(val mode: PickerMode, val planId: Long? = null)
