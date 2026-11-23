package com.example.gymtracker.ui.screens.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gymtracker.data.db.PlannedWithExercise
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.formatTarget
import com.example.gymtracker.data.plan.PlanExport
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppFab
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.Stepper
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.components.ExercisePickerSheet
import com.example.gymtracker.ui.rememberAppHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gymtracker.ui.theme.Radius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    planId: Long,
    onBack: () -> Unit,
    onLog: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: PlanEditorViewModel = viewModel(
        key = "plan-$planId",
        factory = AppViewModelFactory(
            mapOf(PlanEditorViewModel::class.java to {
                PlanEditorViewModel(container.repository, planId)
            })
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state?.plan
    val planned = state?.planned?.sortedBy { it.planned.sortIndex }.orEmpty()
    var showPicker by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var editingPlanned by remember { mutableStateOf<PlannedExerciseEntity?>(null) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
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

    // Drag-and-drop reorder state. localOrder is the display order; it mirrors the DB while idle
    // and is mutated live during a drag, then committed on drop.
    val listState = rememberLazyListState()
    var localOrder by remember { mutableStateOf<List<PlannedWithExercise>>(emptyList()) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(planned, draggingId) {
        if (draggingId != null) return@LaunchedEffect
        if (localOrder.isEmpty()) {
            localOrder = planned
            return@LaunchedEffect
        }
        // Preserve current order, refresh row content, append new rows, drop removed ones.
        val byId = planned.associateBy { it.planned.id }
        val merged = localOrder.mapNotNull { byId[it.planned.id] }.toMutableList()
        val existing = merged.map { it.planned.id }.toSet()
        planned.forEach { if (it.planned.id !in existing) merged.add(it) }
        if (merged != localOrder) localOrder = merged
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = plan?.name ?: "Plan",
                onBack = onBack,
                actions = {
                    if (plan != null) {
                        IconButton(onClick = { showRename = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Rename")
                        }
                    }
                    if (plan != null && planned.isNotEmpty()) {
                        IconButton(onClick = {
                            exportLauncher.launch(PlanExport.suggestedFileName(plan.name))
                        }) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = "Export plan")
                        }
                        FilledIconButton(
                            onClick = { onLog(planId) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.padding(end = Spacing.s),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = "Log session")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            AppFab(text = "Add exercise", onClick = { showPicker = true }, icon = Icons.Outlined.Add)
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        if (planned.isEmpty()) {
            EmptyState(
                title = "No exercises yet",
                message = "Tap Add exercise to start building this plan.",
                modifier = Modifier.padding(padding).padding(horizontal = Spacing.gutter),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Sizes.fabClearance),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                itemsIndexed(localOrder, key = { _, row -> row.planned.id }) { index, row ->
                    val dragging = row.planned.id == draggingId
                    PlannedCard(
                        modifier = Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .then(if (dragging) Modifier else Modifier.animateItem())
                            .graphicsLayer { translationY = if (dragging) dragDeltaY else 0f }
                            .pointerInput(row.planned.id) {
                                detectDragGesturesAfterLongPress(
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
                                        val curIndex = localOrder.indexOfFirst { it.planned.id == draggingId }
                                        if (curIndex < 0) return@detectDragGesturesAfterLongPress
                                        val curInfo = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == curIndex }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val center = curInfo.offset + curInfo.size / 2 + dragDeltaY
                                        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                            info.index != curIndex &&
                                                center.toInt() in info.offset..(info.offset + info.size)
                                        }
                                        if (target != null) {
                                            val newList = localOrder.toMutableList()
                                                .apply { add(target.index, removeAt(curIndex)) }
                                            // Re-anchor the floating card so it stays under the finger after the
                                            // slot shift. Moving DOWN, the card lands one neighbour lower, so its
                                            // new top depends on THAT neighbour's height — using the dragged
                                            // card's own height (the old formula) made a two-line card jump and
                                            // oscillate, so the reorder looked stuck. Moving UP it lands exactly
                                            // on the target's slot.
                                            val newBase = if (target.index > curIndex)
                                                target.offset + target.size - curInfo.size
                                            else
                                                target.offset
                                            dragDeltaY = (curInfo.offset + dragDeltaY) - newBase
                                            localOrder = newList
                                            haptics.reorder()
                                        }
                                    },
                                )
                            },
                        index = index,
                        dragging = dragging,
                        planned = row.planned,
                        exercise = row.exercise,
                        onRemove = { haptics.light(); viewModel.removePlanned(row.planned.id) },
                        onEdit = {
                            editingPlanned = row.planned
                            editingExercise = row.exercise
                        },
                        onOpenExercise = onOpenExercise,
                    )
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            repository = container.repository,
            onPick = { ex ->
                viewModel.addExercise(ex.id)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    if (showRename && plan != null) {
        RenameSheet(
            current = plan.name,
            onDismiss = { showRename = false },
            onConfirm = { new ->
                viewModel.renamePlan(new)
                showRename = false
            },
        )
    }

    editingPlanned?.let { ep ->
        TargetsSheet(
            exerciseName = editingExercise?.name.orEmpty(),
            movementType = editingExercise?.movementType ?: MovementType.Compound,
            initial = ep,
            onDismiss = { editingPlanned = null; editingExercise = null },
            onApply = { updated ->
                viewModel.updatePlanned(updated)
                editingPlanned = null
                editingExercise = null
            },
        )
    }
}

@Composable
private fun PlannedCard(
    index: Int,
    dragging: Boolean,
    planned: PlannedExerciseEntity,
    exercise: ExerciseEntity,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none, pressedElevation = Elevation.none),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.m, end = Spacing.s, top = Spacing.m, bottom = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (index + 1).toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    formatTarget(
                        sets = planned.targetSets,
                        low = planned.targetRepsLow,
                        high = planned.targetRepsHigh,
                        type = exercise.movementType,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (exercise.primaryMuscles.isNotEmpty()) {
                    Text(
                        exercise.primaryMuscles.joinToString { it.displayName },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "${exercise.movementType.displayName} · ${exercise.equipment.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onOpenExercise(exercise.id) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.TrendingUp,
                    contentDescription = "View progress",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameSheet(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var text by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text("Rename plan", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Plan name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.End),
            ) {
                AppTextButton(text = "Cancel", onClick = onDismiss)
                AppButton(text = "Save", onClick = { onConfirm(text) }, enabled = text.isNotBlank())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetsSheet(
    exerciseName: String,
    movementType: MovementType,
    initial: PlannedExerciseEntity,
    onDismiss: () -> Unit,
    onApply: (PlannedExerciseEntity) -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var sets by remember(initial.id) { mutableStateOf(initial.targetSets) }
    var low by remember(initial.id) { mutableStateOf(initial.targetRepsLow) }
    var high by remember(initial.id) { mutableStateOf(initial.targetRepsHigh) }

    LaunchedEffect(low) { if (high < low) high = low }
    LaunchedEffect(high) { if (low > high) low = high }

    val unit = movementType.repUnit
    val (minMax, perSetCap) = when (movementType) {
        MovementType.Cardio -> 1..120 to 120
        MovementType.Mobility -> 1..180 to 180
        else -> 1..50 to 50
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.xxl, end = Spacing.xxl, top = Spacing.xs, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Targets", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(exerciseName, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
            }
            StepperRow(
                label = if (movementType == MovementType.Cardio) "Rounds" else "Sets",
                value = sets,
                onChange = { sets = it.coerceIn(1, 10) },
                min = 1, max = 10,
            )
            StepperRow(
                label = "Min $unit",
                value = low,
                onChange = { low = it.coerceIn(minMax.first, perSetCap) },
                min = minMax.first, max = perSetCap,
            )
            StepperRow(
                label = "Max $unit",
                value = high,
                onChange = { high = it.coerceIn(low, perSetCap) },
                min = low, max = perSetCap,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.End),
            ) {
                AppTextButton(text = "Cancel", onClick = onDismiss)
                AppButton(
                    text = "Apply",
                    onClick = {
                        onApply(initial.copy(
                            targetSets = sets,
                            targetRepsLow = low,
                            targetRepsHigh = high,
                        ))
                    },
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Stepper(
            value = value,
            onValueChange = onChange,
            range = min..max.coerceAtLeast(min),
            decrementLabel = "Decrease $label",
            incrementLabel = "Increase $label",
        )
    }
}
