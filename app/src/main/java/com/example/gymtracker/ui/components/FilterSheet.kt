package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/** The three exercise filters (§3 `FilterSheet` + `ActiveFilterLine`). Null = any. */
@Immutable
data class ExerciseFilters(
    val type: MovementType? = null,
    val equipment: Equipment? = null,
    val muscle: Muscle? = null,
) {
    val count: Int get() = listOfNotNull(type, equipment, muscle).size
    val isActive: Boolean get() = count > 0

    fun matches(ex: ExerciseEntity): Boolean =
        (type == null || ex.movementType == type) &&
            (equipment == null || ex.equipment == equipment) &&
            (muscle == null || muscle in ex.primaryMuscles || muscle in ex.secondaryMuscles)

    /** `Compound · Barbell · Chest` — the active-filter line. */
    val summary: String
        get() = listOfNotNull(type?.displayName, equipment?.displayName, muscle?.displayName).joinToString(" · ")
}

/**
 * Filter sheet — three text-option sections (Type, Equipment, Muscle), each an
 * [UnderlineSelector] with `All`. Replaces the three scrolling chip rows. Only ever opened from a
 * page (house rule 12). Applies live; `Clear` resets all three.
 */
@Composable
fun FilterSheet(
    filters: ExerciseFilters,
    onChange: (ExerciseFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismiss = onDismiss) {
        Text("Filter", style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.l))
        FilterSection("Type", MovementType.entries, filters.type, { it.displayName }) { onChange(filters.copy(type = it)) }
        FilterSection("Equipment", Equipment.entries, filters.equipment, { it.displayName }) {
            onChange(filters.copy(equipment = it))
        }
        FilterSection("Muscle", Muscle.entries, filters.muscle, { it.displayName }) { onChange(filters.copy(muscle = it)) }
        AppDivider()
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            AppTextButton(
                text = "Clear",
                onClick = { onChange(ExerciseFilters()) },
                enabled = filters.isActive,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            AppButton(text = "Done", onClick = onDismiss)
        }
    }
}

@Composable
private fun <T : Any> FilterSection(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    Section(title = title) {
        UnderlineSelector(
            options = listOf<T?>(null) + options,
            selected = selected,
            onSelect = onSelect,
            label = { it?.let(label) ?: "All" },
        )
    }
    Spacer(Modifier.height(Spacing.l))
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
