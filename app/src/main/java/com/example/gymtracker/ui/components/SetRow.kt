package com.example.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.kgToDisplay
import com.example.gymtracker.data.repUnitShort
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

@Composable
fun SetRow(
    index: Int,
    repsInput: String,
    weightInput: String,
    hintReps: Int?,
    hintWeightKg: Float?,
    isCompleted: Boolean,
    setType: SetType,
    unit: WeightUnit,
    movementType: MovementType = MovementType.Compound,
    isPr: Boolean = false,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val countLabel = movementType.repUnitShort
    val repsHint = hintReps?.let { "$it $countLabel" } ?: "— $countLabel"
    val weightHint = hintWeightKg?.kgToDisplay(unit)?.let { "$it ${unit.label}" } ?: "— ${unit.label}"

    // A new record subtly tints the whole row so the achievement reads at a glance.
    val rowModifier = if (isPr) {
        modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
    } else {
        modifier.padding(vertical = Spacing.xs)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        SetTypeBadge(
            index = index,
            setType = setType,
            isPr = isPr,
            onSetTypeChange = onSetTypeChange,
        )
        OutlinedTextField(
            value = repsInput,
            onValueChange = { v -> onRepsChange(v.filter(Char::isDigit).take(3)) },
            placeholder = { Text(repsHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = weightInput,
            onValueChange = { v ->
                val cleaned = v.filter { it.isDigit() || it == '.' }.take(6)
                onWeightChange(cleaned)
            },
            placeholder = { Text(weightHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        Checkbox(checked = isCompleted, onCheckedChange = onCompletedChange)
    }
}

/**
 * Tappable set-number chip that doubles as the set-type picker. Working sets stay visually quiet
 * (neutral surface); warm-up / failure / no-set get a subtle accent and a one-letter tag so they
 * read at a glance without shouting.
 */
@Composable
private fun SetTypeBadge(
    index: Int,
    setType: SetType,
    isPr: Boolean,
    onSetTypeChange: (SetType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    when (setType) {
        SetType.Working -> { container = scheme.surfaceVariant; content = scheme.onSurfaceVariant }
        SetType.Warmup -> { container = scheme.tertiaryContainer; content = scheme.onTertiaryContainer }
        SetType.Failure -> { container = scheme.errorContainer; content = scheme.onErrorContainer }
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(Radius.sm),
            color = container,
            contentColor = content,
            modifier = Modifier.sizeIn(minWidth = 30.dp, minHeight = 44.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val tag = setType.shortLabel
                if (tag.isNotEmpty()) {
                    Text(tag, style = AppText.eyebrow)
                }
            }
        }
        if (isPr) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = "Personal record",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(15.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onSetTypeChange(type)
                        expanded = false
                    },
                    trailingIcon = if (type == setType) {
                        { Text("✓", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}
