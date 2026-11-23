package com.example.gymtracker.ui.screens.library

import com.example.gymtracker.data.displayName
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.components.ExerciseListRow
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.Radius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Query text is bound to the synchronous filters flow, not `state` (which lags a frame behind
    // the async searchExercises DB query). Binding the field to the async value resets the cursor
    // to the stale position on every keystroke.
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val haptics = rememberAppHaptics()

    LaunchedEffect(state.filters) {
        if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Exercises", onBack = onBack)
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(Radius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l),
            )

            FilterRow(
                label = "Type",
                options = MovementType.values().toList(),
                selected = filters.movementType,
                onSelect = { v -> viewModel.setMovementType(v); haptics.light() },
                labelFor = { it.displayName },
            )
            FilterRow(
                label = "Equipment",
                options = Equipment.values().toList(),
                selected = filters.equipment,
                onSelect = { v -> viewModel.setEquipment(v); haptics.light() },
                labelFor = { it.displayName },
            )
            FilterRow(
                label = "Muscle",
                options = Muscle.values().toList(),
                selected = filters.muscle,
                onSelect = { v -> viewModel.setMuscle(v); haptics.light() },
                labelFor = { it.displayName },
            )

            if (state.exercises.isEmpty()) {
                EmptyState(
                    title = "No matches",
                    message = "No exercises match this search and filter.",
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = Spacing.l,
                        end = Spacing.l,
                        top = Spacing.xs,
                        bottom = Spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    items(state.exercises, key = { it.id }) { ex ->
                        ExerciseListRow(ex, onClick = { onExerciseClick(ex.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Any> FilterRow(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    labelFor: (T) -> String,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = Spacing.s),
        )
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
        )
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(if (selected == opt) null else opt) },
                label = { Text(labelFor(opt)) },
            )
        }
    }
}
