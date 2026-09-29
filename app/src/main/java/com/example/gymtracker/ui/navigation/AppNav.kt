package com.example.gymtracker.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.screens.analytics.AnalyticsScreen
import com.example.gymtracker.ui.screens.library.ExerciseDetailScreen
import com.example.gymtracker.ui.screens.library.ExerciseDetailViewModel
import com.example.gymtracker.ui.screens.library.LibraryScreen
import com.example.gymtracker.ui.screens.library.LibraryViewModel
import com.example.gymtracker.ui.screens.plans.PlanEditorScreen
import com.example.gymtracker.ui.screens.plans.PlanListScreen
import com.example.gymtracker.ui.screens.plans.PlanListViewModel
import com.example.gymtracker.ui.screens.plans.importplan.ImportPlanScreen
import com.example.gymtracker.ui.screens.settings.SettingsScreen
import com.example.gymtracker.ui.screens.settings.SettingsViewModel

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val container = LocalAppContainer.current

    LaunchedEffect(Unit) {
        container.resumeSessionRequests.collect {
            val draft = container.repository.findSessionDraft() ?: return@collect
            // Avoid stacking duplicate logger destinations if one's already on top.
            val onLogger = nav.currentBackStackEntry
                ?.destination?.hasRoute(SessionLoggerRoute::class) == true
            if (!onLogger) {
                nav.navigate(SessionLoggerRoute(planId = draft.planId))
            }
        }
    }

    // A plan JSON opened/shared from outside the app jumps straight to the import review screen.
    val pendingImport by container.pendingImport.collectAsStateWithLifecycle()
    LaunchedEffect(pendingImport) {
        if (pendingImport != null) {
            val onImport = nav.currentBackStackEntry
                ?.destination?.hasRoute(ImportPlanRoute::class) == true
            if (!onImport) nav.navigate(ImportPlanRoute)
        }
    }

    NavHost(
        navController = nav,
        startDestination = PlansRoute,
        // Fade + scale pop: forward, the incoming screen scales up and fades in; back, the top
        // screen scales down and fades out. Under the predictive-back gesture these are seeked
        // by drag progress.
        enterTransition = { scaleIn(initialScale = 0.85f, animationSpec = tween(250)) + fadeIn(tween(250)) },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { scaleIn(initialScale = 0.92f, animationSpec = tween(250)) + fadeIn(tween(250)) },
        popExitTransition = { scaleOut(targetScale = 0.85f, animationSpec = tween(250)) + fadeOut(tween(250)) },
        modifier = Modifier.fillMaxSize(),
    ) {
        composable<PlansRoute> {
            val vm: PlanListViewModel = viewModel(
                factory = AppViewModelFactory(
                    mapOf(PlanListViewModel::class.java to {
                        PlanListViewModel(container.repository)
                    })
                )
            )
            PlanListScreen(
                viewModel = vm,
                onLibrary = { nav.navigate(LibraryRoute) },
                onQuickLog = { nav.navigate(SessionLoggerRoute()) },
                onHistory = { nav.navigate(HistoryRoute) },
                onAnalytics = { nav.navigate(AnalyticsRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
                onOpenPlan = { nav.navigate(PlanEditorRoute(it)) },
                onLogPlan = { nav.navigate(SessionLoggerRoute(planId = it)) },
                onResumeSession = { nav.navigate(SessionLoggerRoute(planId = it)) },
                onImportPlan = { nav.navigate(ImportPlanRoute) },
            )
        }

        composable<ImportPlanRoute> {
            // Consume once on first composition: a deep-link's JSON pre-loads the review screen.
            val initialText = remember { container.consumePendingImport() }
            ImportPlanScreen(
                initialText = initialText,
                onBack = { nav.popBackStack() },
                onImported = { planId ->
                    nav.popBackStack()
                    nav.navigate(PlanEditorRoute(planId))
                },
            )
        }

        composable<AnalyticsRoute> {
            AnalyticsScreen(onBack = { nav.popBackStack() })
        }

        composable<SettingsRoute> {
            val vm: SettingsViewModel = viewModel(
                factory = AppViewModelFactory(
                    mapOf(SettingsViewModel::class.java to {
                        SettingsViewModel(container.prefs)
                    })
                )
            )
            SettingsScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }

        composable<LibraryRoute> {
            val vm: LibraryViewModel = viewModel(
                factory = AppViewModelFactory(
                    mapOf(LibraryViewModel::class.java to {
                        LibraryViewModel(container.repository)
                    })
                )
            )
            LibraryScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onExerciseClick = { nav.navigate(ExerciseDetailRoute(it)) },
            )
        }

        composable<ExerciseDetailRoute> { entry ->
            val route: ExerciseDetailRoute = entry.toRoute<ExerciseDetailRoute>()
            val unit by container.prefs.weightUnit.collectAsStateWithLifecycle(
                initialValue = com.example.gymtracker.data.WeightUnit.Kg
            )
            val vm: ExerciseDetailViewModel = viewModel(
                key = "exercise-${route.exerciseId}",
                factory = AppViewModelFactory(
                    mapOf(ExerciseDetailViewModel::class.java to {
                        ExerciseDetailViewModel(container.repository, route.exerciseId)
                    })
                )
            )
            ExerciseDetailScreen(
                viewModel = vm,
                weightUnit = unit,
                onBack = { nav.popBackStack() },
            )
        }

        composable<PlanEditorRoute> { entry ->
            val route: PlanEditorRoute = entry.toRoute<PlanEditorRoute>()
            PlanEditorScreen(
                planId = route.planId,
                onBack = { nav.popBackStack() },
                onLog = { nav.navigate(SessionLoggerRoute(planId = it)) },
                onOpenExercise = { nav.navigate(ExerciseDetailRoute(it)) },
            )
        }

        composable<SessionLoggerRoute> { entry ->
            val route: SessionLoggerRoute = entry.toRoute<SessionLoggerRoute>()
            com.example.gymtracker.ui.screens.logger.SessionLoggerHost(
                planId = route.planId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }

        composable<HistoryRoute> {
            com.example.gymtracker.ui.screens.history.HistoryHost(
                onBack = { nav.popBackStack() },
                onOpenSession = { nav.navigate(SessionDetailRoute(it)) },
            )
        }

        composable<SessionDetailRoute> { entry ->
            val route: SessionDetailRoute = entry.toRoute<SessionDetailRoute>()
            com.example.gymtracker.ui.screens.history.SessionDetailHost(
                sessionId = route.sessionId,
                onBack = { nav.popBackStack() },
                onOpenExercise = { nav.navigate(ExerciseDetailRoute(it)) },
            )
        }
    }
}
