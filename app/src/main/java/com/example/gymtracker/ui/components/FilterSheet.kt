package com.example.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import com.composables.icons.lucide.ListFilter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.AppHaptics
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTextField
import com.example.gymtracker.ui.components.atoms.RoundIconButton
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
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
 * Filter sheet — three cards, Type, Equipment, Muscle, each a wrapped row of pill chips (ink
 * when on; multi-select within a card). `Clear` and `Show N` sit in a pinned foot; the cards
 * scroll above it. Applies live. [resultCount] labels the foot button; without it, `Done`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filters: ExerciseFilters,
    onChange: (ExerciseFilters) -> Unit,
    onDismiss: () -> Unit,
    resultCount: Int? = null,
) {
    // One haptics handle for the whole sheet: each rememberAppHaptics() starts its own DataStore
    // collector, and the sheet has an option per movement type, equipment and muscle.
    val haptics = rememberAppHaptics()
    AppSheet(onDismiss = onDismiss) {
        SheetTitleRow("Filter", onClose = onDismiss)
        Spacer(Modifier.height(Spacing.m))
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            FilterSection("Type", MovementType.entries, filters.types, { it.displayName }, haptics) {
                onChange(filters.copy(types = it))
            }
            FilterSection("Equipment", Equipment.entries, filters.equipment, { it.displayName }, haptics) {
                onChange(filters.copy(equipment = it))
            }
            FilterSection("Muscle", Muscle.entries, filters.muscles, { it.displayName }, haptics) {
                onChange(filters.copy(muscles = it))
            }
        }
        Spacer(Modifier.height(Spacing.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            AppOutlinedButton(
                text = "Clear",
                onClick = { onChange(ExerciseFilters()) },
                enabled = filters.isActive,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = resultCount?.let { "Show $it" } ?: "Done",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Any> FilterSection(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    haptics: AppHaptics,
    onChange: (Set<T>) -> Unit,
) {
    Section(title = title, trailing = {
        if (selected.isNotEmpty()) {
            Text("${selected.size} on", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            options.forEach { option ->
                FilterChip(label(option), option in selected, haptics) { on ->
                    onChange(if (on) selected + option else selected - option)
                }
            }
        }
    }
}

/** A soft pill that turns ink when on. */
@Composable
fun FilterChip(text: String, on: Boolean, haptics: AppHaptics, onToggle: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (on) scheme.primary else softFill(), label = "chipFill")
    val ink by animateColorAsState(if (on) scheme.onPrimary else scheme.onSurface, label = "chipInk")
    Box(
        Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(fill)
            .toggleable(value = on, role = Role.Checkbox) { haptics.light(); onToggle(it) }
            .heightIn(min = 40.dp)
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.label, color = ink)
    }
}

/**
 * The pill search field with the filter button beside it (an ink count badge when filters are
 * on) — the head of the exercise library and the picker. [focusRequester] lets the picker focus
 * it on arrival.
 */
@Composable
fun ExerciseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterCount: Int,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search exercises",
    focusRequester: FocusRequester? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder, style = AppText.body) },
            leadingIcon = { Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Lucide.X, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(Radius.pill),
            focusRequester = focusRequester,
        )
        Spacer(Modifier.size(Spacing.xs))
        BadgedBox(badge = { if (filterCount > 0) AppBadge("$filterCount") }) {
            RoundIconButton(
                icon = Lucide.ListFilter,
                contentDescription = if (filterCount > 0) "Filters, $filterCount active" else "Filters",
                onClick = onFilter,
            )
        }
    }
}

/** One `bodyS` line under the search when filters are on: the summary + `Clear`. */
@Composable
fun ActiveFilterLine(filters: ExerciseFilters, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = Spacing.gutter + Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(
            filters.summary,
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppTextButton(text = "Clear", onClick = onClear)
    }
}
