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
