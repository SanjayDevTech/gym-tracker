package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/**
 * "There is nothing here" — design-plan §2. A [AppText.titleM] line saying what
 * happened, a [AppText.bodyS] line saying why, and optionally one text action.
 * No icon (house rule 4), left-aligned on the gutter like every other block.
 *
 * Render this only after a real **empty** emission, never while loading
 * (house rule 8).
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.xxxl)) {
        Text(title, style = AppText.titleM, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            message,
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Comfortable measure; a size, not a gap.
            modifier = Modifier.widthIn(max = 360.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.s))
            AppTextButton(text = actionLabel, onClick = onAction)
        }
    }
}
