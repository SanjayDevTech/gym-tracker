package com.example.gymtracker.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

@Composable
fun Section(
    modifier: Modifier = Modifier,
    title: String? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.l,
        vertical = Spacing.m,
    ),
    content: @Composable () -> Unit,
) {
    val resolvedBorder = borderColor ?: MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.lg))
            .border(Elevation.hairline, resolvedBorder, RoundedCornerShape(Radius.lg))
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}
