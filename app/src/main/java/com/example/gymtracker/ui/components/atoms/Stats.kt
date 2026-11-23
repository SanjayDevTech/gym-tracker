package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Stat displays — design-plan §2 `StatRow` / `StatColumns`.
 *
 * House rule 2: label left, number right; eyebrow labels; units trailing in
 * `bodyS`. These replace `StatTile`, `LabeledLine`, `MetricReadout`,
 * `HeaderStat`, `SummaryStat` and `MetaStat`.
 */

/**
 * Eyebrow label (+ optional caption) left, [AppText.numberL] value + unit right,
 * optional chevron. Clickable when [onClick] is set.
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueStyle: TextStyle = AppText.numberL,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Eyebrow(label)
            if (caption != null) {
                Text(
                    caption,
                    style = AppText.bodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(Spacing.m))
        ValueText(value = value, unit = unit, style = valueStyle, color = valueColor)
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One column of [StatColumns]. */
@Immutable
data class StatItem(
    val label: String,
    val value: String,
    val unit: String? = null,
)

/**
 * 2–4 equal-width columns of eyebrow over number. Every column gets the same
 * width and every label is free to wrap, so at a large font scale a long label
 * ("WORKING SETS") breaks onto two lines instead of ellipsing or pushing its
 * neighbours — house rule 7. Values align on the bottom so wrapped and unwrapped
 * labels still put their numbers on one line.
 */
@Composable
fun StatColumns(
    items: List<StatItem>,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = AppText.numberM,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.Bottom,
    ) {
        items.forEach { item ->
            Column(Modifier.weight(1f)) {
                Eyebrow(item.label)
                ValueText(value = item.value, unit = item.unit, style = valueStyle)
            }
        }
    }
}
