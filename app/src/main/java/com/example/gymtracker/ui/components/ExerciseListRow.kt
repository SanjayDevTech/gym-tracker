package com.example.gymtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/** Two-line row minimum. A size, not a gap. */
private val RowMinHeight = 64.dp

/**
 * §3 `ExerciseListRow` (rewrite): no card, no chip. Name in [AppText.titleM], `Barbell · Chest,
 * Triceps` in [AppText.bodyS] (equipment, then primary and secondary muscles), and a trailing
 * eyebrow with the movement type — preceded by `CUSTOM` in `tertiary` for custom exercises.
 * Callers divide rows with hairlines.
 */
@Composable
fun ExerciseListRow(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
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
            val muscles = (exercise.primaryMuscles + exercise.secondaryMuscles).distinct()
                .joinToString(", ") { it.displayName }
            Text(
                listOf(exercise.equipment.displayName, muscles).filter { it.isNotBlank() }.joinToString(" · "),
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.m))
        if (exercise.isCustom) {
            Eyebrow("Custom", color = scheme.tertiary)
            Spacer(Modifier.width(Spacing.s))
        }
        Eyebrow(exercise.movementType.displayName)
    }
}
