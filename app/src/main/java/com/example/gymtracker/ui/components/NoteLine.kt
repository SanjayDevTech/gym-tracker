package com.example.gymtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * A tappable note: the text itself, or a quiet "Add a note…" prompt. Tap → edit. With [label],
 * an eyebrow leads the line (`NOTE  Felt strong.`, Session detail §4.9); without, the note stands
 * alone under the title (Plan editor §4.2, issue 9).
 */
@Composable
fun NoteLine(
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit note", onClick = onClick)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            Eyebrow(label)
            Spacer(Modifier.width(Spacing.m))
        }
        Text(
            note.ifBlank { "Add a note…" },
            style = AppText.body,
            color = if (note.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
