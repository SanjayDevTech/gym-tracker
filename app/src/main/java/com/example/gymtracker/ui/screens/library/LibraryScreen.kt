package com.example.gymtracker.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.ui.components.ActiveFilterLine
import com.example.gymtracker.ui.components.ExerciseFilters
import com.example.gymtracker.ui.components.ExerciseListRow
import com.example.gymtracker.ui.components.FilterSheet
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Exercises (§4.10) — a leaf of Progress. Top bar with the count; one search line with the filter
 * button (and its active count); the active filters as one line under it; then text rows on
 * hairlines. Loading draws the top bar and search only (house rule 8).
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state?.filters, query) {
        if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Exercises",
                onBack = onBack,
                actions = {
                    state?.let { s ->
                        Text(
                            "${s.exercises.size}",
                            style = AppText.bodyS,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = Spacing.gutter),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            SearchLine(
                query = query,
                onQueryChange = viewModel::setQuery,
                filterCount = state?.filters?.count ?: 0,
                onFilter = { showFilters = true },
            )
            val s = state ?: return@Column
            if (s.filters.isActive) {
                ActiveFilterLine(s.filters, onClear = { viewModel.setFilters(ExerciseFilters()) })
            }
            if (s.exercises.isEmpty()) {
                EmptyState(
                    title = "No exercises match",
                    message = "Try a shorter search or fewer filters.",
                    actionLabel = "Clear filters",
                    onAction = viewModel::clearFilters,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
                return@Column
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(s.exercises, key = { it.id }) { ex ->
                    ExerciseListRow(ex, onClick = { onExerciseClick(ex.id) })
                    AppDivider(Modifier.padding(horizontal = Spacing.gutter))
                }
            }
        }
    }

    if (showFilters) {
        state?.let { s ->
            FilterSheet(
                filters = s.filters,
                onChange = viewModel::setFilters,
                onDismiss = { showFilters = false },
                resultCount = s.exercises.size,
            )
        }
    }
}

/** Search field on a hairline, with the filter button (and its active count) trailing (S3-b). */
@Composable
private fun SearchLine(
    query: String,
    onQueryChange: (String) -> Unit,
    filterCount: Int,
    onFilter: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
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
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Search exercises", style = AppText.body, color = scheme.onSurfaceVariant)
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
