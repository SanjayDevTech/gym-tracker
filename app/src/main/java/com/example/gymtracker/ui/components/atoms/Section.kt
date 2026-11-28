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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.AppTheme
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * The fill a card uses here. Null means "on the page canvas"; sheets provide
 * [com.example.gymtracker.ui.theme.AppExtras.cardInSheet] so a card inside a sheet still reads
 * as a card.
 */
val LocalCardColor = compositionLocalOf<Color?> { null }

@Composable
@ReadOnlyComposable
fun cardColor(): Color = LocalCardColor.current ?: AppTheme.extras.card

/** The fill directly behind the current content; null means the page canvas. */
val LocalBehind = compositionLocalOf<Color?> { null }

/**
 * A fill for small controls — chips, steppers, toggle tracks — that always contrasts with what is
 * behind it: grey on a white card, white on grey.
 */
@Composable
@ReadOnlyComposable
fun softFill(): Color {
    val behind = LocalBehind.current ?: MaterialTheme.colorScheme.background
    return if (behind == AppTheme.extras.card) MaterialTheme.colorScheme.surfaceContainerHigh
    else AppTheme.extras.card
}

/**
 * A titled card: 24dp corners, a white fill on the grey canvas, the title in the display face and
 * an optional summary figure on the right. Stack them with `spacedBy(Spacing.section)`.
 *
 * [trailing] is the section's summary — `3 of 4`, `12,400 kg` — never an action.
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fill = cardColor()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(fill)
            .padding(Spacing.cardPadding),
    ) {
        CompositionLocalProvider(LocalBehind provides fill) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = AppText.titleM,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke(this)
            }
            Spacer(Modifier.height(Spacing.m))
            content()
        }
    }
}

/** A head on the canvas, above a list of cards: title left, optional figure right. */
@Composable
fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = AppText.titleM,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/** The small label above a figure or field — "Volume", "Last time". */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(text, style = AppText.eyebrow, color = color, modifier = modifier)
}

/** 1dp rule, full width; `outlineVariant` unless a [color] is given. */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Elevation.hairline)
            .background(color),
    )
}
