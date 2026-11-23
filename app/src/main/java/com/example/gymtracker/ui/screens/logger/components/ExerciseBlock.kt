package com.example.gymtracker.ui.screens.logger.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.screens.logger.ExerciseGroup
import com.example.gymtracker.ui.theme.AppText
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

/** Cardio and mobility log time, not load: no weight column (issue 12). */
val MovementType.logsWeight: Boolean
    get() = this != MovementType.Cardio && this != MovementType.Mobility

/**
 * One exercise in the logger — §3 `ExerciseBlock`, §4.3 (was a bordered `Section` card).
 *
 * Expanded: name + `2/3` + ⋮ (Edit target, Progress, Remove) · amber `TARGET` eyebrow (tap →
 * Target sheet) · rule · column eyebrows once · set rows on hairlines · `+ Add set` foot.
 * Collapsed (S2-b): one 48dp line `✓ name 3/3 ⌄`; tap re-expands. Whether a block is collapsed is
 * decided by the screen, not stored in the draft.
 *
 * The block pads itself (not the list) so done rows can tint edge to edge.
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
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
    focusFirstSet: Boolean = false,
    onFocusConsumed: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().animateContentSize(tween(200))) {
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
                onAddSet = onAddSet,
                focusFirstSet = focusFirstSet,
                onFocusConsumed = onFocusConsumed,
            )
        }
        AppDivider()
    }
}

@Composable
private fun CollapsedHead(group: ExerciseGroup, onExpand: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .clickable(role = Role.Button, onClickLabel = "Expand ${group.exerciseName}", onClick = onExpand)
            .padding(start = Spacing.gutter, end = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Check, contentDescription = "Done", tint = scheme.primary, modifier = Modifier.size(Sizes.icon))
        Spacer(Modifier.width(Spacing.s))
        Text(
            group.exerciseName,
            style = AppText.titleM,
            color = scheme.onSurface,
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
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = scheme.onSurfaceVariant)
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
    onAddSet: () -> Unit,
    focusFirstSet: Boolean,
    onFocusConsumed: () -> Unit,
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
            .padding(start = Spacing.gutter, end = Spacing.xs, top = Spacing.s),
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
            color = if (group.allDone) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s),
        )
        if (group.allDone) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Collapse", tint = scheme.onSurfaceVariant)
            }
        }
        OverflowMenu(
            items = listOf(
                LoggerMenuItem("Edit target", onEditTarget),
                LoggerMenuItem("Progress", onViewProgress),
                LoggerMenuItem("Remove exercise", onRemove, destructive = true),
            ),
            contentDescription = "${group.exerciseName} options",
        )
    }
    Eyebrow(
        "Target " + formatTarget(group.sets.size, group.targetRepsLow, group.targetRepsHigh, group.movementType),
        color = scheme.tertiary,
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = "Edit target", onClick = onEditTarget)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
    )
    AppDivider(Modifier.padding(horizontal = Spacing.gutter))

    // Column heads, once per block — aligned to SetRow's columns.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .padding(top = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow("Set", modifier = Modifier.width(SetTable.badge))
        if (showWeight) Eyebrow(unit.label, modifier = Modifier.weight(1f))
        Eyebrow(group.movementType.repUnitShort, modifier = Modifier.weight(1f))
        Eyebrow("✓", modifier = Modifier.width(Sizes.touchTarget))
    }

    group.sets.forEachIndexed { setIndex, set ->
        if (setIndex > 0) AppDivider(Modifier.padding(horizontal = Spacing.gutter))
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
        )
    }
    AppDivider(Modifier.padding(horizontal = Spacing.gutter))
    AppTextButton(
        text = "Add set",
        onClick = onAddSet,
        icon = Icons.Outlined.Add,
        color = scheme.onSurface,
        modifier = Modifier.padding(horizontal = Spacing.xs),
    )
}
