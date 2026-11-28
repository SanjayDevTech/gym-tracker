package com.example.gymtracker.ui.screens.logger

import com.example.gymtracker.ui.screens.logger.components.RestFocus
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import com.example.gymtracker.ui.screens.logger.components.logsWeight
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.ui.format.trimDecimal
import com.example.gymtracker.data.repUnitShort
import com.example.gymtracker.data.kgToDisplay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.theme.Radius
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.gymtracker.ui.AppHaptics
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.TargetSheet
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.formatShortDate
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel.Companion.REST_MAX_SECONDS
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel.Companion.REST_MIN_SECONDS
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel.Companion.REST_STEP_SECONDS
import com.example.gymtracker.ui.screens.logger.components.ExerciseBlock
import com.example.gymtracker.ui.screens.logger.components.DockConfirm
import com.example.gymtracker.ui.screens.logger.components.LoggerDock
import com.example.gymtracker.ui.screens.logger.components.LoggerTopBar
import com.example.gymtracker.ui.screens.logger.components.RestSheet
import com.example.gymtracker.ui.screens.logger.components.SetActions
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/** Undo snackbars stay 5s (§6.6) — between Material's Short (4s) and Long (10s). */
private const val UNDO_MILLIS = 5_000L

/** §1 motion: a finished exercise folds 600ms after its last set is ticked. */
private const val COLLAPSE_DELAY_MILLIS = 600L

/**
 * Session logger — design-plan §4.3, the reference implementation.
 *
 * Top bar (title, Finish, ⋮, progress) and the bottom workout dock (workout clock, rest ring and
 * its keys) are fixed; one `LazyColumn` of [ExerciseBlock]s scrolls between them using the
 * Scaffold's content padding. Tapping the ring while resting opens [RestFocus] over everything.
 * The dock is the only IME consumer, so the list's bottom padding follows the dock above the
 * keyboard and nothing double-counts the inset.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoggerScreen(
    viewModel: SessionLoggerViewModel,
    onMinimise: () -> Unit,
    onDiscarded: () -> Unit,
    onFinished: (sessionId: Long) -> Unit,
    onAddExercises: () -> Unit,
    onOpenExercise: (exerciseId: Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showRest by remember { mutableStateOf(false) }
    var restExpanded by rememberSaveable { mutableStateOf(false) }
    var targetFor by remember { mutableStateOf<String?>(null) }
    var reorderMode by rememberSaveable { mutableStateOf(false) }
    if (state.groups.size < 2 && reorderMode) reorderMode = false

    LaunchedEffect(state.savedSessionId) {
        state.savedSessionId?.let { haptics.confirm(); onFinished(it) }
    }
    LaunchedEffect(state.discarded) { if (state.discarded) onDiscarded() }

    // One clock drives both the elapsed and the rest readouts. It is read only through these
    // lambdas, by the leaves that show it (the dock, the rest view, Rest and Finish sheets), so a
    // tick never recomposes this screen or the list.
    val clock = rememberSessionClock()
    val startedAt = state.startedAtMillis
    val restBase = state.lastSetCompletedAtMillis
    // Paused: both readouts stand still at the moment of the pause.
    val pausedAt = state.pausedAtMillis
    val now: () -> Long = remember(clock, pausedAt) { { if (pausedAt > 0L) pausedAt else clock.longValue } }
    val elapsed: () -> String = remember(now, startedAt) { { elapsedLabel(now(), startedAt) } }
    val restElapsed: () -> Long? = remember(now, restBase) { { restElapsedMillis(now(), restBase) } }
    RestReachedHaptic(restElapsed, state.restTargetSeconds, restBase, haptics)
    // The full-screen rest view closes itself when rest is cleared (Skip, or no set ticked yet),
    // and system back closes it before it minimises the workout.
    LaunchedEffect(restBase) { if (restBase == 0L) restExpanded = false }
    BackHandler(enabled = restExpanded) { restExpanded = false }
    val nextSet = remember(state.groups, state.weightUnit) { nextSetLabel(state.groups, state.weightUnit) }

    // PRs: sets that currently beat the pre-session records. Structural equality keeps the same
    // instance while typing doesn't change which sets are PRs, so the per-group index sets below
    // keep their identity too and untouched blocks skip recomposition.
    val prKeys by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            sessionPrKeys(state.groups, state.baselineRecords, state.weightUnit)
        }
    }
    val prIndexes = remember(prKeys) { prSetIndexesByGroup(prKeys) }
    PrHaptic(state, prKeys, haptics)

    val collapse = rememberCollapseState()
    CollapseEffects(state, collapse, listState)

    // A group just added from the picker: scroll it into view and focus its first field.
    var focusUid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.revealGroupUid) {
        val uid = state.revealGroupUid ?: return@LaunchedEffect
        val index = state.groups.indexOfFirst { it.uid == uid }
        if (index >= 0) listState.animateScrollToItem(index)
        focusUid = uid
        viewModel.consumeReveal()
    }

    val density = LocalDensity.current
    val imeOpen = WindowInsets.imeAnimationTarget.getBottom(density) > 0

    // Remembered (a local fun is a new closure per composition) so the per-item callbacks that
    // capture it keep their identity and the blocks can skip.
    val showUndo: (String) -> Unit = remember(scope, snackbar, viewModel) {
        { message ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = withTimeoutOrNull(UNDO_MILLIS) {
                    snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Indefinite)
                }
                if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
            }
        }
    }

    // Re-checked on every clock tick (this screen used to recompose each second), so "Today"
    // still turns into the date at midnight; it recomposes the screen only when it flips.
    val isToday by remember {
        derivedStateOf { clock.longValue; state.dateEpochDay == DateUtil.todayEpochDay() }
    }
    val dateLabel = remember(state.dateEpochDay, isToday) {
        if (isToday) "Today" else formatShortDate(state.dateEpochDay)
    }
    val title = state.planName ?: "Quick log"
    // Discard until a set is ticked (nothing to save yet), then Finish; Discard moves into ⋮.
    val finishing = state.primaryAction == PrimaryAction.Finish
    // The set whose check is nudging: "groupUid#setIndex".
    var pulseKey by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (reorderMode) {
                    AppTopBar(
                        title = "Reorder exercises",
                        actions = { AppTextButton(text = "Done", onClick = { haptics.light(); reorderMode = false }) },
                    )
                } else {
                    LoggerTopBar(
                        title = title,
                        meta = "${state.doneSets} of ${state.totalSets} ${plural(state.totalSets, "set")}"
                            .takeIf { state.totalSets > 0 },
                        dateLabel = dateLabel,
                        dateHighlighted = !isToday,
                        progress = if (state.totalSets == 0) 0f else state.doneSets.toFloat() / state.totalSets,
                        onMinimise = onMinimise,
                        menu = listOfNotNull(
                            MenuItem("Change date", { showDatePicker = true }, enabled = state.initialized),
                            MenuItem(
                                "Reorder exercises",
                                { haptics.light(); reorderMode = true },
                                enabled = state.groups.size >= 2,
                            ),
                            MenuItem("Rest timer settings", { showRest = true }, enabled = state.initialized),
                            MenuItem("Discard workout", { showDiscard = true }, destructive = true).takeIf { finishing },
                        ),
                    )
                }
            },
            bottomBar = {
                if (!reorderMode) {
                    // The dock is the screen's only IME consumer. imeAnimationTarget ∪ navigationBars:
                    // nav-bar height when closed, keyboard height when open — never both. The target
                    // (not the live `ime` value) because the animated value can stick at its last frame
                    // when the keyboard is dismissed with system back (Google issue 229769102).
                    LoggerDock(
                        elapsed = elapsed,
                        restElapsed = restElapsed,
                        targetSeconds = state.restTargetSeconds,
                        doneSets = state.doneSets,
                        totalSets = state.totalSets,
                        enabled = state.initialized && state.groups.isNotEmpty(),
                        compact = imeOpen,
                        paused = pausedAt > 0L,
                        onTogglePause = { haptics.light(); viewModel.togglePause() },
                        onStartRest = { haptics.light(); viewModel.restartRest() },
                        onExpand = { haptics.light(); restExpanded = true },
                        onMinus = { haptics.light(); viewModel.adjustRestTarget(-REST_STEP_SECONDS) },
                        onPlus = { haptics.light(); viewModel.adjustRestTarget(REST_STEP_SECONDS) },
                        onSkip = { haptics.light(); viewModel.skipRest() },
                        primaryLabel = if (finishing) "Finish" else "Discard",
                        primaryDestructive = !finishing,
                        primaryEnabled = state.initialized && !state.saving,
                        onPrimary = {
                            haptics.light()
                            if (finishing) viewModel.openFinish() else showDiscard = true
                        },
                        // Finish and Discard confirm in the dock itself, not in a sheet.
                        confirm = when {
                            state.finish != null -> {
                                val skipped = state.allSets - state.doneSets
                                val prs = prKeys.map { it.substringBefore('#') }.distinct().size
                                DockConfirm.Finish(
                                    title = "Finish ${state.planName ?: "workout"}?",
                                    caption = listOfNotNull(
                                        "${state.doneSets} ${plural(state.doneSets, "set")}",
                                        "$prs ${plural(prs, "record")}".takeIf { prs > 0 },
                                        "$skipped skipped".takeIf { skipped > 0 },
                                    ).joinToString(" · "),
                                    planName = state.planName,
                                    planChanges = state.finish?.planChanges.orEmpty(),
                                    saving = state.saving,
                                )
                            }
                            showDiscard -> DockConfirm.Discard(
                                if (state.doneSets > 0) "${state.doneSets} logged ${plural(state.doneSets, "set")} will be lost"
                                else "Nothing will be saved",
                            )
                            else -> null
                        },
                        onConfirmFinish = { updatePlan -> haptics.confirm(); viewModel.confirmFinish(updatePlan) },
                        onConfirmDiscard = { showDiscard = false; viewModel.discard() },
                        onCancelConfirm = { showDiscard = false; viewModel.dismissFinish() },
                        stepSeconds = REST_STEP_SECONDS,
                        // Padding inside the dock's background, so it paints through the inset.
                        insets = WindowInsets.imeAnimationTarget.union(WindowInsets.navigationBars),
                    )
                }
            },
            snackbarHost = { AppSnackbarHost(snackbar) },
            // Each bar handles its own system inset; content padding is just the bars' heights.
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            when {
                // Loading: top bar + dock only, no "add an exercise" flash (house rule 8).
                !state.initialized -> Unit
                reorderMode -> ReorderList(
                    groups = state.groups,
                    haptics = haptics,
                    onCommit = viewModel::reorderGroups,
                    contentPadding = padding,
                )
                state.groups.isEmpty() -> EmptyLogger(onAddExercises = onAddExercises, padding = padding)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + Spacing.l,
                        start = Spacing.gutter,
                        end = Spacing.gutter,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.section),
                ) {
                    itemsIndexed(
                        state.groups,
                        key = { _, g -> g.uid },
                        contentType = { _, _ -> "block" },
                    ) { groupIndex, group ->
                        // Kept across keystrokes: a new SetActions would recompose every set row.
                        val uid = group.uid
                        val sets = remember(groupIndex, uid, haptics, showUndo) {
                            setActions(viewModel, groupIndex, haptics, showUndo) { setIndex -> pulseKey = "$uid#$setIndex" }
                        }
                        ExerciseBlock(
                            group = group,
                            unit = state.weightUnit,
                            collapsed = collapse.isCollapsed(group),
                            prSetIndexes = prIndexes[uid] ?: emptySet(),
                            sets = sets,
                            onToggleCollapsed = { haptics.light(); collapse.toggle(group) },
                            onEditTarget = { targetFor = group.uid },
                            onViewProgress = { onOpenExercise(group.exerciseId) },
                            onRemove = {
                                haptics.light()
                                viewModel.removeGroup(groupIndex)?.let { showUndo("Removed ${it.exerciseName}") }
                            },
                            onSkipChange = { skip ->
                                haptics.light()
                                if (skip) collapse.collapseNow(group)
                                if (skip) viewModel.skipGroup(groupIndex) else viewModel.unskipGroup(groupIndex)
                            },
                            onAddSet = { haptics.light(); viewModel.addSet(groupIndex) },
                            focusFirstSet = focusUid == group.uid,
                            onFocusConsumed = { focusUid = null },
                            pulseSetIndex = pulseKey
                                ?.takeIf { it.substringBefore('#') == group.uid }
                                ?.substringAfter('#')?.toIntOrNull(),
                            onPulseEnd = { pulseKey = null },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "foot", contentType = "foot") {
                        AppOutlinedButton(
                            text = "Add exercises",
                            onClick = onAddExercises,
                            icon = Lucide.Plus,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        // The rest timer taking the whole screen, over the list and both bars.
        AnimatedVisibility(
            visible = restExpanded,
            enter = slideInVertically(tween(280)) { it / 3 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(180)),
        ) {
            RestFocus(
                elapsed = elapsed,
                restElapsed = restElapsed,
                targetSeconds = state.restTargetSeconds,
                next = nextSet,
                onCollapse = { restExpanded = false },
                onTargetClick = { showRest = true },
                onMinus = { haptics.light(); viewModel.adjustRestTarget(-REST_STEP_SECONDS) },
                onPlus = { haptics.light(); viewModel.adjustRestTarget(REST_STEP_SECONDS) },
                onSkip = { haptics.light(); viewModel.skipRest() },
                stepSeconds = REST_STEP_SECONDS,
            )
        }
    }

    if (showDatePicker) {
        LoggerDatePicker(
            epochDay = state.dateEpochDay,
            onPick = { viewModel.setDateFromPicker(it); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showRest) {
        RestSheet(
            restElapsed = restElapsed,
            targetSeconds = state.restTargetSeconds,
            range = REST_MIN_SECONDS..REST_MAX_SECONDS,
            step = REST_STEP_SECONDS,
            onTargetChange = viewModel::setRestTarget,
            onRestart = { haptics.light(); viewModel.restartRest() },
            onDone = { asDefault ->
                if (asDefault) viewModel.saveRestTargetAsDefault()
                showRest = false
            },
            onDismiss = { showRest = false },
        )
    }

    targetFor?.let { uid ->
        val index = state.groups.indexOfFirst { it.uid == uid }
        val group = state.groups.getOrNull(index)
        if (group == null) {
            targetFor = null
        } else {
            TargetSheet(
                exerciseName = group.exerciseName,
                movementType = group.movementType,
                initialLow = group.targetRepsLow,
                initialHigh = group.targetRepsHigh,
                previewSets = group.sets.size,
                onApply = { _, low, high ->
                    viewModel.setRepTarget(index, low, high)
                    targetFor = null
                },
                onDismiss = { targetFor = null },
                onViewProgress = { targetFor = null; onOpenExercise(group.exerciseId) },
                removeLabel = "Remove exercise",
                onRemove = {
                    targetFor = null
                    viewModel.removeGroup(index)?.let { showUndo("Removed ${it.exerciseName}") }
                },
            )
        }
    }

}

/** Per-set callbacks for one block. Typing auto-completes a set (the fast path, §4.3) unless it
 *  would complete the exercise; then [onNudge] pulses that set's check instead. They read the
 *  group from the VM at call time rather than capturing it, so one instance serves the block
 *  across edits. */
private fun setActions(
    viewModel: SessionLoggerViewModel,
    groupIndex: Int,
    haptics: AppHaptics,
    showUndo: (String) -> Unit,
    onNudge: (setIndex: Int) -> Unit,
) = SetActions(
    onWeightChange = { setIndex, v -> input(viewModel, groupIndex, setIndex, haptics, onNudge) { it.copy(weightInput = v) } },
    onRepsChange = { setIndex, v -> input(viewModel, groupIndex, setIndex, haptics, onNudge) { it.copy(repsInput = v) } },
    onCompletedChange = { setIndex, done ->
        haptics.toggle()
        viewModel.setCompleted(groupIndex, setIndex, done)
    },
    onSetTypeChange = { setIndex, type ->
        haptics.light()
        viewModel.updateSet(groupIndex, setIndex) { it.copy(setType = type) }
    },
    onDelete = { setIndex ->
        haptics.light()
        val name = viewModel.state.value.groups.getOrNull(groupIndex)?.exerciseName.orEmpty()
        viewModel.removeSet(groupIndex, setIndex)
        showUndo("Removed set ${setIndex + 1} · $name")
    },
)

private fun input(
    viewModel: SessionLoggerViewModel,
    groupIndex: Int,
    setIndex: Int,
    haptics: AppHaptics,
    onNudge: (setIndex: Int) -> Unit,
    edit: (SetDraft) -> SetDraft,
) {
    when (viewModel.inputSet(groupIndex, setIndex, edit)) {
        InputOutcome.Ticked -> haptics.toggle()
        InputOutcome.NudgeTick -> onNudge(setIndex)
        InputOutcome.None -> Unit
    }
}

/** The top bar's elapsed clock; 0:00 until the session has a start time. */
internal fun elapsedLabel(nowMillis: Long, startedAtMillis: Long): String =
    if (startedAtMillis > 0L) formatClock(nowMillis - startedAtMillis) else formatClock(0)

/** Rest so far, or null while rest is cleared (no set ticked yet, or Skip). */
internal fun restElapsedMillis(nowMillis: Long, lastSetCompletedAtMillis: Long): Long? =
    lastSetCompletedAtMillis.takeIf { it > 0L }?.let { (nowMillis - it).coerceAtLeast(0L) }

/** Rest has reached its target; false while rest is cleared. */
internal fun restReached(restElapsedMillis: Long?, targetSeconds: Int): Boolean =
    restElapsedMillis != null && restElapsedMillis >= targetSeconds * 1000L

/**
 * The rest ring's readout: the time left, counted down (rounded up, so it reads `0:01` until it
 * is really over), then `+0:12` past the target. The target itself while rest is cleared.
 */
internal fun restClockLabel(restElapsedMillis: Long?, targetSeconds: Int): String {
    val target = targetSeconds * 1000L
    return when {
        restElapsedMillis == null -> formatClock(target)
        restElapsedMillis < target -> formatClock((target - restElapsedMillis + 999L) / 1000L * 1000L)
        else -> "+" + formatClock(restElapsedMillis - target)
    }
}

/** How much of the rest is still to go, 1 → 0; 0 while cleared or once the target is reached. */
internal fun restRemainingFraction(restElapsedMillis: Long?, targetSeconds: Int): Float {
    if (restElapsedMillis == null || targetSeconds <= 0) return 0f
    return (1f - restElapsedMillis.toFloat() / (targetSeconds * 1000L)).coerceIn(0f, 1f)
}

/**
 * `Bench Press · set 2 · 60 kg × 8`: the first set still to tick, in order, with what was typed
 * or else last time's hint. Null when every exercise is done or skipped.
 */
internal fun nextSetLabel(groups: List<ExerciseGroup>, unit: WeightUnit): String? {
    for (group in groups) {
        if (group.skipped) continue
        val index = group.sets.indexOfFirst { !it.isCompleted }
        if (index < 0) continue
        val set = group.sets[index]
        val weight = set.weightInput.ifBlank { set.hintWeightKg?.let { trimDecimal(it.kgToDisplay(unit)) }.orEmpty() }
            .takeIf { group.movementType.logsWeight && it.isNotBlank() }
            ?.let { "$it ${unit.label}" }
        val reps = set.repsInput.ifBlank { set.hintReps?.toString().orEmpty() }
            .takeIf { it.isNotBlank() }
            ?.let { "$it ${group.movementType.repUnitShort}" }
        val load = listOfNotNull(weight, reps).joinToString(" × ").ifBlank { null }
        return listOfNotNull(group.exerciseName, "set ${index + 1}", load).joinToString(" · ")
    }
    return null
}

/** [sessionPrKeys]' `"groupUid#setIndex"` keys as set indexes per group uid. */
internal fun prSetIndexesByGroup(prKeys: Set<String>): Map<String, Set<Int>> =
    prKeys.groupBy({ it.substringBeforeLast('#') }, { it.substringAfterLast('#').toInt() })
        .mapValues { (_, indexes) -> indexes.toSet() }

/**
 * Wall-clock millis, refreshed every second while the screen is at least STARTED. While stopped
 * (screen locked between sets, app in background) nothing draws, so ticking then only woke the
 * main thread every second; on return it restarts with a fresh reading, and every readout is
 * derived from timestamps, so it is exact at once.
 */
@Composable
private fun rememberSessionClock(): LongState {
    val clock = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                clock.longValue = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    return clock
}

/** One `confirm` haptic when rest crosses its target — only if we saw it below target first,
 *  so resuming a long-rested workout doesn't buzz. Re-arms when the base or target changes.
 *  It watches derived booleans, so a clock tick recomposes it only when one of them flips. */
@Composable
private fun RestReachedHaptic(restElapsed: () -> Long?, targetSeconds: Int, base: Long, haptics: AppHaptics) {
    val running by remember(restElapsed) { derivedStateOf { restElapsed() != null } }
    val reached by remember(restElapsed, targetSeconds) {
        derivedStateOf { restReached(restElapsed(), targetSeconds) }
    }
    var armed by remember(base, targetSeconds) { mutableStateOf(false) }
    LaunchedEffect(reached, running, base, targetSeconds) {
        if (running && !reached) armed = true
        if (reached && armed) {
            haptics.confirm()
            armed = false
        }
    }
}

/** `confirm` haptic when a new PR appears — only once every baseline has loaded, so the async
 *  load itself never looks like a fresh record (e.g. resuming a session that already hit one). */
@Composable
private fun PrHaptic(state: SessionLoggerState, prKeys: Set<String>, haptics: AppHaptics) {
    val baselinesReady = state.groups.isNotEmpty() &&
        state.groups.all { state.baselineRecords.containsKey(it.exerciseId) }
    var seen by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(prKeys, baselinesReady) {
        if (!baselinesReady) return@LaunchedEffect
        val prev = seen
        if (prev != null && (prKeys - prev).isNotEmpty()) haptics.confirm()
        seen = prKeys
    }
}

/**
 * Which settled blocks are folded (§7 phase 3: derived from `sets.all { isCompleted }` or the
 * group's skipped flag, plus a user override never stored in the draft). A block that isn't
 * settled is always expanded.
 */
private class CollapseState(
    autoCollapsed: Set<String>,
    expanded: Set<String>,
    collapsed: Set<String>,
    seeded: Boolean,
) {
    var autoCollapsed by mutableStateOf(autoCollapsed)
    var userExpanded by mutableStateOf(expanded)
    var userCollapsed by mutableStateOf(collapsed)
    var seeded by mutableStateOf(seeded)

    fun isCollapsed(group: ExerciseGroup): Boolean = group.settled && when (group.uid) {
        in userCollapsed -> true
        in userExpanded -> false
        else -> group.uid in autoCollapsed
    }

    fun toggle(group: ExerciseGroup) {
        if (isCollapsed(group)) {
            userCollapsed = userCollapsed - group.uid
            userExpanded = userExpanded + group.uid
        } else {
            userExpanded = userExpanded - group.uid
            userCollapsed = userCollapsed + group.uid
        }
    }

    /** Skip exercise folds the block at once — the user chose it, no delay. */
    fun collapseNow(group: ExerciseGroup) {
        userExpanded = userExpanded - group.uid
        userCollapsed = userCollapsed - group.uid
        autoCollapsed = autoCollapsed + group.uid
    }
}

@Composable
private fun rememberCollapseState(): CollapseState = rememberSaveable(
    saver = androidx.compose.runtime.saveable.listSaver(
        save = {
            listOf(
                it.autoCollapsed.toList(),
                it.userExpanded.toList(),
                it.userCollapsed.toList(),
                listOf(it.seeded.toString()),
            )
        },
        restore = {
            CollapseState(it[0].toSet(), it[1].toSet(), it[2].toSet(), it[3].firstOrNull() == "true")
        },
    ),
) { CollapseState(emptySet(), emptySet(), emptySet(), seeded = false) }

/** Blocks already finished when the logger opens fold at once; a block finished *now* folds
 *  600ms later and the next block scrolls to the top (§1 motion, §6.2 step 5). */
@Composable
private fun CollapseEffects(
    state: SessionLoggerState,
    collapse: CollapseState,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val doneUids = state.groups.filter { it.settled }.map { it.uid }.toSet()
    LaunchedEffect(state.initialized, doneUids) {
        if (!state.initialized) return@LaunchedEffect
        if (!collapse.seeded) {
            collapse.autoCollapsed = doneUids
            collapse.seeded = true
            return@LaunchedEffect
        }
        // Unticking a set (or unskipping) re-opens its block immediately and forgets the user's
        // fold/unfold choice, so the block folds again once it settles.
        collapse.autoCollapsed = collapse.autoCollapsed intersect doneUids
        collapse.userExpanded = collapse.userExpanded intersect doneUids
        collapse.userCollapsed = collapse.userCollapsed intersect doneUids
        val fresh = doneUids - collapse.autoCollapsed - collapse.userExpanded
        if (fresh.isEmpty()) return@LaunchedEffect
        delay(COLLAPSE_DELAY_MILLIS)
        collapse.autoCollapsed = collapse.autoCollapsed + fresh
        val lastFresh = state.groups.indexOfLast { it.uid in fresh }
        val next = state.groups.withIndex()
            .firstOrNull { (i, g) -> i > lastFresh && !g.settled }?.index
        if (next != null) listState.animateScrollToItem(next)
    }
}

@Composable
private fun EmptyLogger(onAddExercises: () -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Add your first exercise", style = AppText.titleM, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.l))
        AppButton(text = "Add exercises", onClick = onAddExercises, icon = Lucide.Plus)
    }
}

/** Change date (§4.3 change 1): capped at *local* today; the picker's UTC-midnight millis are
 *  converted by the VM, so the logged day is the day tapped (issue 15). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggerDatePicker(epochDay: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = remember { DateUtil.todayEpochDay() }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtil.epochDayToPickerMillis(epochDay),
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    DateUtil.pickerMillisToEpochDay(utcTimeMillis) <= today
                override fun isSelectableYear(year: Int) = year <= LocalDate.ofEpochDay(today).year
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppTextButton(
                text = "Set date",
                onClick = { pickerState.selectedDateMillis?.let(onPick) ?: onDismiss() },
            )
        },
        dismissButton = {
            AppTextButton(text = "Cancel", onClick = onDismiss, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    ) { DatePicker(state = pickerState) }
}

/**
 * Reorder mode (entered from ⋮): compact `PlannedRow`-style lines — index, name, set count,
 * handle — on hairlines. Long-press a row to pick it up. The drag reorders a *local* copy and
 * commits once on release, so the gesture itself has no side effects; cancel restores.
 */
@Composable
private fun ReorderList(
    groups: List<ExerciseGroup>,
    haptics: AppHaptics,
    onCommit: (List<String>) -> Unit,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    val latestGroups by rememberUpdatedState(groups)
    val commit by rememberUpdatedState(onCommit)
    var order by remember { mutableStateOf(groups) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    // Mirror the VM while idle (e.g. an undo landing mid-mode).
    LaunchedEffect(groups) { if (draggedKey == null) order = groups }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + Spacing.s,
            bottom = contentPadding.calculateBottomPadding() + Spacing.l,
            start = Spacing.gutter,
            end = Spacing.gutter,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        itemsIndexed(order, key = { _, g -> g.uid }) { index, group ->
            val dragging = draggedKey == group.uid
            Column(
                Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffsetY else 0f }
                    .then(if (dragging) Modifier else Modifier.animateItem())
                    .clip(RoundedCornerShape(Radius.md))
                    .background(if (dragging) scheme.surfaceContainerHigh else cardColor())
                    .pointerInput(group.uid) {
                        fun end(save: Boolean) {
                            if (save) commit(order.map { it.uid }) else order = latestGroups
                            draggedKey = null
                            dragOffsetY = 0f
                        }
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedKey = group.uid
                                dragOffsetY = 0f
                                haptics.reorder()
                            },
                            onDragEnd = { end(save = true) },
                            onDragCancel = { end(save = false) },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffsetY += amount.y
                                val key = draggedKey ?: return@detectDragGesturesAfterLongPress
                                val info = listState.layoutInfo
                                val curInfo = info.visibleItemsInfo.firstOrNull { it.key == key }
                                    ?: return@detectDragGesturesAfterLongPress
                                val cur = order.indexOfFirst { it.uid == key }
                                val center = curInfo.offset + curInfo.size / 2 + dragOffsetY
                                val target = info.visibleItemsInfo.firstOrNull {
                                    it.key != key && center.toInt() in it.offset..(it.offset + it.size)
                                } ?: return@detectDragGesturesAfterLongPress
                                val targetIndex = order.indexOfFirst { it.uid == target.key }
                                if (targetIndex < 0 || targetIndex == cur) return@detectDragGesturesAfterLongPress
                                order = order.toMutableList().apply { add(targetIndex, removeAt(cur)) }
                                // Re-anchor under the finger; moving down, anchor on the target's
                                // height (rows can differ) or the row jumps and oscillates.
                                val newBase = if (targetIndex > cur) target.offset + target.size - curInfo.size
                                else target.offset
                                dragOffsetY = (curInfo.offset + dragOffsetY) - newBase
                                haptics.reorder()
                            },
                        )
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.touchTarget)
                        .padding(horizontal = Spacing.cardPadding, vertical = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = AppText.numberM,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.width(Sizes.icon + Spacing.s),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            group.exerciseName,
                            style = AppText.titleM,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${group.sets.size} ${plural(group.sets.size, "set")}",
                            style = AppText.bodyS,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Icon(Lucide.GripVertical, contentDescription = "Drag to reorder", tint = scheme.onSurfaceVariant)
                }
            }
        }
    }
}
