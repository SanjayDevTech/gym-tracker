package com.example.gymtracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.RunningDraft
import com.example.gymtracker.ui.components.SessionGuardSheet
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.screens.analytics.ProgressScreen
import com.example.gymtracker.ui.screens.exerciseform.ExerciseFormScreen
import com.example.gymtracker.ui.screens.exerciseform.ExerciseFormViewModel
import com.example.gymtracker.ui.screens.exerciseform.exerciseFormRouteFor
import com.example.gymtracker.ui.screens.exerciseform.prefill
import com.example.gymtracker.ui.screens.data.DataScreen
import com.example.gymtracker.ui.screens.history.HistoryScreen
import com.example.gymtracker.ui.screens.history.SessionDetailScreen
import com.example.gymtracker.ui.screens.library.ExerciseDetailScreen
import com.example.gymtracker.ui.screens.library.ExerciseDetailViewModel
import com.example.gymtracker.ui.screens.library.LibraryScreen
import com.example.gymtracker.ui.screens.library.LibraryViewModel
import com.example.gymtracker.ui.screens.logger.SessionLoggerHost
import com.example.gymtracker.ui.screens.picker.ExercisePickerScreen
import com.example.gymtracker.ui.screens.plans.PlanEditorScreen
import com.example.gymtracker.ui.screens.plans.PlanListViewModel
import com.example.gymtracker.ui.screens.plans.TrainScreen
import com.example.gymtracker.ui.screens.plans.importplan.ImportPlanScreen
import com.example.gymtracker.ui.screens.plans.templates.TemplateDetailScreen
import com.example.gymtracker.ui.screens.plans.templates.TemplateListScreen
import com.example.gymtracker.ui.screens.settings.SettingsScreen
import com.example.gymtracker.ui.screens.settings.SettingsViewModel
import com.example.gymtracker.ui.screens.summary.SessionSummaryScreen
import kotlinx.coroutines.launch

/** A workout the user asked to start while another was running (§4.6). */
private data class PendingStart(val planId: Long?, val title: String, val running: RunningDraft)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

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

    // ---- Starting a workout (§6.1) and the session guard (§4.6) --------------------------------
    // Hoisted here so Train's Start / Quick log and the editor's Start workout share one guard.
    // Remembered: Room hands back a new Flow per call, and collectAsStateWithLifecycle restarts
    // (re-subscribing and re-querying) whenever its flow instance changes. AppNav recomposes on
    // every navigation, so an un-remembered call re-ran the draft query on each one.
    val draftFlow = remember(container) { container.repository.observeSessionDraft() }
    val draft by draftFlow.collectAsStateWithLifecycle(initialValue = null)
    var pendingStart by remember { mutableStateOf<PendingStart?>(null) }

    fun startSession(planId: Long?) {
        val running = draft
        // No draft, or the same start again (same plan / quick log again) → just open it.
        if (running == null || running.planId == planId) {
            nav.navigate(SessionLoggerRoute(planId = planId))
            return
        }
        scope.launch {
            val title = planId?.let { container.repository.findPlan(it)?.name } ?: "a quick log"
            pendingStart = PendingStart(planId, title, RunningDraft.from(running))
        }
    }

    pendingStart?.let { pending ->
        SessionGuardSheet(
            running = pending.running,
            startingTitle = pending.title,
            onResume = {
                pendingStart = null
                nav.navigate(SessionLoggerRoute(planId = pending.running.planId))
            },
            onDiscardAndStart = {
                pendingStart = null
                scope.launch {
                    // Clear before navigating: the new logger's VM seeds from the draft row.
                    container.repository.clearSessionDraft()
                    nav.navigate(SessionLoggerRoute(planId = pending.planId))
                }
            },
            onDismiss = { pendingStart = null },
        )
    }

    // ---- "Push A is still running · Resume" (§5), once per session, on Train -------------------
    var runningNotice by remember { mutableStateOf<RunningDraft?>(null) }
    var noticeShownFor by rememberSaveable { mutableLongStateOf(0L) }
    val trainSnackbar = remember { SnackbarHostState() }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.barDestination()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The outer Scaffold pads for the bar only. With no insets of its own, `barPadding` is the
        // bar's height (which already includes the navigation-bar inset) on the three tabs and 0
        // on leaf pages, whose own Scaffolds then consume the system inset themselves (§5).
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // Leaf pages push onto the current tab's stack and hide the bar; the logger owns the
            // bottom of the screen (rule 11).
            if (currentTab != null) {
                AppBottomBar(current = currentTab, onSelect = { nav.toBarDestination(it) })
            }
        },
    ) { inner ->
        val barPadding = inner.calculateBottomPadding()
        NavHost(
            navController = nav,
            startDestination = TrainRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = barPadding)
                // Inset rule (§5, food lesson 3): padding moves the content up but consumes
                // nothing, so a destination's own Scaffold would still read the navigation-bar
                // inset and add it a second time, floating its FAB and snackbar an inset clear
                // of the bar. Consume the bottom only; status-bar insets stay each screen's job.
                .consumeWindowInsets(PaddingValues(bottom = barPadding)),
            enterTransition = navEnter,
            exitTransition = navExit,
            popEnterTransition = navPopEnter,
            popExitTransition = navPopExit,
            predictivePopEnterTransition = navPredictivePopEnter,
            predictivePopExitTransition = navPredictivePopExit,
        ) {
            // ---- Bar destinations -------------------------------------------------------------

            composable<TrainRoute> {
                val vm: PlanListViewModel = viewModel(
                    factory = AppViewModelFactory(
                        mapOf(PlanListViewModel::class.java to {
                            PlanListViewModel(container.repository, container.database.sessionDao(), container.prefs)
                        })
                    )
                )
                LaunchedEffect(runningNotice) {
                    val notice = runningNotice ?: return@LaunchedEffect
                    runningNotice = null // restarts this effect; the snackbar runs in AppNav's scope
                    scope.launch {
                        val result = trainSnackbar.showSnackbar(
                            message = "${notice.title} is still running",
                            actionLabel = "Resume",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            nav.navigate(SessionLoggerRoute(planId = notice.planId))
                        }
                    }
                }
                TrainScreen(
                    viewModel = vm,
                    snackbarHostState = trainSnackbar,
                    onSettings = { nav.navigate(SettingsRoute) },
                    onQuickLog = { startSession(null) },
                    onOpenPlan = { nav.navigate(PlanEditorRoute(it)) },
                    onLogPlan = { startSession(it) },
                    onResumeSession = { nav.navigate(SessionLoggerRoute(planId = it)) },
                    onOpenTemplates = { nav.navigate(TemplatesRoute) },
                )
            }

            composable<HistoryRoute> {
                HistoryScreen(onOpenSession = { nav.navigate(SessionDetailRoute(it)) })
            }

            composable<ProgressRoute> {
                ProgressScreen(onOpenExercises = { nav.navigate(ExercisesRoute) })
            }

            // ---- Leaf pages -----------------------------------------------------------------

            composable<SettingsRoute> {
                val vm: SettingsViewModel = viewModel(
                    factory = AppViewModelFactory(
                        mapOf(SettingsViewModel::class.java to {
                            SettingsViewModel(container.prefs)
                        })
                    )
                )
                SettingsScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() },
                    onOpenData = { nav.navigate(DataRoute) },
                )
            }

            composable<DataRoute> {
                DataScreen(
                    onBack = { nav.popBackStack() },
                    onImportPlan = { nav.navigate(ImportPlanRoute) },
                    onOpenTemplates = { nav.navigate(TemplatesRoute) },
                )
            }

            composable<TemplatesRoute> {
                TemplateListScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(TemplateDetailRoute(it)) },
                )
            }

            composable<TemplateDetailRoute> { entry ->
                val route: TemplateDetailRoute = entry.toRoute<TemplateDetailRoute>()
                TemplateDetailScreen(
                    templateId = route.templateId,
                    onBack = { nav.popBackStack() },
                    onAdded = { count ->
                        // Back to Train, where the new plans now sit at the end of the list.
                        nav.popBackStack(TrainRoute, inclusive = false)
                        scope.launch { trainSnackbar.showSnackbar("Added $count ${plural(count, "plan")}") }
                    },
                )
            }

            composable<ImportPlanRoute> { entry ->
                // Consume once on first composition: a deep-link's JSON pre-loads the review screen.
                val initialText = remember { container.consumePendingImport() }
                ImportPlanScreen(
                    initialText = initialText,
                    onPickExercise = { nav.navigate(ExercisePickerRoute(PickerMode.Single)) },
                    pickedExerciseIds = entry.pickedExercises(),
                    onPickedHandled = { entry.clearPickedExercises() },
                    onCreateExercise = { source -> nav.navigate(exerciseFormRouteFor(source)) },
                    createdExerciseId = entry.createdExercise(),
                    onCreatedHandled = { entry.clearCreatedExercise() },
                    onBack = { nav.popBackStack() },
                    onImported = { planId ->
                        nav.popBackStack()
                        nav.navigate(PlanEditorRoute(planId))
                    },
                )
            }

            composable<ExercisesRoute> {
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
                    onNewExercise = { nav.navigate(ExerciseFormRoute()) },
                )
            }

            composable<ExerciseDetailRoute> { entry ->
                val route: ExerciseDetailRoute = entry.toRoute<ExerciseDetailRoute>()
                val unit by container.prefs.weightUnit.collectAsStateWithLifecycle(initialValue = WeightUnit.Kg)
                val vm: ExerciseDetailViewModel = viewModel(
                    key = "exercise-${route.exerciseId}",
                    factory = AppViewModelFactory(
                        mapOf(ExerciseDetailViewModel::class.java to {
                            ExerciseDetailViewModel(container.repository, route.exerciseId, container.applicationScope)
                        })
                    )
                )
                ExerciseDetailScreen(
                    viewModel = vm,
                    weightUnit = unit,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(ExerciseFormRoute(exerciseId = it)) },
                )
            }

            composable<ExerciseFormRoute> { entry ->
                val route: ExerciseFormRoute = entry.toRoute<ExerciseFormRoute>()
                val vm: ExerciseFormViewModel = viewModel(
                    factory = AppViewModelFactory(
                        mapOf(ExerciseFormViewModel::class.java to {
                            ExerciseFormViewModel(container.repository, route.exerciseId, route.prefill())
                        })
                    )
                )
                ExerciseFormScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() },
                    // A new exercise's id flows back to whoever opened the form (picker, Import).
                    onSaved = { if (it.created) nav.returnCreatedExercise(it.id) else nav.popBackStack() },
                )
            }

            composable<PlanEditorRoute> { entry ->
                val route: PlanEditorRoute = entry.toRoute<PlanEditorRoute>()
                PlanEditorScreen(
                    planId = route.planId,
                    onBack = { nav.popBackStack() },
                    onLog = { startSession(it) },
                    onOpenExercise = { nav.navigate(ExerciseDetailRoute(it)) },
                    onAddExercises = { nav.navigate(ExercisePickerRoute(PickerMode.Plan, planId = route.planId)) },
                    pickedExerciseIds = entry.pickedExercises(),
                    onPickedHandled = { entry.clearPickedExercises() },
                )
            }

            composable<SessionLoggerRoute> { entry ->
                val route: SessionLoggerRoute = entry.toRoute<SessionLoggerRoute>()
                SessionLoggerHost(
                    planId = route.planId,
                    pickedExerciseIds = entry.pickedExercises(),
                    onPickedHandled = { entry.clearPickedExercises() },
                    onMinimise = {
                        // The draft persists; Train says so once per session (§5).
                        draft?.let { RunningDraft.from(it) }?.let { running ->
                            if (running.startedAtMillis != noticeShownFor) {
                                noticeShownFor = running.startedAtMillis
                                runningNotice = running
                            }
                        }
                        nav.popBackStack()
                    },
                    onDiscarded = { nav.popBackStack() },
                    onFinished = { sessionId ->
                        // The summary replaces the logger in the stack (§5 back-stack rules).
                        nav.navigate(SessionSummaryRoute(sessionId)) {
                            popUpTo<SessionLoggerRoute> { inclusive = true }
                        }
                    },
                    onAddExercises = { nav.navigate(ExercisePickerRoute(PickerMode.Session)) },
                    onOpenExercise = { nav.navigate(ExerciseDetailRoute(it)) },
                )
            }

            composable<SessionSummaryRoute> { entry ->
                val route: SessionSummaryRoute = entry.toRoute<SessionSummaryRoute>()
                SessionSummaryScreen(sessionId = route.sessionId, onDone = { nav.popBackStack() })
            }

            composable<ExercisePickerRoute> { entry ->
                val route: ExercisePickerRoute = entry.toRoute<ExercisePickerRoute>()
                ExercisePickerScreen(
                    mode = route.mode,
                    planId = route.planId,
                    onBack = { nav.popBackStack() },
                    onPicked = { ids -> nav.returnPickedExercises(ids) },
                    onNewExercise = { name -> nav.navigate(ExerciseFormRoute(prefillName = name.ifBlank { null })) },
                    createdExerciseId = entry.createdExercise(),
                    onCreatedHandled = { entry.clearCreatedExercise() },
                )
            }

            composable<SessionDetailRoute> { entry ->
                val route: SessionDetailRoute = entry.toRoute<SessionDetailRoute>()
                SessionDetailScreen(
                    sessionId = route.sessionId,
                    onBack = { nav.popBackStack() },
                    onOpenExercise = { nav.navigate(ExerciseDetailRoute(it)) },
                )
            }
        }
    }
}

/**
 * Switch bar tabs — §5 "Back-stack behaviour". The three are peers, not a stack:
 * `popUpTo(startDestination)` with `saveState`/`restoreState` means Train → History → Progress →
 * back lands on Train, and each tab keeps its own stack and scroll position. `launchSingleTop`
 * stops a second tap on the current tab stacking a duplicate.
 */
private fun NavHostController.toBarDestination(dest: BarDestination) {
    val route: Any = when (dest) {
        BarDestination.Train -> TrainRoute
        BarDestination.History -> HistoryRoute
        BarDestination.Progress -> ProgressRoute
    }
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/*
 * One set of transitions, chosen per pair of entries. Tab ↔ tab is a fade-through (§1 motion:
 * 90ms out, then 210ms in, so the outgoing tab is gone before the incoming one starts — peers do
 * not slide, a slide says "deeper" or "back"). Everything else is a push or pop: food's 300ms
 * slide with ~30% parallax.
 *
 * The `predictivePop*` pair matters as much as `pop*`: a pop driven by the predictive-back gesture
 * uses its own transitions, which default to a fade + scale-out. Pointing them at the same motion
 * keeps the gesture and the back button identical. (The Int is the swipe edge; unused.)
 */
private const val NAV_ANIM_MS = 300
private const val FADE_OUT_MS = 90
private const val FADE_IN_MS = 210

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.barDestination() != null &&
        targetState.destination.barDestination() != null

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(durationMillis = FADE_IN_MS, delayMillis = FADE_OUT_MS))

private fun fadeThroughOut(): ExitTransition = fadeOut(tween(FADE_OUT_MS))

private val navEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isTabSwitch()) fadeThroughIn() else slideIntoContainer(SlideDirection.Start, tween(NAV_ANIM_MS))
}

private val navExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isTabSwitch()) fadeThroughOut() else slideOutOfContainer(SlideDirection.Start, tween(NAV_ANIM_MS)) { it / 3 }
}

private val navPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isTabSwitch()) fadeThroughIn() else slideIntoContainer(SlideDirection.End, tween(NAV_ANIM_MS)) { it / 3 }
}

private val navPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isTabSwitch()) fadeThroughOut() else slideOutOfContainer(SlideDirection.End, tween(NAV_ANIM_MS))
}

private val navPredictivePopEnter:
    AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = { navPopEnter() }

private val navPredictivePopExit:
    AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { navPopExit() }
