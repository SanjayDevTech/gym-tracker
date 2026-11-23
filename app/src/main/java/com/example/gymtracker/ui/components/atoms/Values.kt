package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * A number and its unit on one baseline — design-plan §2 `ValueText`.
 *
 * [value] arrives already formatted (`ui/format/Format.kt`), so one rounding rule
 * serves the whole app. The unit trails after a non-breaking space in
 * [AppText.bodyS] / `onSurfaceVariant` so the two never read as one value.
 */
@Composable
fun ValueText(
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = AppText.numberM,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = style, color = color)
        if (!unit.isNullOrEmpty()) {
            Text(
                NBSP + unit,
                style = AppText.bodyS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Small badge — `PR`, counts. `primaryContainer` fill at [Radius.sm]: one of the
 * few fills §1 allows, because a record is work done.
 */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Text(
        text.uppercase(),
        style = AppText.eyebrow,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(container)
            // 2dp vertical is the badge's own inset (a size, not a layout gap).
            .padding(horizontal = Spacing.s, vertical = 2.dp),
    )
}
