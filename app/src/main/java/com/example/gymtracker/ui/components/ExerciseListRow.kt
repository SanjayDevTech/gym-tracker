package com.example.gymtracker.ui.components

import com.example.gymtracker.data.displayName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.Radius

/**
 * The canonical exercise list row, shared by the Exercise library screen and the
 * "Pick an exercise" sheet so the two stay visually identical. Shows the name with a
 * movement-type badge, primary muscles in the accent color, and equipment · secondary
 * muscles as a muted subtitle.
 */
@Composable
fun ExerciseListRow(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                TypeBadge(typeName = exercise.movementType.name)
            }
            if (exercise.primaryMuscles.isNotEmpty()) {
                Text(
                    exercise.primaryMuscles.joinToString { it.displayName },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val secondary = exercise.secondaryMuscles.joinToString { it.displayName }
            Text(
                buildString {
                    append(exercise.equipment.name)
                    if (secondary.isNotBlank()) append(" · ").append(secondary)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypeBadge(typeName: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(typeName, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}
