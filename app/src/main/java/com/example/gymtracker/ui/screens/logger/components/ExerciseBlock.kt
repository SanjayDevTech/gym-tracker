package com.example.gymtracker.ui.screens.logger.components

import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.formatTarget
import com.example.gymtracker.data.repUnitShort
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.OverflowMenu
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.LocalBehind
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.screens.logger.ExerciseGroup
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** Per-set callbacks, bundled so the block's signature stays readable. */
class SetActions(
    val onWeightChange: (setIndex: Int, value: String) -> Unit,
    val onRepsChange: (setIndex: Int, value: String) -> Unit,
    val onCompletedChange: (setIndex: Int, completed: Boolean) -> Unit,
    val onSetTypeChange: (setIndex: Int, type: SetType) -> Unit,
    val onDelete: (setIndex: Int) -> Unit,
)

/** Cardio and mobility log time, not load: no weight column. */
val MovementType.logsWeight: Boolean
    get() = this != MovementType.Cardio && this != MovementType.Mobility

/**
 * One exercise in the logger: a white card, as Foodprint's sections.
 *
 * Expanded: name + `2/3` + ⋮ (Edit target, Progress, Skip/Unskip, Remove) · the target line (tap →
 * Target sheet) · column labels once · set rows · a soft `+ Add set` pill.
 * Collapsed: one 56dp line `✓ name 3/3 ⌄` (`Skipped name 1/3 ⌄` when skipped); tap re-expands.
 * Whether a block is collapsed is decided by the screen, not stored in the draft.
 *
 * The card pads its rows itself (not the list) so done rows can tint edge to edge inside it.
 */
@Composable
fun ExerciseBlock(
    group: ExerciseGroup,
    unit: WeightUnit,
    collapsed: Boolean,
    prSetIndexes: Set<Int>,
    sets: SetActions,
    onToggleCollapsed: () -> Unit,
    onEditTarget: () -> Unit,
    onViewProgress: () -> Unit,
    onRemove: () -> Unit,
    onSkipChange: (skipped: Boolean) -> Unit,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
    focusFirstSet: Boolean = false,
    onFocusConsumed: () -> Unit = {},
    pulseSetIndex: Int? = null,
    onPulseEnd: () -> Unit = {},
) {
    val fill = cardColor()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(fill)
            .animateContentSize(tween(200)),
    ) {
      CompositionLocalProvider(LocalBehind provides fill) {
        if (collapsed) {
            CollapsedHead(group, onExpand = onToggleCollapsed)
        } else {
            ExpandedBlock(
                group = group,
                unit = unit,
                prSetIndexes = prSetIndexes,
                sets = sets,
                onCollapse = onToggleCollapsed,
                onEditTarget = onEditTarget,
                onViewProgress = onViewProgress,
                onRemove = onRemove,
                onSkipChange = onSkipChange,
                onAddSet = onAddSet,
                focusFirstSet = focusFirstSet,
                onFocusConsumed = onFocusConsumed,
                pulseSetIndex = pulseSetIndex,
                onPulseEnd = onPulseEnd,
            )
        }
      }
    }
}

@Composable
private fun CollapsedHead(group: ExerciseGroup, onExpand: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = "Expand ${group.exerciseName}", onClick = onExpand)
            .padding(start = Spacing.cardPadding, end = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A skipped exercise reads as skipped, not done: muted label and name instead of ✓.
        val skipped = group.skipped && !group.allDone
        if (skipped) {
            Text("Skipped", style = AppText.label, color = scheme.onSurfaceVariant)
        } else {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(scheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Check, contentDescription = "Done", tint = scheme.onSecondary, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(Spacing.s))
        Text(
            group.exerciseName,
            style = AppText.titleM,
            color = if (skipped) scheme.onSurfaceVariant else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${group.doneCount}/${group.sets.size}",
            style = AppText.numberM,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s),
        )
        Icon(Lucide.ChevronDown, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpandedBlock(
    group: ExerciseGroup,
    unit: WeightUnit,
    prSetIndexes: Set<Int>,
    sets: SetActions,
    onCollapse: () -> Unit,
    onEditTarget: () -> Unit,
    onViewProgress: () -> Unit,
    onRemove: () -> Unit,
    onSkipChange: (skipped: Boolean) -> Unit,
    onAddSet: () -> Unit,
    focusFirstSet: Boolean,
    onFocusConsumed: () -> Unit,
    pulseSetIndex: Int?,
    onPulseEnd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val showWeight = group.movementType.logsWeight
    val focusRequester = remember { FocusRequester() }
    if (focusFirstSet) {
        LaunchedEffect(group.uid) {
            runCatching { focusRequester.requestFocus() }
            onFocusConsumed()
        }
    }

    // Head: name · x/y · (collapse when done) · ⋮
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.cardPadding, end = Spacing.xs, top = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            group.exerciseName,
            style = AppText.titleM,
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${group.doneCount}/${group.sets.size}",
            style = AppText.numberM,
            color = if (group.allDone) scheme.secondary else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s),
        )
        if (group.settled) {
            IconButton(onClick = onCollapse) {
                Icon(Lucide.ChevronUp, contentDescription = "Collapse", tint = scheme.onSurfaceVariant)
            }
        }
        OverflowMenu(
            items = listOf(
                MenuItem("Edit target", onEditTarget),
                MenuItem("Progress", onViewProgress),
                if (group.skipped) {
                    MenuItem("Unskip exercise", { onSkipChange(false) })
                } else {
                    MenuItem("Skip exercise", { onSkipChange(true) }, enabled = !group.allDone)
                },
                MenuItem("Remove exercise", onRemove, destructive = true),
            ),
            contentDescription = "${group.exerciseName} options",
        )
    }
    Text(
        "Target " + formatTarget(group.sets.size, group.targetRepsLow, group.targetRepsHigh, group.movementType),
        style = AppText.bodyS,
        color = scheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = "Edit target", onClick = onEditTarget)
            .padding(horizontal = Spacing.cardPadding)
            .padding(bottom = Spacing.s),
    )

    // Column heads, once per block — aligned to SetRow's columns.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.cardPadding)
            .padding(top = Spacing.xs, bottom = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow("Set", modifier = Modifier.width(SetTable.badge))
        if (showWeight) Eyebrow(unit.label, modifier = Modifier.weight(1f))
        Eyebrow(group.movementType.repUnitShort, modifier = Modifier.weight(1f))
        // Same 48dp slot as SetRow's check, centred, so ✓ sits over the circles.
        Box(Modifier.width(Sizes.touchTarget), contentAlignment = Alignment.Center) { Eyebrow("✓") }
    }

    group.sets.forEachIndexed { setIndex, set ->
        SetRow(
            index = setIndex,
            set = set,
            unit = unit,
            showWeight = showWeight,
            isPr = setIndex in prSetIndexes,
            canDelete = group.sets.size > 1,
            onWeightChange = { sets.onWeightChange(setIndex, it) },
            onRepsChange = { sets.onRepsChange(setIndex, it) },
            onCompletedChange = { sets.onCompletedChange(setIndex, it) },
            onSetTypeChange = { sets.onSetTypeChange(setIndex, it) },
            onDelete = { sets.onDelete(setIndex) },
            focusRequester = if (setIndex == 0) focusRequester else null,
            pulse = setIndex == pulseSetIndex,
            onPulseEnd = onPulseEnd,
        )
    }
    AppOutlinedButton(
        text = "Add set",
        onClick = onAddSet,
        icon = Lucide.Plus,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.cardPadding)
            .padding(top = Spacing.s, bottom = Spacing.cardPadding),
    )
}
