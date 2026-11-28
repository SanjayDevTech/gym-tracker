package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * A number and its unit on one baseline. [value] arrives already formatted
 * (`ui/format/Format.kt`), so one rounding rule serves the whole app. The unit trails after a
 * non-breaking space in [AppText.bodyS] / `onSurfaceVariant` so the two never read as one value.
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

/** Small pill badge — `PR`, counts. Ink by default; records pass the accent. */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Text(
        text,
        style = AppText.eyebrow,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(container)
            .padding(horizontal = Spacing.s, vertical = 2.dp),
    )
}

/** How loud a [Metric] is. One [Hero] per screen; [Large] in cards; [Medium] in dense rows. */
enum class MetricSize { Hero, Large, Medium }

/**
 * The standard "label over a number" block, so the same kind of number is always the same size:
 * label in [AppText.eyebrow], value in the size's number style, unit trailing on the baseline.
 */
@Composable
fun Metric(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    size: MetricSize = MetricSize.Medium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val valueStyle = when (size) {
        MetricSize.Hero -> AppText.numberXXL
        MetricSize.Large -> AppText.numberL
        MetricSize.Medium -> AppText.numberM
    }
    val unitStyle = if (size == MetricSize.Hero) AppText.titleM else AppText.bodyS
    Column(modifier, horizontalAlignment = horizontalAlignment) {
        if (label != null) {
            Text(label, style = AppText.eyebrow, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = valueStyle, color = color, maxLines = 1)
            if (unit.isNotEmpty()) {
                Text(
                    NBSP + unit,
                    style = unitStyle,
                    color = labelColor,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = if (size == MetricSize.Hero) 8.dp else 2.dp),
                )
            }
        }
    }
}
