package com.example.gymtracker.ui.screens.logger

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DragHandle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.ui.AppHaptics
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.TargetSheet
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.ConfirmSheet
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel.Companion.REST_MAX_SECONDS
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel.Companion.REST_MIN_SECONDS
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel.Companion.REST_STEP_SECONDS
import com.example.gymtracker.ui.screens.logger.components.ExerciseBlock
import com.example.gymtracker.ui.screens.logger.components.FinishSheet
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
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Undo snackbars stay 5s (§6.6) — between Material's Short (4s) and Long (10s). */
private const val UNDO_MILLIS = 5_000L

/** §1 motion: a finished exercise folds 600ms after its last set is ticked. */
private const val COLLAPSE_DELAY_MILLIS = 600L

/**
 * Session logger — design-plan §4.3, the reference implementation.
 *
 * Top bar (clock, Finish, ⋮, progress) and the rest dock are fixed; one `LazyColumn` of
 * [ExerciseBlock]s scrolls between them using the Scaffold's content padding. The dock is the
 * only IME consumer, so the list's bottom padding follows the dock above the keyboard and
 * nothing double-counts the inset.
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
    var targetFor by remember { mutableStateOf<String?>(null) }
    var reorderMode by rememberSaveable { mutableStateOf(false) }
    if (state.groups.size < 2 && reorderMode) reorderMode = false

    LaunchedEffect(state.savedSessionId) {
        state.savedSessionId?.let { haptics.confirm(); onFinished(it) }
    }
    LaunchedEffect(state.discarded) { if (state.discarded) onDiscarded() }

    // One clock drives both the elapsed and the rest readouts.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val elapsed = if (state.startedAtMillis > 0L) formatClock(nowMillis - state.startedAtMillis) else formatClock(0)
    val restElapsed = state.lastSetCompletedAtMillis.takeIf { it > 0L }?.let { (nowMillis - it).coerceAtLeast(0L) }
    RestReachedHaptic(restElapsed, state.restTargetSeconds, state.lastSetCompletedAtMillis, haptics)

    // PRs: sets that currently beat the pre-session records.
    val prKeys = remember(state.groups, state.baselineRecords, state.weightUnit) {
        sessionPrKeys(state.groups, state.baselineRecords, state.weightUnit)
    }
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

    fun showUndo(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = withTimeoutOrNull(UNDO_MILLIS) {
                snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Indefinite)
            }
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
        }
    }

    val dateLabel = remember(state.dateEpochDay) {
        if (state.dateEpochDay == DateUtil.todayEpochDay()) null
        else LocalDate.ofEpochDay(state.dateEpochDay).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }
    val title = state.planName ?: "Quick log"

    Scaffold(
        topBar = {
            if (reorderMode) {
                AppTopBar(
                    title = "Reorder exercises",
                    actions = { AppTextButton(text = "Done", onClick = { haptics.light(); reorderMode = false }) },
                )
            } else {
                LoggerTopBar(
                    elapsed = elapsed,
                    eyebrow = listOfNotNull(
                        title,
                        "${state.doneSets}/${state.totalSets}".takeIf { state.totalSets > 0 },
                        dateLabel,
                    ).joinToString(" · "),
                    progress = if (state.totalSets == 0) 0f else state.doneSets.toFloat() / state.totalSets,
                    finishEnabled = state.initialized && state.groups.isNotEmpty() && !state.saving,
                    onMinimise = onMinimise,
                    onFinish = { haptics.light(); viewModel.openFinish() },
                    menu = listOf(
                        MenuItem("Change date", { showDatePicker = true }, enabled = state.initialized),
                        MenuItem(
                            "Reorder exercises",
                            { haptics.light(); reorderMode = true },
                            enabled = state.groups.size >= 2,
                        ),
                        MenuItem("Discard workout", { showDiscard = true }, destructive = true),
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
                    restElapsedMillis = restElapsed,
                    targetSeconds = state.restTargetSeconds,
                    enabled = state.initialized && state.groups.isNotEmpty(),
                    compact = imeOpen,
                    onClockClick = { showRest = true },
                    onMinus = { haptics.light(); viewModel.adjustRestTarget(-REST_STEP_SECONDS) },
                    onPlus = { haptics.light(); viewModel.adjustRestTarget(REST_STEP_SECONDS) },
                    onSkip = { haptics.light(); viewModel.skipRest() },
                    stepSeconds = REST_STEP_SECONDS,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.imeAnimationTarget.union(WindowInsets.navigationBars),
                    ),
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
                ),
            ) {
                itemsIndexed(state.groups, key = { _, g -> g.uid }) { groupIndex, group ->
                    ExerciseBlock(
                        group = group,
                        unit = state.weightUnit,
                        collapsed = collapse.isCollapsed(group),
                        prSetIndexes = group.sets.indices.filterTo(HashSet()) { "${group.uid}#$it" in prKeys },
                        sets = setActions(viewModel, groupIndex, group, haptics, ::showUndo),
                        onToggleCollapsed = { haptics.light(); collapse.toggle(group) },
                        onEditTarget = { targetFor = group.uid },
                        onViewProgress = { onOpenExercise(group.exerciseId) },
                        onRemove = {
                            haptics.light()
                            viewModel.removeGroup(groupIndex)?.let { showUndo("Removed ${it.exerciseName}") }
                        },
                        onAddSet = { haptics.light(); viewModel.addSet(groupIndex) },
                        focusFirstSet = focusUid == group.uid,
                        onFocusConsumed = { focusUid = null },
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "foot") {
                    AppTextButton(
                        text = "Add exercises",
                        onClick = onAddExercises,
                        icon = Icons.Outlined.Add,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.s),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        LoggerDatePicker(
            epochDay = state.dateEpochDay,
            onPick = { viewModel.setDateFromPicker(it); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showDiscard) {
        ConfirmSheet(
            title = "Discard workout?",
            message = if (state.doneSets > 0) "${state.doneSets} logged ${plural(state.doneSets, "set")} will be lost."
            else "Nothing from this workout will be saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep logging",
            onConfirm = { showDiscard = false; viewModel.discard() },
            onDismiss = { showDiscard = false },
        )
    }

    if (showRest) {
        RestSheet(
            restElapsedMillis = restElapsed,
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

    state.finish?.let { prompt ->
        FinishSheet(
            title = "Finish ${state.planName ?: "workout"}?",
            elapsed = elapsed,
            doneSets = state.doneSets,
            totalSets = state.totalSets,
            prCount = prKeys.map { it.substringBefore('#') }.distinct().size,
            planName = state.planName,
            planChanges = prompt.planChanges,
            saving = state.saving,
            onFinish = { updatePlan -> viewModel.confirmFinish(updatePlan) },
            onDismiss = viewModel::dismissFinish,
        )
    }
}

/** Per-set callbacks for one block. Typing still auto-completes a set (the fast path, §4.3). */
private fun setActions(
    viewModel: SessionLoggerViewModel,
    groupIndex: Int,
    group: ExerciseGroup,
    haptics: AppHaptics,
    showUndo: (String) -> Unit,
) = SetActions(
    onWeightChange = { setIndex, v -> autoComplete(viewModel, groupIndex, group, setIndex, haptics) { it.copy(weightInput = v) } },
    onRepsChange = { setIndex, v -> autoComplete(viewModel, groupIndex, group, setIndex, haptics) { it.copy(repsInput = v) } },
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
        viewModel.removeSet(groupIndex, setIndex)
        showUndo("Removed set ${setIndex + 1} · ${group.exerciseName}")
    },
)

private fun autoComplete(
    viewModel: SessionLoggerViewModel,
    groupIndex: Int,
    group: ExerciseGroup,
    setIndex: Int,
    haptics: AppHaptics,
    edit: (SetDraft) -> SetDraft,
) {
    val before = group.sets.getOrNull(setIndex) ?: return
    val after = edit(before)
    val completes = !before.isCompleted && (after.repsInput.isNotEmpty() || after.weightInput.isNotEmpty())
    if (completes) haptics.toggle()
    viewModel.updateSet(groupIndex, setIndex) {
        val edited = edit(it)
        edited.copy(isCompleted = it.isCompleted || edited.repsInput.isNotEmpty() || edited.weightInput.isNotEmpty())
    }
}

/** One `confirm` haptic when rest crosses its target — only if we saw it below target first,
 *  so resuming a long-rested workout doesn't buzz. Re-arms when the base or target changes. */
@Composable
private fun RestReachedHaptic(restElapsed: Long?, targetSeconds: Int, base: Long, haptics: AppHaptics) {
    val reached = restElapsed != null && restElapsed >= targetSeconds * 1000L
    var armed by remember(base, targetSeconds) { mutableStateOf(false) }
    LaunchedEffect(reached, restElapsed != null, base, targetSeconds) {
        if (restElapsed != null && !reached) armed = true
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
 * Which finished blocks are folded (§7 phase 3: derived from `sets.all { isCompleted }` plus a
 * user override, never stored in the draft). A block that isn't finished is always expanded.
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

    fun isCollapsed(group: ExerciseGroup): Boolean = group.allDone && when (group.uid) {
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
    val doneUids = state.groups.filter { it.allDone }.map { it.uid }.toSet()
    LaunchedEffect(state.initialized, doneUids) {
        if (!state.initialized) return@LaunchedEffect
        if (!collapse.seeded) {
            collapse.autoCollapsed = doneUids
            collapse.seeded = true
            return@LaunchedEffect
        }
        // Unticking a set re-opens its block immediately.
        collapse.autoCollapsed = collapse.autoCollapsed intersect doneUids
        val fresh = doneUids - collapse.autoCollapsed - collapse.userExpanded
        if (fresh.isEmpty()) return@LaunchedEffect
        delay(COLLAPSE_DELAY_MILLIS)
        collapse.autoCollapsed = collapse.autoCollapsed + fresh
        val lastFresh = state.groups.indexOfLast { it.uid in fresh }
        val next = state.groups.withIndex()
            .firstOrNull { (i, g) -> i > lastFresh && !g.allDone }?.index
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
        AppButton(text = "Add exercises", onClick = onAddExercises, icon = Icons.Outlined.Add)
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
        contentPadding = contentPadding,
    ) {
        itemsIndexed(order, key = { _, g -> g.uid }) { index, group ->
            val dragging = draggedKey == group.uid
            Column(
                Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffsetY else 0f }
                    .then(if (dragging) Modifier else Modifier.animateItem())
                    .background(if (dragging) scheme.surfaceContainerHigh else scheme.background)
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
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.m),
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
                    Icon(Icons.Outlined.DragHandle, contentDescription = "Drag to reorder", tint = scheme.onSurfaceVariant)
                }
                AppDivider()
            }
        }
    }
}
