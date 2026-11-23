package com.example.gymtracker.ui.screens.picker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.ActiveFilterLine
import com.example.gymtracker.ui.components.FilterSheet
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.SectionHead
import com.example.gymtracker.ui.navigation.PickerMode
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** The selection mark's box (a size). */
private val MarkSize = 22.dp

/**
 * Exercise picker — §4.13 (S3-b), a page instead of a sheet so several exercises can be added
 * in one visit and its filter sheet isn't a sheet over a sheet.
 *
 * Search + filter button, active-filter line, RECENT (hidden while searching/filtering), ALL with
 * its count. Multi-select modes number picks in order once two or more are chosen and show a
 * sticky `Add n exercises`; [PickerMode.Single] (Import's Map) returns on the first tap.
 * [onPicked] receives ids in pick order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerScreen(
    mode: PickerMode,
    planId: Long?,
    onBack: () -> Unit,
    onPicked: (List<Long>) -> Unit,
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

    Scaffold(
        topBar = {
            AppTopBar(title = if (multi) "Add exercises" else "Pick an exercise", onBack = onBack)
        },
        bottomBar = {
            if (multi && selectedCount > 0) {
                // Sticky CTA; the page's only IME consumer, so it rides above the keyboard.
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.imeAnimationTarget.union(WindowInsets.navigationBars)),
                ) {
                    AppDivider()
                    AppButton(
                        text = "Add $selectedCount ${if (selectedCount == 1) "exercise" else "exercises"}",
                        onClick = { haptics.confirm(); onPicked(state?.selected.orEmpty()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.gutter, vertical = Spacing.m),
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchBar(
                query = state?.query.orEmpty(),
                onQueryChange = viewModel::setQuery,
                filterCount = state?.filters?.count ?: 0,
                onFilter = { showFilters = true },
                autofocus = mode == PickerMode.Session,
            )
            state?.filters?.takeIf { it.isActive }?.let { f ->
                ActiveFilterLine(f, onClear = { viewModel.setFilters(com.example.gymtracker.ui.components.ExerciseFilters()) })
            }
            val s = state ?: return@Column // Loading: list not drawn until the first emission.
            if (s.results.isEmpty()) {
                EmptyState(
                    title = "No exercises match",
                    message = "Try a shorter search or fewer filters.",
                    actionLabel = "Clear filters",
                    onAction = viewModel::clearSearch,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
                return@Column
            }
            val onRow: (ExerciseEntity) -> Unit = { ex ->
                haptics.light()
                if (multi) viewModel.toggle(ex.id) else onPicked(listOf(ex.id))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (!s.searching && s.recent.isNotEmpty()) {
                    section("recent", "Recent", trailing = null)
                    rows("recent", s.recent, s, multi, onRow)
                }
                section("all", if (s.searching) "Results" else "All", trailing = "${s.results.size}")
                rows("all", s.results, s, multi, onRow)
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
    item(key = "head-$key") {
        Column(Modifier.padding(horizontal = Spacing.gutter).padding(top = Spacing.section)) {
            SectionHead(
                title = title,
                trailing = trailing?.let { t ->
                    { Text(t, style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
            )
            Spacer(Modifier.height(Spacing.s))
            AppDivider()
        }
    }
}

private fun LazyListScope.rows(
    keyPrefix: String,
    exercises: List<ExerciseEntity>,
    s: PickerUiState,
    multi: Boolean,
    onRow: (ExerciseEntity) -> Unit,
) {
    items(exercises, key = { "$keyPrefix-${it.id}" }) { ex ->
        val order = s.selected.indexOf(ex.id)
        PickerRow(
            exercise = ex,
            tag = if (ex.id in s.inPlan) "In plan" else ex.movementType.displayName,
            custom = ex.isCustom,
            multi = multi,
            selectedOrder = order.takeIf { it >= 0 }?.plus(1),
            showNumbers = s.selected.size >= 2,
            onClick = { onRow(ex) },
        )
    }
}

/** Search field on a hairline, with the filter button (and its active count) trailing. */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterCount: Int,
    onFilter: () -> Unit,
    autofocus: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    if (autofocus && !focusedOnce) {
        LaunchedEffect(Unit) {
            runCatching { focus.requestFocus() }
            focusedOnce = true
        }
    }
    Column(Modifier.padding(horizontal = Spacing.gutter)) {
        Row(Modifier.heightIn(min = Sizes.touchTarget), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = scheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.m))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = AppText.body.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f).focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Search", style = AppText.body, color = scheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = scheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onFilter) {
                BadgedBox(badge = { if (filterCount > 0) AppBadge("$filterCount") }) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = if (filterCount > 0) "Filters, $filterCount active" else "Filters",
                        tint = if (filterCount > 0) scheme.onSurface else scheme.onSurfaceVariant,
                    )
                }
            }
        }
        AppDivider()
    }
}

/**
 * Picker row: name `titleM` over `Barbell · Chest, Triceps` in `bodyS`; the trailing column holds
 * the type eyebrow (`IN PLAN` when already planned; `CUSTOM` in tertiary for custom exercises)
 * over the 48dp selection mark — empty box, ✓ in primary, or the pick number when ≥2 are chosen.
 */
@Composable
private fun PickerRow(
    exercise: ExerciseEntity,
    tag: String,
    custom: Boolean,
    multi: Boolean,
    selectedOrder: Int?,
    showNumbers: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selected = selectedOrder != null
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (multi) this.selected = selected }
                .clickable(role = if (multi) Role.Checkbox else Role.Button, onClick = onClick)
                .heightIn(min = 64.dp)
                .padding(start = Spacing.gutter, end = Spacing.xs, top = Spacing.s, bottom = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow(
                    if (custom && tag == exercise.movementType.displayName) "Custom" else tag,
                    color = if (custom) scheme.tertiary else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = Spacing.m),
                )
                if (multi) {
                    Box(Modifier.size(Sizes.touchTarget), contentAlignment = Alignment.Center) {
                        SelectionMark(selectedOrder, showNumbers)
                    }
                }
            }
        }
        AppDivider(Modifier.padding(horizontal = Spacing.gutter))
    }
}

@Composable
private fun SelectionMark(order: Int?, showNumber: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sm)
    if (order == null) {
        Box(Modifier.size(MarkSize).border(BorderStroke(Elevation.hairline * 2, scheme.outline), shape))
    } else {
        Box(
            Modifier.size(MarkSize).clip(shape).background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (showNumber) {
                Text("$order", style = AppText.label, color = scheme.onPrimary)
            } else {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
