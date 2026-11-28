package com.example.gymtracker.ui.components

import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.formatTarget
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Stepper
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** Same caps for every caller (issue 10): sets 1–10; 50 reps / 120 min / 180 sec. */
val MovementType.targetCap: Int
    get() = when (this) {
        MovementType.Cardio -> 120
        MovementType.Mobility -> 180
        else -> 50
    }

const val TARGET_MAX_SETS = 10

/**
 * Target sheet — §4.7, one component for the logger and (phase 5) the Plan editor.
 *
 * Title, a live `numberL` preview in tertiary, then SETS (only when [sets] is non-null — hidden
 * in the logger, where the set table *is* the count), FROM and TO steppers with unit-aware
 * labels; FROM ≤ TO is kept consistent. Footer: optional `View progress →` and destructive
 * remove link, then Apply.
 */
@Composable
fun TargetSheet(
    exerciseName: String,
    movementType: MovementType,
    initialLow: Int,
    initialHigh: Int,
    onApply: (sets: Int?, low: Int, high: Int) -> Unit,
    onDismiss: () -> Unit,
    initialSets: Int? = null,
    previewSets: Int = initialSets ?: 1,
    onViewProgress: (() -> Unit)? = null,
    removeLabel: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val cap = movementType.targetCap
    var sets by remember { mutableIntStateOf(initialSets?.coerceIn(1, TARGET_MAX_SETS) ?: 0) }
    var low by remember { mutableIntStateOf(initialLow.coerceIn(1, cap)) }
    var high by remember { mutableIntStateOf(initialHigh.coerceIn(low, cap)) }
    val unit = movementType.repUnit

    AppSheet(onDismiss = onDismiss) {
        SheetTitleRow(exerciseName, onClose = onDismiss)
        Text(
            formatTarget(if (initialSets != null) sets else previewSets, low, high, movementType),
            style = AppText.numberL,
            color = scheme.tertiary,
        )
        Spacer(Modifier.height(Spacing.m))
        if (initialSets != null) {
            StepperLine("Sets", sets, 1..TARGET_MAX_SETS) { sets = it }
        }
        StepperLine("From · $unit", low, 1..cap) { v ->
            low = v
            if (high < v) high = v
        }
        StepperLine("To · $unit", high, 1..cap) { v ->
            high = v
            if (low > v) low = v
        }
        Spacer(Modifier.height(Spacing.m))
        if (onViewProgress != null || onRemove != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onViewProgress != null) {
                    AppTextButton(
                        text = "View progress",
                        onClick = onViewProgress,
                        icon = Lucide.ArrowRight,
                        color = scheme.onSurface,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onRemove != null) {
                    AppTextButton(text = removeLabel ?: "Remove", onClick = onRemove, color = scheme.error)
                }
            }
        }
        Spacer(Modifier.height(Spacing.s))
        AppButton(
            text = "Apply",
            onClick = { onApply(initialSets?.let { sets }, low, high) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StepperLine(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(label, style = AppText.body, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Stepper(
            value = value,
            onValueChange = onChange,
            range = range,
            decrementLabel = "Decrease $label",
            incrementLabel = "Increase $label",
        )
    }
}
