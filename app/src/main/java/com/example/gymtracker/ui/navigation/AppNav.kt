package com.example.gymtracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.first
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import com.example.gymtracker.ui.screens.plans.importplan.ImportPlanScreen
import com.example.gymtracker.ui.screens.plans.templates.TemplateDetailScreen
import com.example.gymtracker.ui.screens.plans.templates.TemplateListScreen
import com.example.gymtracker.ui.screens.settings.YouScreen
import com.example.gymtracker.ui.screens.home.HomeScreen
import com.example.gymtracker.ui.screens.home.StartWorkoutSheet
import com.example.gymtracker.ui.screens.onboarding.OnboardingScreen
import com.example.gymtracker.ui.screens.settings.SettingsViewModel
import com.example.gymtracker.ui.screens.summary.SessionSummaryScreen
import kotlinx.coroutines.launch

/** A workout the user asked to start while another was running. */
private data class PendingStart(val planId: Long?, val title: String, val running: RunningDraft)

@Composable
fun AppNav() {
    val container = LocalAppContainer.current
    // Read once before the graph is built: the start destination can't change under a NavHost.
    val onboardedPref by produceState<Boolean?>(initialValue = null) {
        value = container.prefs.onboarded.first()
    }
    val onboarded = onboardedPref ?: return
    AppNavHost(onboarded)
}

@Composable
private fun AppNavHost(onboarded: Boolean) {
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

    // ---- Starting a workout and the session guard ------------------------------------------------
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

    // ---- "Push A is still running · Resume", once per session, on Home --------------------------
    var runningNotice by remember { mutableStateOf<RunningDraft?>(null) }
    var noticeShownFor by rememberSaveable { mutableLongStateOf(0L) }
    val homeSnackbar = remember { SnackbarHostState() }
    var showStart by rememberSaveable { mutableStateOf(false) }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.barDestination()
    val hazeState = rememberHazeState()

    if (showStart) {
        StartWorkoutSheet(
            onQuickLog = { showStart = false; startSession(null) },
            onStartPlan = { showStart = false; startSession(it) },
            onDismiss = { showStart = false },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = nav,
            startDestination = if (onboarded) HomeRoute else OnboardingRoute,
            // The bar floats over the tab screens, which pad their own content by
            // [bottomBarClearance]; leaf pages hide it and own the bottom of the screen.
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            enterTransition = navEnter,
            exitTransition = navExit,
            popEnterTransition = navPopEnter,
            popExitTransition = navPopExit,
            predictivePopEnterTransition = navPredictivePopEnter,
            predictivePopExitTransition = navPredictivePopExit,
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onDone = {
                        nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                    },
                )
            }

            // ---- Bar destinations -------------------------------------------------------------

            composable<HomeRoute> {
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
                        val result = homeSnackbar.showSnackbar(
                            message = "${notice.title} is still running",
                            actionLabel = "Resume",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            nav.navigate(SessionLoggerRoute(planId = notice.planId))
                        }
                    }
                }
                HomeScreen(
                    viewModel = vm,
                    snackbarHostState = homeSnackbar,
                    onOpenPlan = { nav.navigate(PlanEditorRoute(it)) },
                    onStartPlan = { startSession(it) },
                    onQuickLog = { startSession(null) },
                    onResumeSession = { nav.navigate(SessionLoggerRoute(planId = it)) },
                    onOpenTemplates = { nav.navigate(TemplatesRoute) },
                    onOpenProgress = { nav.toBarDestination(BarDestination.Progress) },
                    onOpenHistory = { nav.navigate(HistoryRoute) },
                    onOpenSession = { nav.navigate(SessionDetailRoute(it)) },
                )
            }

            composable<ProgressRoute> {
                ProgressScreen(
                    onOpenExercises = { nav.navigate(ExercisesRoute) },
                    onOpenHistory = { nav.navigate(HistoryRoute) },
                )
            }

            // ---- Leaf pages -----------------------------------------------------------------

            composable<YouRoute> {
                val vm: SettingsViewModel = viewModel(
                    factory = AppViewModelFactory(
                        mapOf(SettingsViewModel::class.java to {
                            SettingsViewModel(container.prefs, container.database.sessionDao())
                        })
                    )
                )
                YouScreen(
                    viewModel = vm,
                    onOpenData = { nav.navigate(DataRoute) },
                    onOpenExercises = { nav.navigate(ExercisesRoute) },
                    onOpenTemplates = { nav.navigate(TemplatesRoute) },
                    onOpenHistory = { nav.navigate(HistoryRoute) },
                )
            }

            // ---- Leaf pages -----------------------------------------------------------------

            composable<HistoryRoute> {
                HistoryScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSession = { nav.navigate(SessionDetailRoute(it)) },
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
                        // Back to Home, where the new plans now sit at the end of the list.
                        nav.toBarDestination(BarDestination.Home)
                        scope.launch { homeSnackbar.showSnackbar("Added $count ${plural(count, "plan")}") }
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
                        // The draft persists; Home says so once per session.
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
                        // The summary replaces the logger in the stack.
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
        AnimatedVisibility(
            visible = currentTab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            AppBottomBar(
                hazeState = hazeState,
                current = currentTab,
                onSelect = { nav.toBarDestination(it) },
                onAdd = {
                    // A running workout is one tap away; otherwise pick what to start.
                    val running = draft
                    if (running != null) nav.navigate(SessionLoggerRoute(planId = running.planId))
                    else showStart = true
                },
            )
        }
    }
}

/**
 * Switch bar tabs. The three are peers, not a stack:
 * `popUpTo(HomeRoute)` with `saveState`/`restoreState` means Home → Progress → You → back lands
 * on Home, and each tab keeps its own stack and scroll position. `launchSingleTop`
 * stops a second tap on the current tab stacking a duplicate.
 */
private fun NavHostController.toBarDestination(dest: BarDestination) {
    val route: Any = when (dest) {
        BarDestination.Home -> HomeRoute
        BarDestination.Progress -> ProgressRoute
        BarDestination.You -> YouRoute
    }
    navigate(route) {
        // HomeRoute, not the graph's start: after onboarding the start destination is gone.
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/*
 * One set of transitions, chosen per pair of entries. Tab ↔ tab is a fade-through (house rule 7:
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
