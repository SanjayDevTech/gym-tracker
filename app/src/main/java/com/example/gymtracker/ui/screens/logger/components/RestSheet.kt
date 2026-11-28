package com.example.gymtracker.ui.screens.logger.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.Meter
import com.example.gymtracker.ui.components.atoms.Stepper
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

private val Presets: List<Int?> = listOf(60, 90, 120, 180)

/**
 * Rest sheet — §4.4. The dock already carries the transport; this sheet is for the *target*.
 *
 * Live `numberXXL` rest clock with `of 1:30`, the meter (rest hue, green once reached), a card
 * with a 15s [Stepper] (0:15–10:00) plus preset selector, "Use as default for new workouts", and
 * a footer of `Restart now` · `Done`. Target changes apply live; the default pref is written on
 * Done. Before the first ticked set the hero reads `—`.
 */
@Composable
fun RestSheet(
    /** Read inside the sheet, so the per-second tick doesn't recompose the logger screen. */
    restElapsed: () -> Long?,
    targetSeconds: Int,
    range: IntRange,
    step: Int,
    onTargetChange: (Int) -> Unit,
    onRestart: () -> Unit,
    onDone: (saveAsDefault: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var asDefault by remember { mutableStateOf(false) }
    val targetMillis = targetSeconds * 1000L

    AppSheet(onDismiss = onDismiss) {
        val restElapsedMillis = restElapsed()
        val reached = restElapsedMillis != null && restElapsedMillis >= targetMillis
        SheetTitleRow("Rest", onClose = onDismiss)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                restElapsedMillis?.let { formatClock(it) } ?: EM_DASH,
                style = AppText.numberXXL,
                color = when {
                    restElapsedMillis == null -> scheme.onSurfaceVariant
                    reached -> scheme.secondary
                    else -> scheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                "of ${formatClock(targetMillis)}",
                style = AppText.titleM,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.s),
            )
        }
        if (restElapsedMillis == null) {
            Text("Starts when you tick a set", style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.s))
        Meter(
            fraction = restElapsedMillis?.let { it.toFloat() / targetMillis } ?: 0f,
            color = if (reached) scheme.secondary else Rich.Rest.ink,
        )

        Spacer(Modifier.height(Spacing.xl))
        Section("Target") {
        Stepper(
            value = targetSeconds,
            onValueChange = onTargetChange,
            range = range,
            step = step,
            format = { formatClock(it * 1000L) },
            decrementLabel = "Shorter rest",
            incrementLabel = "Longer rest",
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(Spacing.s))
        ExclusiveButtonGroup(
            options = Presets,
            selected = targetSeconds.takeIf { it in Presets },
            onSelect = { it?.let(onTargetChange) },
            fillWidth = true,
            label = { formatClock((it ?: 0) * 1000L) },
        )

        Spacer(Modifier.height(Spacing.s))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.touchTarget)
                .toggleable(value = asDefault, role = Role.Checkbox, onValueChange = { asDefault = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = asDefault,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = scheme.primary, checkmarkColor = scheme.onPrimary),
            )
            Text(
                "Use as default for new workouts",
                style = AppText.body,
                color = scheme.onSurface,
                modifier = Modifier.padding(start = Spacing.m),
            )
        }
        }

        Spacer(Modifier.height(Spacing.l))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            AppOutlinedButton(text = "Restart now", onClick = onRestart, modifier = Modifier.weight(1f))
            AppButton(text = "Done", onClick = { onDone(asDefault) }, modifier = Modifier.weight(1f))
        }
    }
}
