package com.example.gymtracker.ui.screens.picker

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.SearchX
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.ActiveFilterLine
import com.example.gymtracker.ui.components.ExerciseFilters
import com.example.gymtracker.ui.components.ExerciseIcon
import com.example.gymtracker.ui.components.ExerciseSearchBar
import com.example.gymtracker.ui.components.FilterSheet
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.SectionHead
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.components.cardRow
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.navigation.PickerMode
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/** The selection mark's disc (a size) — Foodprint's OptionRow check. */
private val MarkSize = 24.dp

/** Dividers start after the icon disc, aligned with the text. */
private val RowDividerInset = Spacing.l + 40.dp + Spacing.m

/**
 * Exercise picker — a page instead of a sheet so several exercises can be added in one visit and
 * its filter sheet isn't a sheet over a sheet.
 *
 * Pill search + filter button, active-filter line, Recent (hidden while searching/filtering), All
 * with its count — each a white card of rows. Multi-select modes number picks in order once two or
 * more are chosen and show a sticky ink `Add n exercises`; [PickerMode.Single] (Import's Map)
 * returns on the first tap. [onPicked] receives ids in pick order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerScreen(
    mode: PickerMode,
    planId: Long?,
    onBack: () -> Unit,
    onPicked: (List<Long>) -> Unit,
    onNewExercise: (prefillName: String) -> Unit,
    createdExerciseId: Long?,
    onCreatedHandled: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ExercisePickerViewModel = viewModel(
        factory = AppViewModelFactory(
            mapOf(ExercisePickerViewModel::class.java to { ExercisePickerViewModel(container.repository, planId) })
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    var showFilters by remember { mutableStateOf(false) }
    val multi = mode != PickerMode.Single
    val selectedCount = state?.selected?.size ?: 0

    // The logger's picker focuses the search once on arrival.
    val focus = remember { FocusRequester() }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    if (mode == PickerMode.Session && !focusedOnce) {
        LaunchedEffect(Unit) {
            runCatching { focus.requestFocus() }
            focusedOnce = true
        }
    }

    // An exercise made from the empty search comes back here: Single mode returns it at once;
    // multi-select picks it and clears the search so it shows in the list.
    LaunchedEffect(createdExerciseId) {
        val id = createdExerciseId ?: return@LaunchedEffect
        onCreatedHandled()
        if (multi) viewModel.onCreated(id) else onPicked(listOf(id))
    }

    Scaffold(
        topBar = {
            AppTopBar(title = if (multi) "Add exercises" else "Pick an exercise", onBack = onBack)
        },
        bottomBar = {
            if (multi && selectedCount > 0) {
                // Sticky CTA; the page's only IME consumer, so it rides above the keyboard.
                AppButton(
                    text = "Add $selectedCount ${plural(selectedCount, "exercise")}",
                    onClick = { haptics.confirm(); onPicked(state?.selected.orEmpty()) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.imeAnimationTarget.union(WindowInsets.navigationBars))
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.m),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ExerciseSearchBar(
                query = state?.query.orEmpty(),
                onQueryChange = viewModel::setQuery,
                filterCount = state?.filters?.count ?: 0,
                onFilter = { showFilters = true },
                placeholder = "Search",
                focusRequester = focus,
                modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
            )
            state?.filters?.takeIf { it.isActive }?.let { f ->
                ActiveFilterLine(f, onClear = { viewModel.setFilters(ExerciseFilters()) })
            }
            val s = state ?: return@Column // Loading: list not drawn until the first emission.
            if (s.results.isEmpty()) {
                EmptyState(
                    title = "No exercises match",
                    message = "Try a shorter search or fewer filters.",
                    icon = Lucide.SearchX,
                    actionLabel = "Clear filters",
                    onAction = viewModel::clearSearch,
                )
                // Make the exercise that isn't there, named as searched.
                AppOutlinedButton(
                    text = "New exercise",
                    icon = Lucide.Plus,
                    onClick = { onNewExercise(s.query.trim()) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter),
                )
                return@Column
            }
            val onRow: (ExerciseEntity) -> Unit = { ex ->
                haptics.light()
                if (multi) viewModel.toggle(ex.id) else onPicked(listOf(ex.id))
            }
            val fill = cardColor()
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxl),
            ) {
                if (!s.searching && s.recent.isNotEmpty()) {
                    section("recent", "Recent", trailing = null)
                    rows("recent", s.recent, s, multi, fill, onRow)
                }
                section("all", if (s.searching) "Results" else "All", trailing = "${s.results.size}")
                rows("all", s.results, s, multi, fill, onRow)
            }
        }
    }

    if (showFilters) {
        val s = state
        if (s != null) {
            FilterSheet(filters = s.filters, onChange = viewModel::setFilters, onDismiss = { showFilters = false })
        }
    }
}

private fun LazyListScope.section(key: String, title: String, trailing: String?) {
    item(key = "head-$key", contentType = "head") {
        SectionHead(
            title = title,
            trailing = trailing?.let { t ->
                { Text(t, style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            modifier = Modifier.padding(top = Spacing.l, bottom = Spacing.s),
        )
    }
}

private fun LazyListScope.rows(
    keyPrefix: String,
    exercises: List<ExerciseEntity>,
    s: PickerUiState,
    multi: Boolean,
    fill: Color,
    onRow: (ExerciseEntity) -> Unit,
) {
    itemsIndexed(exercises, key = { _, ex -> "$keyPrefix-${ex.id}" }, contentType = { _, _ -> "row" }) { i, ex ->
        val order = s.selected.indexOf(ex.id)
        Column(Modifier.cardRow(i, exercises.size, fill)) {
            if (i > 0) AppDivider(Modifier.padding(start = RowDividerInset))
            PickerRow(
                exercise = ex,
                inPlan = ex.id in s.inPlan,
                multi = multi,
                selectedOrder = order.takeIf { it >= 0 }?.plus(1),
                showNumbers = s.selected.size >= 2,
                onClick = { onRow(ex) },
            )
        }
    }
}

/**
 * Picker row: the exercise's pastel disc, name `titleM` over `Barbell · Chest, Triceps` in
 * `bodyS` (with `In plan` / `Custom` badges), and the selection disc — an empty ring, an ink ✓,
 * or the pick number when two or more are chosen.
 */
@Composable
private fun PickerRow(
    exercise: ExerciseEntity,
    inPlan: Boolean,
    multi: Boolean,
    selectedOrder: Int?,
    showNumbers: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selected = selectedOrder != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = if (multi) Role.Checkbox else Role.Button
                if (multi) this.selected = selected
            }
            .pressable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseIcon(exercise)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(
                exercise.name,
                style = AppText.titleM,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val muscles = exercise.primaryMuscles.joinToString(", ") { it.displayName }
            Text(
                listOf(exercise.equipment.displayName, muscles).filter { it.isNotBlank() }.joinToString(" · "),
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (inPlan || exercise.isCustom) {
                Row(Modifier.padding(top = Spacing.xs)) {
                    if (inPlan) {
                        AppBadge("In plan", container = scheme.secondaryContainer, content = scheme.onSecondaryContainer)
                        Spacer(Modifier.width(Spacing.xs))
                    }
                    if (exercise.isCustom) {
                        AppBadge("Custom", container = scheme.tertiaryContainer, content = scheme.onTertiaryContainer)
                    }
                }
            }
        }
        if (multi) {
            Spacer(Modifier.width(Spacing.m))
            SelectionMark(selectedOrder, showNumbers)
        }
    }
}

@Composable
private fun SelectionMark(order: Int?, showNumber: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (order != null) scheme.primary else Color.Transparent, label = "markFill")
    Box(
        Modifier
            .size(MarkSize)
            .clip(CircleShape)
            .background(fill)
            .then(if (order == null) Modifier.border(1.5.dp, scheme.outline, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            order == null -> Unit
            showNumber -> Text("$order", style = AppText.label, color = scheme.onPrimary)
            else -> Icon(Lucide.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(14.dp))
        }
    }
}
