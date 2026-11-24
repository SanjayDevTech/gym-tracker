package com.example.gymtracker.ui.screens.plans

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.PlannedWithExercise
import com.example.gymtracker.data.displayName
import com.example.gymtracker.data.formatTarget
import com.example.gymtracker.data.plan.PlanExport
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.NoteLine
import com.example.gymtracker.ui.components.OverflowMenu
import com.example.gymtracker.ui.components.TargetSheet
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.ConfirmSheet
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.SectionHead
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.screens.plans.components.TextEntrySheet
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val UNDO_MILLIS = 5_000L

/** The index column fits two digits at any font scale; a size, not a gap. */
private val IndexColumn = 28.dp

private enum class EditorSheet { Rename, Note, Delete }

/**
 * Plan editor — design-plan §4.2. Name (tap → rename), a meta line with the typical duration
 * (decision 4), the plan note (tap → edit), then the exercises as text on hairlines: index,
 * name, `Chest · Barbell`, the target in tertiary, and a drag handle that is the only drag zone
 * (issue 8). Row tap → the shared [TargetSheet] with View progress and Remove (undoable). ⋮ holds
 * Rename, Export and Delete plan; Start workout is pinned above the navigation inset.
 */
@Composable
fun PlanEditorScreen(
    planId: Long,
    onBack: () -> Unit,
    onLog: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onAddExercises: () -> Unit,
    pickedExerciseIds: List<Long>?,
    onPickedHandled: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: PlanEditorViewModel = viewModel(
        key = "plan-$planId",
        factory = AppViewModelFactory(
            mapOf(PlanEditorViewModel::class.java to {
                PlanEditorViewModel(container.repository, planId, container.database.sessionDao())
            })
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val typicalDuration by viewModel.typicalDurationMillis.collectAsStateWithLifecycle()
    LaunchedEffect(pickedExerciseIds) {
        if (pickedExerciseIds != null) {
            viewModel.addExercises(pickedExerciseIds)
            onPickedHandled()
        }
    }
    val plan = state?.plan
    // Remembered: the screen recomposes on every drag start/stop, sheet and edit; re-sort only
    // when Room emits a new plan.
    val planned = remember(state) { state?.planned?.sortedBy { it.planned.sortIndex }.orEmpty() }
    var sheet by rememberSaveable { mutableStateOf<EditorSheet?>(null) }
    var editing by remember { mutableStateOf<PlannedWithExercise?>(null) }
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val current = state
        if (uri != null && current != null) {
            scope.launch {
                val ok = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                            out.write(PlanExport.toJson(current).toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open output stream")
                    }
                }.isSuccess
                snackbarHostState.showSnackbar(if (ok) "Plan exported" else "Export failed")
            }
        }
    }

    fun remove(row: PlannedWithExercise) {
        haptics.light()
        viewModel.removePlanned(row.planned.id)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = withTimeoutOrNull(UNDO_MILLIS) {
                snackbarHostState.showSnackbar(
                    "Removed ${row.exercise.name}",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == SnackbarResult.ActionPerformed) viewModel.restorePlanned(row.planned)
        }
    }

    // Drag-and-drop reorder. localOrder is the display order; it mirrors the DB while idle and is
    // mutated live during a drag, then committed on drop.
    val listState = rememberLazyListState()
    var localOrder by remember { mutableStateOf<List<PlannedWithExercise>>(emptyList()) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(planned, draggingId) {
        if (draggingId != null) return@LaunchedEffect
        // Keep the local order while a dropped order is still being written row by row (so the
        // list doesn't jump through half-written states); take the DB's order whenever the set of
        // rows changes (add, remove, undo).
        val byId = planned.associateBy { it.planned.id }
        val sameRows = localOrder.size == planned.size && localOrder.all { it.planned.id in byId }
        localOrder = if (sameRows) localOrder.map { byId.getValue(it.planned.id) } else planned
    }

    val totalSets = planned.sumOf { it.planned.targetSets }

    Scaffold(
        topBar = {
            // Loading: no title, no menu, no placeholder name (§4.2).
            AppTopBar(
                title = "",
                onBack = onBack,
                actions = {
                    if (plan != null) {
                        OverflowMenu(
                            items = listOf(
                                MenuItem("Rename", { sheet = EditorSheet.Rename }),
                                MenuItem(
                                    "Export",
                                    { exportLauncher.launch(PlanExport.suggestedFileName(plan.name)) },
                                    enabled = planned.isNotEmpty(),
                                ),
                                MenuItem("Delete plan", { sheet = EditorSheet.Delete }, destructive = true),
                            ),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (plan != null) {
                StartBar(
                    enabled = planned.isNotEmpty(),
                    onStart = { haptics.light(); onLog(planId) },
                )
            }
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        if (plan == null) return@Scaffold
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = Spacing.l,
            ),
        ) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        plan.name,
                        style = AppText.titleL,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Rename plan") { sheet = EditorSheet.Rename },
                    )
                    Text(
                        PlanStats.metaLine(planned.size, typicalDuration, sets = totalSets),
                        style = AppText.bodyS,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NoteLine(note = plan.notes, onClick = { sheet = EditorSheet.Note })
                    Spacer(Modifier.height(Spacing.l))
                }
            }
            item(key = "exercises-head") {
                SectionHead(title = "Exercises") {
                    if (totalSets > 0) {
                        Text(
                            "$totalSets ${plural(totalSets, "set")}",
                            style = AppText.bodyS,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.s))
                AppDivider()
            }
            if (localOrder.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No exercises yet",
                        message = "Add the first one.",
                        actionLabel = "Add exercise",
                        onAction = onAddExercises,
                    )
                }
            } else {
                itemsIndexed(localOrder, key = { _, row -> row.planned.id }) { index, row ->
                    val dragging = row.planned.id == draggingId
                    Column(
                        Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .then(if (dragging) Modifier else Modifier.animateItem())
                            .graphicsLayer { translationY = if (dragging) dragDeltaY else 0f }
                            .background(
                                if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.background,
                            ),
                    ) {
                        PlannedRow(
                            index = index,
                            planned = row.planned,
                            exercise = row.exercise,
                            onClick = { editing = row },
                            handleModifier = Modifier.pointerInput(row.planned.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = row.planned.id
                                        dragDeltaY = 0f
                                        haptics.reorder()
                                    },
                                    onDragEnd = {
                                        viewModel.applyOrder(localOrder.map { it.planned.id })
                                        draggingId = null
                                        dragDeltaY = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragDeltaY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDeltaY += amount.y
                                        val id = draggingId ?: return@detectDragGestures
                                        val visible = listState.layoutInfo.visibleItemsInfo
                                        val curInfo = visible.firstOrNull { it.key == id }
                                            ?: return@detectDragGestures
                                        val center = curInfo.offset + curInfo.size / 2 + dragDeltaY
                                        // Only planned rows (Long keys) are drop targets, never
                                        // the header, section head or foot.
                                        val target = visible.firstOrNull { info ->
                                            info.key != id && info.key is Long &&
                                                center.toInt() in info.offset..(info.offset + info.size)
                                        } ?: return@detectDragGestures
                                        val from = localOrder.indexOfFirst { it.planned.id == id }
                                        val to = localOrder.indexOfFirst { it.planned.id == target.key }
                                        if (from < 0 || to < 0) return@detectDragGestures
                                        // Re-anchor the floating row so it stays under the finger
                                        // after the slot shift. Moving down, it lands one
                                        // neighbour lower, so its new top depends on that
                                        // neighbour's height; moving up it lands on the target.
                                        val newBase = if (to > from)
                                            target.offset + target.size - curInfo.size
                                        else
                                            target.offset
                                        dragDeltaY = (curInfo.offset + dragDeltaY) - newBase
                                        localOrder = localOrder.toMutableList().apply { add(to, removeAt(from)) }
                                        haptics.reorder()
                                    },
                                )
                            },
                        )
                        AppDivider()
                    }
                }
                item(key = "add") {
                    AppTextButton(text = "Add exercise", onClick = onAddExercises, icon = Icons.Outlined.Add)
                }
            }
        }
    }

    when (sheet) {
        EditorSheet.Rename -> if (plan != null) {
            TextEntrySheet(
                title = "Rename plan",
                initial = plan.name,
                placeholder = "Plan name",
                confirmLabel = "Save",
                onDismiss = { sheet = null },
                onConfirm = { name ->
                    viewModel.renamePlan(name)
                    sheet = null
                },
            )
        }
        EditorSheet.Note -> if (plan != null) {
            TextEntrySheet(
                title = "Plan note",
                caption = "Shown under the plan's name.",
                initial = plan.notes,
                placeholder = "Heavy day. 2 min rests on compounds.",
                confirmLabel = "Save",
                singleLine = false,
                allowBlank = true,
                onDismiss = { sheet = null },
                onConfirm = { note ->
                    viewModel.updateNotes(note.trim())
                    sheet = null
                },
            )
        }
        EditorSheet.Delete -> if (plan != null) {
            ConfirmSheet(
                title = "Delete ${plan.name}?",
                message = "The plan and its exercise list go. Logged sessions stay in History.",
                confirmLabel = "Delete plan",
                onConfirm = {
                    sheet = null
                    viewModel.deletePlan(onDeleted = onBack)
                },
                onDismiss = { sheet = null },
            )
        }
        null -> Unit
    }

    editing?.let { row ->
        TargetSheet(
            exerciseName = row.exercise.name,
            movementType = row.exercise.movementType,
            initialSets = row.planned.targetSets,
            initialLow = row.planned.targetRepsLow,
            initialHigh = row.planned.targetRepsHigh,
            onApply = { sets, low, high ->
                viewModel.updatePlanned(
                    row.planned.copy(
                        targetSets = sets ?: row.planned.targetSets,
                        targetRepsLow = low,
                        targetRepsHigh = high,
                    ),
                )
                editing = null
            },
            onDismiss = { editing = null },
            onViewProgress = {
                editing = null
                onOpenExercise(row.exercise.id)
            },
            removeLabel = "Remove from plan",
            onRemove = {
                editing = null
                remove(row)
            },
        )
    }
}

/**
 * §3 `PlannedRow`: index `numberM` · name `titleM` over `Chest · Barbell` · target in tertiary ·
 * drag handle. Only [handleModifier]'s 48dp target starts a drag; the rest of the row is a tap.
 */
@Composable
private fun PlannedRow(
    index: Int,
    planned: PlannedExerciseEntity,
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    handleModifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val caption = listOfNotNull(
        exercise.primaryMuscles.firstOrNull()?.displayName,
        exercise.equipment.displayName,
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            style = AppText.numberM,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = IndexColumn),
        )
        Spacer(Modifier.width(Spacing.s))
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = AppText.titleM, color = scheme.onSurface)
            Text(caption, style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Spacing.m))
        Text(
            formatTarget(planned.targetSets, planned.targetRepsLow, planned.targetRepsHigh, exercise.movementType),
            style = AppText.bodyS,
            color = scheme.tertiary,
        )
        Box(
            handleModifier
                .size(Sizes.touchTarget)
                .semantics { contentDescription = "Drag to reorder ${exercise.name}" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.DragHandle, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** Start workout, pinned above the navigation inset; disabled with a caption while empty. */
@Composable
private fun StartBar(enabled: Boolean, onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
    ) {
        AppDivider()
        Spacer(Modifier.height(Spacing.s))
        AppButton(
            text = "Start workout",
            onClick = onStart,
            enabled = enabled,
            icon = Icons.Outlined.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!enabled) {
            Text(
                "Add an exercise to start",
                style = AppText.bodyS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
