package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/**
 * One record — §3 `RecordRow` (Summary; Exercise detail later). Exercise name left, value in
 * `primary` right (a record is work done); under it the record kind and the delta vs the
 * previous best, also `primary`. [delta] is null for a first-ever log.
 */
@Composable
fun RecordRow(
    exercise: String,
    kind: String,
    value: String,
    delta: String?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.m)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                exercise,
                style = AppText.titleM,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(value, style = AppText.numberM, color = scheme.primary)
        }
        Row {
            Text(kind, style = AppText.bodyS, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(delta ?: "First log", style = AppText.bodyS, color = if (delta != null) scheme.primary else scheme.onSurfaceVariant)
        }
    }
}
