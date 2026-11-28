package com.example.gymtracker.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.SearchX
import com.example.gymtracker.ui.components.ActiveFilterLine
import com.example.gymtracker.ui.components.ExerciseFilters
import com.example.gymtracker.ui.components.ExerciseListRow
import com.example.gymtracker.ui.components.ExerciseSearchBar
import com.example.gymtracker.ui.components.FilterSheet
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppFab
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.cardRow
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Exercises — the library. Top bar with the count; the pill search with the filter button (and
 * its active count); the active filters as one line under it; then the exercises as one white
 * card of rows; a `New exercise` FAB opens the custom-exercise form. Loading draws the top bar and
 * search only.
 */
/** Dividers start after the icon disc, aligned with the text. */
private val RowDividerInset = Spacing.l + 40.dp + Spacing.m

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onNewExercise: () -> Unit,
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
                subtitle = state?.let { "${it.exercises.size} shown" },
                onBack = onBack,
            )
        },
        floatingActionButton = { AppFab(text = "New exercise", icon = Lucide.Plus, onClick = onNewExercise) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            ExerciseSearchBar(
                query = query,
                onQueryChange = viewModel::setQuery,
                filterCount = state?.filters?.count ?: 0,
                onFilter = { showFilters = true },
                modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
            )
            val s = state ?: return@Column
            if (s.filters.isActive) {
                ActiveFilterLine(s.filters, onClear = { viewModel.setFilters(ExerciseFilters()) })
            }
            if (s.exercises.isEmpty()) {
                EmptyState(
                    title = "No exercises match",
                    message = "Try a shorter search or fewer filters.",
                    icon = Lucide.SearchX,
                    actionLabel = "Clear filters",
                    onAction = viewModel::clearFilters,
                )
                return@Column
            }
            val fill = cardColor()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.gutter,
                    end = Spacing.gutter,
                    top = Spacing.s,
                    bottom = Sizes.fabClearance,
                ),
            ) {
                itemsIndexed(s.exercises, key = { _, ex -> ex.id }) { i, ex ->
                    Column(Modifier.cardRow(i, s.exercises.size, fill)) {
                        if (i > 0) AppDivider(Modifier.padding(start = RowDividerInset))
                        ExerciseListRow(ex, onClick = { onExerciseClick(ex.id) })
                    }
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
