package com.example.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * The three exercise filters (§3 `FilterSheet` + `ActiveFilterLine`). Multi-select within a
 * section (any of the chosen types, any of the chosen equipment, …); empty = any.
 */
@Immutable
data class ExerciseFilters(
    val types: Set<MovementType> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
    val muscles: Set<Muscle> = emptySet(),
) {
    /** Options chosen across all three sections — the count on the filter button. */
    val count: Int get() = types.size + equipment.size + muscles.size
    val isActive: Boolean get() = count > 0

    fun matches(ex: ExerciseEntity): Boolean =
        (types.isEmpty() || ex.movementType in types) &&
            (equipment.isEmpty() || ex.equipment in equipment) &&
            (muscles.isEmpty() || muscles.any { it in ex.primaryMuscles || it in ex.secondaryMuscles })

    /** `Compound · Barbell, Dumbbell · Chest` — the active-filter line, in sheet order. */
    val summary: String
        get() = listOf(
            MovementType.entries.filter { it in types }.joinToString(", ") { it.displayName },
            Equipment.entries.filter { it in equipment }.joinToString(", ") { it.displayName },
            Muscle.entries.filter { it in muscles }.joinToString(", ") { it.displayName },
        ).filter { it.isNotEmpty() }.joinToString(" · ")
}

/**
 * Filter sheet (§4.10, S3-b) — three [Section]s, TYPE, EQUIPMENT, MUSCLE, each a wrapped row of
 * text options (selected = `onSurface` + 2dp rule; multi-select within a section). `Clear` and
 * `Show N` sit in a pinned foot; the sections scroll above it. Applies live. Only ever opened
 * from a page (house rule 12). [resultCount] labels the foot button; without it, `Done`.
 */
@Composable
fun FilterSheet(
    filters: ExerciseFilters,
    onChange: (ExerciseFilters) -> Unit,
    onDismiss: () -> Unit,
    resultCount: Int? = null,
) {
    AppSheet(onDismiss = onDismiss) {
        Text("Filter", style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.l))
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            FilterSection("Type", MovementType.entries, filters.types, { it.displayName }) {
                onChange(filters.copy(types = it))
            }
            FilterSection("Equipment", Equipment.entries, filters.equipment, { it.displayName }) {
                onChange(filters.copy(equipment = it))
            }
            FilterSection("Muscle", Muscle.entries, filters.muscles, { it.displayName }) {
                onChange(filters.copy(muscles = it))
            }
        }
        AppDivider()
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTextButton(
                text = "Clear",
                onClick = { onChange(ExerciseFilters()) },
                enabled = filters.isActive,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            AppButton(text = resultCount?.let { "Show $it" } ?: "Done", onClick = onDismiss)
        }
    }
}

@Composable
private fun <T : Any> FilterSection(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onChange: (Set<T>) -> Unit,
) {
    Section(title = title, trailing = {
        if (selected.isNotEmpty()) {
            Text("${selected.size}", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            options.forEach { option ->
                FilterOption(label(option), option in selected) { on ->
                    onChange(if (on) selected + option else selected - option)
                }
            }
        }
    }
    Spacer(Modifier.height(Spacing.l))
}

/** One text option: label over a 24×2dp rule, `onSurface` when on (house rule 3, no chip). */
@Composable
private fun FilterOption(text: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    Box(
        Modifier
            .heightIn(min = Sizes.touchTarget)
            .toggleable(value = on, role = Role.Checkbox) { haptics.light(); onToggle(it) }
            .padding(vertical = Spacing.s),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = AppText.label, color = if (on) scheme.onSurface else scheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xs))
            Box(
                Modifier
                    .size(width = 24.dp, height = 2.dp)
                    .background(if (on) scheme.onSurface else Color.Transparent),
            )
        }
    }
}

/** One `bodyS` line under the search when filters are on: the summary + `Clear`. */
@Composable
fun ActiveFilterLine(filters: ExerciseFilters, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = Spacing.gutter), verticalAlignment = Alignment.CenterVertically) {
        Text(
            filters.summary,
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppTextButton(text = "Clear", onClick = onClear, color = MaterialTheme.colorScheme.onSurface)
    }
}
