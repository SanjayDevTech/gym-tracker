package com.example.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.BicepsFlexed
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PersonStanding
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.MuscleGroup
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.data.group
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/** Two-line row minimum. A size, not a gap. */
private val RowMinHeight = 64.dp

/** One pastel per muscle group, so an exercise reads the same colour everywhere. */
val MuscleGroup.pastel: Pastel
    get() = when (this) {
        MuscleGroup.Chest -> Pastel.Rose
        MuscleGroup.Back -> Pastel.Sky
        MuscleGroup.Shoulders -> Pastel.Lavender
        MuscleGroup.Arms -> Pastel.Peach
        MuscleGroup.Legs -> Pastel.Mint
        MuscleGroup.Core -> Pastel.Teal
    }

/** The exercise's pastel: its first primary muscle's group, or sand when it has none. */
val ExerciseEntity.pastel: Pastel
    get() = primaryMuscles.firstOrNull()?.group?.pastel ?: Pastel.Sand

/** One icon per movement type. */
val MovementType.icon: ImageVector
    get() = when (this) {
        MovementType.Compound -> Lucide.Dumbbell
        MovementType.Isolation -> Lucide.BicepsFlexed
        MovementType.Cardio -> Lucide.Activity
        MovementType.Mobility -> Lucide.PersonStanding
    }

/** The exercise's disc: its movement type's icon on its muscle group's pastel. */
@Composable
fun ExerciseIcon(exercise: ExerciseEntity, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    PastelIcon(exercise.movementType.icon, exercise.pastel, modifier, size)
}

/**
 * One row of a white card that is split across lazy-list items: the first row gets the card's
 * top corners, the last its bottom corners, so a long list still reads as one card.
 */
fun Modifier.cardRow(index: Int, count: Int, fill: Color): Modifier {
    val top = if (index == 0) Radius.lg else 0.dp
    val bottom = if (index == count - 1) Radius.lg else 0.dp
    return clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
        .background(fill)
}

/**
 * An exercise in a list: its pastel disc, name in [AppText.titleM], `Barbell · Chest, Triceps`
 * in [AppText.bodyS] (equipment, then primary and secondary muscles), and the movement type —
 * preceded by a `Custom` badge for custom exercises. Callers give the row its card fill
 * ([cardRow]) and divide rows with hairlines.
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
            .pressable(onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseIcon(exercise)
        Spacer(Modifier.width(Spacing.m))
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
        Column(horizontalAlignment = Alignment.End) {
            if (exercise.isCustom) {
                AppBadge("Custom", container = scheme.tertiaryContainer, content = scheme.onTertiaryContainer)
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(exercise.movementType.displayName, style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
    }
}
