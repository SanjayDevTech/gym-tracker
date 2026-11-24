package com.example.gymtracker.ui.screens.logger.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.displayName
import com.example.gymtracker.data.kgToDisplay
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.NumberField
import com.example.gymtracker.ui.format.trimDecimal
import com.example.gymtracker.ui.screens.logger.SetDraft
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.DoneRow
import com.example.gymtracker.ui.theme.Effort
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** The check's nudge (§6.2): three accent pulses over ~2s. */
private const val PULSE_COUNT = 3
private const val PULSE_HALF_MILLIS = 330
private const val PULSE_SCALE = 0.25f

/** Size tokens local to the set table (sizes, not gaps). */
internal object SetTable {
    val rowMinHeight = 64.dp
    val badge = 40.dp
}

/**
 * One set — §3 `SetRow`, §4.3.
 *
 * Badge (set number, `W` muted, `F` in [Effort]; tap → type menu + Delete set) · weight
 * [NumberField] · reps [NumberField] · 48dp check. Weight first: "62.5 × 9". A done row carries
 * the full-bleed [DoneRow] tint (house rule 3's gym exception) that fades in over 150ms, and its
 * numerals tween from `onSurfaceVariant` to `onSurface`. A PR turns the numerals `primary` and
 * adds a `PR` badge. The row draws its own gutter so the tint can run edge to edge.
 *
 * [pulse] nudges the empty check — a few `primary` pulses — when typing has filled the set that
 * would complete its exercise, which is left for the user to tick (§6.2).
 */
@Composable
fun SetRow(
    index: Int,
    set: SetDraft,
    unit: WeightUnit,
    showWeight: Boolean,
    isPr: Boolean,
    canDelete: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    pulse: Boolean = false,
    onPulseEnd: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val done = set.isCompleted
    val pulseLevel = remember { Animatable(0f) }
    val endPulse by rememberUpdatedState(onPulseEnd)
    val pulsing = pulse && !done
    LaunchedEffect(pulsing) {
        if (!pulsing) {
            pulseLevel.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(PULSE_COUNT) {
            pulseLevel.animateTo(1f, tween(PULSE_HALF_MILLIS))
            pulseLevel.animateTo(0f, tween(PULSE_HALF_MILLIS))
        }
        endPulse()
    }
    val tint by animateColorAsState(
        if (done) DoneRow else Color.Transparent,
        animationSpec = tween(150),
        label = "doneTint",
    )
    val numeral by animateColorAsState(
        when {
            isPr -> scheme.primary
            done -> scheme.onSurface
            else -> scheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "numeral",
    )
    val firstField = Modifier.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tint)
            .heightIn(min = SetTable.rowMinHeight)
            .padding(horizontal = Spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        SetBadge(
            index = index,
            setType = set.setType,
            canDelete = canDelete,
            onSetTypeChange = onSetTypeChange,
            onDelete = onDelete,
        )
        if (showWeight) {
            NumberField(
                value = set.weightInput,
                onValueChange = onWeightChange,
                hint = set.hintWeightKg?.let { trimDecimal(it.kgToDisplay(unit)) },
                decimal = true,
                color = numeral,
                modifier = Modifier.weight(1f).then(firstField),
            )
        }
        Row(
            modifier = Modifier.weight(1f).then(if (showWeight) Modifier else firstField),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberField(
                value = set.repsInput,
                onValueChange = onRepsChange,
                hint = set.hintReps?.toString(),
                maxLength = 3,
                color = numeral,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = isPr,
                enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.9f),
                exit = fadeOut(tween(200)),
            ) {
                AppBadge("PR", modifier = Modifier.padding(start = Spacing.s))
            }
        }
        IconButton(
            onClick = { onCompletedChange(!done) },
            modifier = Modifier.size(Sizes.touchTarget),
        ) {
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (done) "Mark set ${index + 1} not done" else "Mark set ${index + 1} done",
                tint = if (done) scheme.primary else lerp(scheme.outline, scheme.primary, pulseLevel.value),
                modifier = Modifier.graphicsLayer {
                    val scale = 1f + PULSE_SCALE * pulseLevel.value
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

/**
 * 40dp set badge: the set number, or `W` (warm-up, muted) / `F` (failure, [Effort]). Tap opens
 * Warm-up / Working / Failure / — / Delete set (issue 13: "−" used to remove only the last set).
 */
@Composable
private fun SetBadge(
    index: Int,
    setType: SetType,
    canDelete: Boolean,
    onSetTypeChange: (SetType) -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val (label, color) = when (setType) {
        SetType.Working -> "${index + 1}" to scheme.onSurface
        SetType.Warmup -> "W" to scheme.onSurfaceVariant
        SetType.Failure -> "F" to Effort
    }
    Box {
        Box(
            modifier = Modifier
                .size(SetTable.badge)
                .clip(RoundedCornerShape(Radius.sm))
                .clickable(role = Role.Button, onClickLabel = "Set ${index + 1} options") { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = AppText.numberM, color = color)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(Radius.md),
            containerColor = scheme.surfaceContainer,
        ) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            type.displayName,
                            style = AppText.body,
                            color = if (type == setType) scheme.onSurface else scheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = if (type == setType) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = scheme.onSurface) }
                    } else null,
                    onClick = {
                        open = false
                        if (type != setType) onSetTypeChange(type)
                    },
                )
            }
            HorizontalDivider(thickness = Elevation.hairline, color = scheme.outlineVariant)
            DropdownMenuItem(
                text = {
                    Text(
                        "Delete set",
                        style = AppText.body,
                        color = if (canDelete) scheme.error else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                enabled = canDelete,
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}
