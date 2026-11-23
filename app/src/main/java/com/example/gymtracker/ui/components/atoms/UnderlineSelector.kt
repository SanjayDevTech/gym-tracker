package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Single-select row of text options — design-plan §2, rewrite of
 * `ExclusiveButtonGroup`.
 *
 * Selection is a 24×2dp `onSurface` rule under a full-strength label (house
 * rule 3): no filled chip, no accent — selection is chrome, and the accent means
 * work done. Unselected labels sit in `onSurfaceVariant`.
 *
 * The haptic tick is kept and fires only on an actual change; re-tapping the
 * selected option is a no-op (no tick, no [onSelect]), because callers persist
 * on every select.
 *
 * [fillWidth] gives every option an equal share of the row (labels wrap if they
 * must); otherwise each option sizes to its label and the row scrolls.
 */
@Composable
fun <T> UnderlineSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    label: (T) -> String,
) {
    val haptics = rememberAppHaptics()
    val rowModifier = if (fillWidth) modifier else modifier.horizontalScroll(rememberScrollState())
    Row(
        modifier = rowModifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(if (fillWidth) Spacing.xs else Spacing.l),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val scheme = MaterialTheme.colorScheme
            Box(
                modifier = (if (fillWidth) Modifier.weight(1f) else Modifier)
                    .heightIn(min = Sizes.touchTarget)
                    .semantics {
                        this.selected = isSelected
                        role = Role.Tab
                    }
                    .clickable {
                        if (!isSelected) {
                            haptics.light()
                            onSelect(option)
                        }
                    }
                    .padding(vertical = Spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label(option),
                        style = AppText.label,
                        color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Box(
                        Modifier
                            .size(width = 24.dp, height = 2.dp)
                            .background(if (isSelected) scheme.onSurface else Color.Transparent),
                    )
                }
            }
        }
    }
}
