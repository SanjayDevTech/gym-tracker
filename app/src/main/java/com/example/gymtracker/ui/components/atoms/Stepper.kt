package com.example.gymtracker.ui.components.atoms

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.shape.CircleShape
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
 * `[−] value [+]` — Foodprint's stepper: two 40dp soft round keys flanking the value in
 * [AppText.numberM]. Replaces both private `StepperRow`s (and the unused
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
    val scheme = MaterialTheme.colorScheme
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = softFill(),
        contentColor = scheme.onSurface,
        disabledContainerColor = softFill().copy(alpha = 0.5f),
        disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.38f),
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        FilledIconButton(
            onClick = {
                haptics.light()
                onValueChange((value - step).coerceAtLeast(range.first))
            },
            enabled = value > range.first,
            colors = colors,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) { Icon(Lucide.Minus, contentDescription = decrementLabel, modifier = Modifier.size(18.dp)) }

        Text(
            format(value),
            style = AppText.numberM,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            // Wide enough for "999" at 28sp so the buttons don't shift as it changes.
            modifier = Modifier.widthIn(min = 64.dp),
        )

        FilledIconButton(
            onClick = {
                haptics.light()
                onValueChange((value + step).coerceAtMost(range.last))
            },
            enabled = value < range.last,
            colors = colors,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) { Icon(Lucide.Plus, contentDescription = incrementLabel, modifier = Modifier.size(18.dp)) }
    }
}
