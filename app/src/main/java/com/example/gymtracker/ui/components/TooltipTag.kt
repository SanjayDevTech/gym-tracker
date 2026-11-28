package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing
import kotlin.math.roundToInt

/** The tag's pointer: 4dp tall. Sizes, not gaps. */
private val PointerHeight = 4.dp
private val PointerWidth = 8.dp

/**
 * `TooltipTag` body: an ink pill (`inverseSurface`) with [AppText.label] in `inverseOnSurface`,
 * like Foodprint's snackbars. Use it through [TagOverlay], which adds the 4dp pointer and clamps the
 * tag inside the plot.
 */
@Composable
fun TooltipTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = AppText.label,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
    )
}

/**
 * Draws [content] and, when [tagText] is set, a [TooltipTag] whose pointer tip touches
 * ([anchorX], [anchorY]) px. The tag is clamped inside [content]'s bounds horizontally and never
 * rises above its top (food lesson 4); the pointer stays on the anchor even when the tag is
 * pushed sideways.
 */
@Composable
fun TagOverlay(
    tagText: String?,
    anchorX: Float,
    anchorY: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fill = MaterialTheme.colorScheme.inverseSurface
    Layout(
        contents = listOf(
            content,
            { if (tagText != null) TooltipTag(tagText) },
            {
                if (tagText != null) {
                    Canvas(Modifier.size(PointerWidth, PointerHeight)) {
                        drawPath(
                            Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2f, size.height)
                                close()
                            },
                            fill,
                        )
                    }
                }
            },
        ),
        modifier = modifier,
    ) { (contentM, tagM, pointerM), constraints ->
        val body = contentM.first().measure(constraints)
        val loose = Constraints(maxWidth = body.width)
        val tag = tagM.firstOrNull()?.measure(loose)
        val pointer = pointerM.firstOrNull()?.measure(loose)
        layout(body.width, body.height) {
            body.place(0, 0)
            if (tag != null && pointer != null) {
                val pointerY = (anchorY - pointer.height).roundToInt().coerceAtLeast(tag.height)
                val tagY = pointerY - tag.height
                val tagX = (anchorX - tag.width / 2f).roundToInt()
                    .coerceIn(0, (body.width - tag.width).coerceAtLeast(0))
                val pointerX = (anchorX - pointer.width / 2f).roundToInt()
                    .coerceIn(tagX, (tagX + tag.width - pointer.width).coerceAtLeast(tagX))
                tag.place(tagX, tagY)
                pointer.place(pointerX, pointerY)
            }
        }
    }
}
