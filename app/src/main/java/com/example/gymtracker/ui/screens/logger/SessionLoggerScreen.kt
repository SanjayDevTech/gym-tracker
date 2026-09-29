package com.example.gymtracker.ui.screens.logger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.displayToKg
import com.example.gymtracker.data.formatTarget
import com.example.gymtracker.data.repUnitShort
import com.example.gymtracker.domain.ExerciseProgress
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.PersonalRecords
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.EmptyState
import com.example.gymtracker.ui.components.ExercisePickerSheet
import com.example.gymtracker.ui.components.Section
import com.example.gymtracker.ui.components.SetRow
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.Dimens
import com.example.gymtracker.ui.AppHaptics
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionLoggerScreen(
    planId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var editTargetIndex by remember { mutableStateOf<Int?>(null) }
    // When on, the exercise list collapses to compact draggable rows for reordering. Auto-exits once
    // fewer than two exercises remain (nothing left to reorder).
    var reorderMode by remember { mutableStateOf(false) }
    if (state.groups.size < 2 && reorderMode) reorderMode = false

    LaunchedEffect(state.saved) {
        if (state.saved) { haptics.confirm(); onSaved() }
    }
    LaunchedEffect(state.discarded) {
        if (state.discarded) onBack()
    }

    val dateLabel = remember(state.dateEpochDay) {
        LocalDate.ofEpochDay(state.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.startedAtMillis) {
        if (state.startedAtMillis > 0L) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val elapsedLabel = remember(state.startedAtMillis, nowMillis) {
        if (state.startedAtMillis <= 0L) ""
        else formatElapsed(nowMillis - state.startedAtMillis)
    }
    // Rest timer: time since the last set was logged. Resets on each completed set, so a glance
    // tells you how long you've rested. Before the first set is logged it shows a dormant "--:--"
    // that the user can tap to start manually.
    val restLabel = remember(state.lastSetCompletedAtMillis, nowMillis) {
        if (state.lastSetCompletedAtMillis <= 0L) "--:--"
        else formatElapsed(nowMillis - state.lastSetCompletedAtMillis)
    }
    // Once you've passed a typical rest window, the chip lifts to an emphasized "ready" look so the
    // answer to "rested enough?" is readable at a glance without parsing the digits.
    val restReady = state.lastSetCompletedAtMillis > 0L &&
        nowMillis - state.lastSetCompletedAtMillis >= REST_READY_MILLIS
    // Fixed-width figures so the ticking time doesn't shift the chip's width each second.
    val tabularStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")

    val totalSets = state.groups.sumOf { it.sets.size }
    val doneSets = state.groups.sumOf { g -> g.sets.count { it.isCompleted } }

    // Sets that currently beat the pre-session records — keyed "groupUid#setIndex".
    val prKeys = remember(state.groups, state.baselineRecords, state.weightUnit) {
        computeSessionPrKeys(state.groups, state.baselineRecords, state.weightUnit)
    }
    // Only trust prKeys once every visible exercise's baseline has loaded, so the async load itself
    // never looks like a fresh PR (e.g. when resuming a session that already hit records).
    val baselinesReady = state.groups.isNotEmpty() &&
        state.groups.all { state.baselineRecords.containsKey(it.exerciseId) }
    var seenPrKeys by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(prKeys, baselinesReady) {
        if (!baselinesReady) return@LaunchedEffect
        val prev = seenPrKeys
        if (prev != null && (prKeys - prev).isNotEmpty()) haptics.confirm()
        seenPrKeys = prKeys
    }

    Scaffold(
        topBar = {
            if (reorderMode) {
                TopAppBar(
                    title = { Text("Reorder exercises", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        TextButton(onClick = { haptics.light(); reorderMode = false }) { Text("Done") }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (planId == null) "Quick log" else "Log session",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            state.planName?.let { name ->
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (state.groups.size >= 2) {
                            IconButton(onClick = { haptics.light(); reorderMode = true }) {
                                Icon(Icons.Outlined.SwapVert, contentDescription = "Reorder exercises")
                            }
                        }
                        IconButton(onClick = { showDiscard = true }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Discard session")
                        }
                    },
                )
            }
        },
        bottomBar = {
            // Hidden while reordering — the compact list has its own "Done" affordance.
            if (!reorderMode) {
            // Carry the inset on an (invisible) wrapper rather than inside the Surface, so the bar
            // lifts to sit just above the keyboard without its background painting over the IME
            // region. union(ime, navigationBars) => nav-bar height when closed, IME height when
            // open — never both stacked, which is what left a black gap above the keyboard before.
            //
            // imeAnimationTarget (the IME's final resting inset) rather than ime (the live animated
            // value): the animated value can stick at its last frame when the keyboard is dismissed
            // by the system back button (Google issue 229769102), leaving dead space below the
            // content. The target is set to 0 the instant a hide begins, so it always resets.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.imeAnimationTarget.union(WindowInsets.navigationBars)),
            ) {
              Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.GutterM, vertical = Dimens.GutterS),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GutterS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Add exercise")
                    }
                    Button(
                        onClick = { viewModel.attemptSave() },
                        enabled = state.groups.isNotEmpty() && !state.saving,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Finish")
                        }
                    }
                }
              }
            }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!reorderMode) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.GutterM, vertical = Dimens.GutterS),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterS),
                verticalArrangement = Arrangement.spacedBy(Dimens.GutterXs),
            ) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                    label = { Text(dateLabel) },
                )
                if (elapsedLabel.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        leadingIcon = {
                            Icon(Icons.Outlined.Timer, contentDescription = "Session time",
                                modifier = Modifier.size(AssistChipDefaults.IconSize))
                        },
                        label = { Text(elapsedLabel, style = tabularStyle) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
                // Always present so the user can start it manually before the first set; muted while
                // still resting (or dormant), lifting to a tertiary "ready" emphasis past the window.
                val restContainer =
                    if (restReady) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                val restContent =
                    if (restReady) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                AssistChip(
                    onClick = { haptics.light(); viewModel.resetRestTimer() },
                    leadingIcon = {
                        Icon(Icons.Outlined.HourglassEmpty, contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                    label = { Text("Rest $restLabel", style = tabularStyle) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = restContainer,
                        labelColor = restContent,
                        leadingIconContentColor = restContent,
                    ),
                    border = null,
                )
                if (totalSets > 0) {
                    Text(
                        "$doneSets/$totalSets done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            }

            if (reorderMode) {
                ReorderExerciseList(
                    groups = state.groups,
                    haptics = haptics,
                    onMove = { from, to -> viewModel.moveGroup(from, to - from) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                )
            } else if (state.groups.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.FitnessCenter,
                    message = "Tap “Add exercise” to start logging.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.GutterM),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GutterS),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
                ) {
                    itemsIndexed(state.groups, key = { _, group -> group.uid }) { groupIndex, group ->
                        val groupDone = group.sets.count { it.isCompleted }
                        Section(modifier = Modifier.fillMaxWidth().animateItem()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterS),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.exerciseName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Target " + formatTarget(
                                            sets = group.sets.size,
                                            low = group.targetRepsLow,
                                            high = group.targetRepsHigh,
                                            type = group.movementType,
                                        ) + "  ·  $groupDone/${group.sets.size} done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { haptics.light(); editTargetIndex = groupIndex }
                                            .padding(vertical = 2.dp),
                                    )
                                }
                                IconButton(onClick = { haptics.light(); viewModel.removeGroup(groupIndex) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove")
                                }
                            }

                            SetColumnHeader(
                                repsLabel = group.movementType.repUnitShort.replaceFirstChar { it.uppercase() },
                                weightLabel = "Weight (${state.weightUnit.label})",
                            )

                            group.sets.forEachIndexed { setIndex, set ->
                                SetRow(
                                    index = setIndex,
                                    repsInput = set.repsInput,
                                    weightInput = set.weightInput,
                                    hintReps = set.hintReps,
                                    hintWeightKg = set.hintWeightKg,
                                    isCompleted = set.isCompleted,
                                    setType = set.setType,
                                    unit = state.weightUnit,
                                    movementType = group.movementType,
                                    isPr = prKeys.contains("${group.uid}#$setIndex"),
                                    onRepsChange = { v ->
                                        viewModel.updateSet(groupIndex, setIndex) {
                                            it.copy(repsInput = v, isCompleted = it.isCompleted || v.isNotEmpty())
                                        }
                                    },
                                    onWeightChange = { v ->
                                        viewModel.updateSet(groupIndex, setIndex) {
                                            it.copy(weightInput = v, isCompleted = it.isCompleted || v.isNotEmpty())
                                        }
                                    },
                                    onCompletedChange = { c ->
                                        haptics.toggle()
                                        viewModel.updateSet(groupIndex, setIndex) { it.copy(isCompleted = c) }
                                    },
                                    onSetTypeChange = { t ->
                                        haptics.light()
                                        viewModel.updateSet(groupIndex, setIndex) { it.copy(setType = t) }
                                    },
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterS),
                                modifier = Modifier.padding(top = Dimens.GutterXs),
                            ) {
                                FilledTonalButton(onClick = { haptics.light(); viewModel.addSet(groupIndex) }) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Text("  Add set")
                                }
                                FilledTonalIconButton(
                                    onClick = { haptics.light(); viewModel.removeSet(groupIndex, group.sets.lastIndex) },
                                    enabled = group.sets.size > 1,
                                ) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "Remove last set")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPicker) {
            ExercisePickerSheet(
                repository = container.repository,
                onPick = {
                    viewModel.addExerciseFreestyle(it)
                    showPicker = false
                },
                onDismiss = { showPicker = false },
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.dateEpochDay * 86_400_000L,
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        val epochDay = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
                        viewModel.setDate(epochDay)
                        showDatePicker = false
                    }) { Text("Set date") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                },
            ) { DatePicker(state = datePickerState) }
        }

        if (showDiscard) {
            DiscardSessionSheet(
                onCancel = { showDiscard = false },
                onConfirm = {
                    showDiscard = false
                    viewModel.discard()
                },
            )
        }

        editTargetIndex?.let { idx ->
            state.groups.getOrNull(idx)?.let { group ->
                EditRepTargetSheet(
                    exerciseName = group.exerciseName,
                    movementType = group.movementType,
                    initialLow = group.targetRepsLow,
                    initialHigh = group.targetRepsHigh,
                    onDismiss = { editTargetIndex = null },
                    onConfirm = { low, high ->
                        viewModel.setRepTarget(idx, low, high)
                        editTargetIndex = null
                    },
                )
            } ?: run { editTargetIndex = null }
        }

        if (state.askPlanSync) {
            SavePlanChangesSheet(
                planName = state.planName,
                onCancel = { viewModel.dismissPlanSync() },
                onJustLog = { viewModel.confirmSave(updatePlan = false) },
                onUpdatePlan = { viewModel.confirmSave(updatePlan = true) },
            )
        }

        if (state.askConfirmFinish) {
            FinishSessionSheet(
                onCancel = { viewModel.dismissConfirmFinish() },
                onConfirm = { viewModel.confirmFinish() },
            )
        }
    }
}

/**
 * Compact drag-to-reorder list shown while the logger is in "reorder mode". Each exercise collapses
 * to a fixed-feel row (drag handle · name · set count) so the moving target is small and easy to
 * track — unlike dragging the tall logging cards. Long-press a row to pick it up.
 *
 * The drag state (which row, how far) is hoisted here, above the items, so it survives the item
 * recompositions that reordering triggers. During a drag we read the *live* list and the list's
 * layout info (never the stale values captured when the gesture's pointerInput first ran) to decide
 * which neighbour the dragged row's centre has crossed, then move it there and re-anchor the offset.
 */
@Composable
private fun ReorderExerciseList(
    groups: List<ExerciseGroup>,
    haptics: AppHaptics,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Always the current list, even inside the long-lived drag lambdas whose pointerInput is keyed
    // by uid and therefore never restarts (so it would otherwise capture the list from drag-start).
    val liveGroups = rememberUpdatedState(groups)
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = Dimens.GutterM),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterS),
        contentPadding = PaddingValues(vertical = Dimens.GutterS),
    ) {
        itemsIndexed(groups, key = { _, group -> group.uid }) { _, group ->
            val dragging = draggedKey == group.uid
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = if (dragging) 6.dp else 0.dp,
                shadowElevation = if (dragging) 8.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffsetY else 0f }
                    .then(if (dragging) Modifier else Modifier.animateItem())
                    .pointerInput(group.uid) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedKey = group.uid
                                dragOffsetY = 0f
                                haptics.reorder()
                            },
                            onDragEnd = { draggedKey = null; dragOffsetY = 0f },
                            onDragCancel = { draggedKey = null; dragOffsetY = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffsetY += amount.y
                                val key = draggedKey
                                val info = listState.layoutInfo
                                val curInfo = info.visibleItemsInfo.firstOrNull { it.key == key }
                                val cur = liveGroups.value.indexOfFirst { it.uid == key }
                                if (key != null && curInfo != null && cur >= 0) {
                                    val center = curInfo.offset + curInfo.size / 2 + dragOffsetY
                                    val target = info.visibleItemsInfo.firstOrNull {
                                        it.key != key && center.toInt() in it.offset..(it.offset + it.size)
                                    }
                                    if (target != null) {
                                        val targetIndex = liveGroups.value.indexOfFirst { it.uid == target.key }
                                        if (targetIndex >= 0 && targetIndex != cur) {
                                            onMove(cur, targetIndex)
                                            // Re-anchor the row under the finger. Moving down it lands past a
                                            // neighbour whose height may differ, so anchor on the target's
                                            // height rather than the dragged row's — else mismatched heights
                                            // make it jump and oscillate.
                                            val newBase = if (targetIndex > cur)
                                                target.offset + target.size - curInfo.size
                                            else
                                                target.offset
                                            dragOffsetY = (curInfo.offset + dragOffsetY) - newBase
                                            haptics.reorder()
                                        }
                                    }
                                }
                            },
                        )
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GutterS),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.GutterM, vertical = 16.dp),
                ) {
                    Icon(
                        Icons.Outlined.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        group.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${group.sets.size} ${if (group.sets.size == 1) "set" else "sets"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Column captions so the two text fields are self-explanatory. Aligned to the SetRow layout. */
@Composable
private fun SetColumnHeader(repsLabel: String, weightLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.GutterXs, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val captionColor = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            "Set",
            style = MaterialTheme.typography.labelSmall,
            color = captionColor,
            modifier = Modifier.width(30.dp),
        )
        Text(repsLabel, style = MaterialTheme.typography.labelSmall, color = captionColor, modifier = Modifier.weight(1f))
        Text(weightLabel, style = MaterialTheme.typography.labelSmall, color = captionColor, modifier = Modifier.weight(1f))
        Text("Done", style = MaterialTheme.typography.labelSmall, color = captionColor, modifier = Modifier.width(48.dp))
    }
}

/**
 * Resolve each draft set (typed value, else its history hint) the same way Save does, then ask the
 * domain which sets beat their exercise's pre-session records. Returns "groupUid#setIndex" keys.
 */
private fun computeSessionPrKeys(
    groups: List<ExerciseGroup>,
    baselines: Map<Long, PersonalRecords>,
    unit: WeightUnit,
): Set<String> {
    val keys = HashSet<String>()
    groups.forEach { group ->
        val baseline = baselines[group.exerciseId] ?: return@forEach
        val samples = group.sets.map { s ->
            LoggedSetSample(
                dateEpochDay = 0L,
                repsDone = s.repsInput.toIntOrNull() ?: s.hintReps,
                weightKg = s.weightInput.toFloatOrNull()?.displayToKg(unit) ?: s.hintWeightKg,
                isCompleted = s.isCompleted,
                setType = s.setType,
            )
        }
        ExerciseProgress.sessionPrs(baseline, samples).keys.forEach { idx ->
            keys.add("${group.uid}#$idx")
        }
    }
    return keys
}

/** Typical rest window; past this the rest chip switches to its "ready for the next set" emphasis. */
private const val REST_READY_MILLIS = 90_000L

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSec = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscardSessionSheet(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Discard this session?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Everything you've entered will be lost. This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text("Keep") }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Discard") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishSessionSheet(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Finish this session?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Your logged sets will be saved to history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Finish & save") }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.End),
            ) { Text("Keep logging") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavePlanChangesSheet(
    planName: String?,
    onCancel: () -> Unit,
    onJustLog: () -> Unit,
    onUpdatePlan: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Save changes to plan?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "You changed the exercises or sets during this session. Update " +
                    (planName?.let { "“$it”" } ?: "the plan") +
                    " to match, or just log this session and leave the plan as it was.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onUpdatePlan,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Update plan & log") }
            FilledTonalButton(
                onClick = onJustLog,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Just log this session") }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.End),
            ) { Text("Cancel") }
        }
    }
}

/** Edit an exercise's target rep range mid-session. Min/max steppers stay consistent (max ≥ min),
 *  and a live preview mirrors how the target reads on the card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRepTargetSheet(
    exerciseName: String,
    movementType: MovementType,
    initialLow: Int,
    initialHigh: Int,
    onDismiss: () -> Unit,
    onConfirm: (low: Int, high: Int) -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var low by remember { mutableIntStateOf(initialLow) }
    var high by remember { mutableIntStateOf(initialHigh) }
    val unit = movementType.repUnitShort
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Target $unit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepperRow(
                label = "Min",
                value = low,
                onDecrease = { low = (low - 1).coerceAtLeast(1) },
                onIncrease = { low += 1; if (high < low) high = low },
            )
            StepperRow(
                label = "Max",
                value = high,
                onDecrease = { high = (high - 1).coerceAtLeast(low) },
                onIncrease = { high += 1 },
            )
            Button(
                onClick = { onConfirm(low, high) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Done") }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { haptics.light(); onDecrease() }) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease $label")
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(48.dp),
        )
        FilledTonalIconButton(onClick = { haptics.light(); onIncrease() }) {
            Icon(Icons.Outlined.Add, contentDescription = "Increase $label")
        }
    }
}
