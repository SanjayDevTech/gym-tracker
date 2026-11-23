package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * `[−] value [+]` — design-plan §2 `Stepper`. 48dp outlined circular buttons,
 * value in [AppText.numberL]. Replaces both private `StepperRow`s (and the unused
 * `NumberStepper`). [format] renders the value (e.g. `90 s`); [step] is the
 * increment. Each change ticks a light haptic; ends of [range] disable the button.
 */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    format: (Int) -> String = { it.toString() },
    decrementLabel: String = "Decrease",
    incrementLabel: String = "Increase",
) {
    val haptics = rememberAppHaptics()
    val border = BorderStroke(Elevation.hairline, MaterialTheme.colorScheme.outline)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        OutlinedIconButton(
            onClick = {
                haptics.light()
                onValueChange((value - step).coerceAtLeast(range.first))
            },
            enabled = value > range.first,
            border = border,
            modifier = Modifier.size(Sizes.iconButton),
        ) { Icon(Icons.Outlined.Remove, contentDescription = decrementLabel) }

        Text(
            format(value),
            style = AppText.numberL,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            // Wide enough for "999" at 28sp so the buttons don't shift as it changes.
            modifier = Modifier.widthIn(min = 64.dp),
        )

        OutlinedIconButton(
            onClick = {
                haptics.light()
                onValueChange((value + step).coerceAtMost(range.last))
            },
            enabled = value < range.last,
            border = border,
            modifier = Modifier.size(Sizes.iconButton),
        ) { Icon(Icons.Outlined.Add, contentDescription = incrementLabel) }
    }
}
