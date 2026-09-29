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
import com.example.gymtracker.ui.theme.Dimens

@Composable
fun Section(
    modifier: Modifier = Modifier,
    title: String? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Dimens.SectionPaddingH,
        vertical = Dimens.SectionPaddingV,
    ),
    content: @Composable () -> Unit,
) {
    val resolvedBorder = borderColor ?: MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.SectionCornerRadius))
            .border(Dimens.SectionBorderWidth, resolvedBorder, RoundedCornerShape(Dimens.SectionCornerRadius))
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterS),
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
