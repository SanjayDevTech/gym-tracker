package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Spacing

/**
 * A standing head with a rule under it, then its content — house rule 1.
 *
 * Head (eyebrow + right-aligned [trailing] figure) · 8 · hairline · 16 · content.
 * No border, no fill: the rule belongs to the heading, not to a box around the
 * content. Stack sections with `spacedBy(Spacing.section)` (24dp).
 *
 * [trailing] is the section's **figure** (`3/4`, `12 400 kg`), never an action —
 * actions go in a foot under the content.
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SectionHead(title = title, trailing = trailing)
        Spacer(Modifier.height(Spacing.s))
        AppDivider()
        Spacer(Modifier.height(Spacing.l))
        content()
    }
}

/** The head alone: uppercase eyebrow left, optional figure right. */
@Composable
fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(title, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** 11sp tracked uppercase label. Uppercased here so call sites can't forget. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(text.uppercase(), style = AppText.eyebrow, color = color, modifier = modifier)
}

/** 1dp rule, full width; `outlineVariant` unless a [color] is given (e.g. the logger dock's edge). */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Elevation.hairline)
            .background(color),
    )
}
