package com.example.gymtracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    repository: GymRepository,
    onPick: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<MovementType?>(null) }
    var equipmentFilter by remember { mutableStateOf<Equipment?>(null) }
    var muscleFilter by remember { mutableStateOf<Muscle?>(null) }

    val results by remember(query) { repository.searchExercises(query.trim()) }
        .collectAsState(initial = emptyList())

    val filtered = remember(results, typeFilter, equipmentFilter, muscleFilter) {
        results.filter { ex ->
            (typeFilter == null || ex.movementType == typeFilter) &&
                (equipmentFilter == null || ex.equipment == equipmentFilter) &&
                (muscleFilter == null ||
                    ex.primaryMuscles.contains(muscleFilter) ||
                    ex.secondaryMuscles.contains(muscleFilter))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Pick an exercise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            FilterRow(
                label = "Type",
                options = MovementType.values().toList(),
                selected = typeFilter,
                onSelect = { typeFilter = it },
                labelFor = { it.name },
            )
            FilterRow(
                label = "Equipment",
                options = Equipment.values().toList(),
                selected = equipmentFilter,
                onSelect = { equipmentFilter = it },
                labelFor = { it.name },
            )
            FilterRow(
                label = "Muscle",
                options = Muscle.values().toList(),
                selected = muscleFilter,
                onSelect = { muscleFilter = it },
                labelFor = { it.name },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filtered, key = { it.id }) { ex ->
                    ExerciseListRow(ex, onClick = { onPick(ex) })
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
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp),
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

