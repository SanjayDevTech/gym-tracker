package com.example.gymtracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.atoms.LocalBehind
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.dotTexture

/*
 * Bento tiles — Foodprint's. A tile is a plain card whose colour lives in the details: its
 * concept's hue on the icon, the label and the bar, so a grid of them stays calm. One tile per
 * screen may be [featured] (its soft gradient plus dots). Lay tiles out two to a row with
 * `Arrangement.spacedBy(Spacing.section)` and `Modifier.weight(1f)`, rows `height(IntrinsicSize.Min)`
 * with each tile `fillMaxHeight()` so a row is even.
 */

/** The tile's ink (deep shade of its hue), provided by [RichTile]. */
private val LocalTileInk = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun tileInk(): Color = LocalTileInk.current

/** A bento tile in [rich]'s hue. Tappable when [onClick] is set. */
@Composable
fun RichTile(
    rich: Rich,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** The one tile that stands out: its soft gradient plus the dot texture. */
    featured: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = rich.ink
    val fill = cardColor()
    CompositionLocalProvider(LocalTileInk provides ink, LocalBehind provides fill) {
        Column(
            modifier
                .clip(RoundedCornerShape(Radius.lg))
                .then(
                    if (featured) Modifier.background(rich.brush).dotTexture(color = ink.copy(alpha = 0.14f))
                    else Modifier.background(fill),
                )
                .then(
                    if (onClick != null) {
                        Modifier.semantics(mergeDescendants = true) { role = Role.Button }.pressable(onClick = onClick)
                    } else Modifier.semantics(mergeDescendants = true) {},
                )
                .padding(14.dp),
            content = content,
        )
    }
}

/** Icon disc + label across the top of a tile, with an optional figure on the right. */
@Composable
fun TileHead(icon: ImageVector, label: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(tileInk().copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tileInk(), modifier = Modifier.size(15.dp)) }
        Spacer(Modifier.width(Spacing.s))
        Text(label, style = AppText.label, color = tileInk(), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) Text(trailing, style = AppText.label, color = tileInk().copy(alpha = 0.75f), maxLines = 1)
    }
}

/** A thin progress bar in the tile's ink on a translucent track. */
@Composable
fun TileMeter(fraction: Float, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing), label = "tileMeter")
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(Radius.pill)).background(tileInk().copy(alpha = 0.15f))) {
        if (v > 0f) Box(Modifier.fillMaxWidth(v).height(6.dp).clip(RoundedCornerShape(Radius.pill)).background(tileInk()))
    }
}

/**
 * The common tile: icon and label on top, then the figure (numberL) with its unit, an optional
 * caption, and an optional bar. Use instead of a row of label/number text.
 */
@Composable
fun StatTile(
    rich: Rich,
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    caption: String? = null,
    trailing: String? = null,
    fraction: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    RichTile(rich, modifier.heightIn(min = 104.dp), onClick = onClick) {
        TileHead(icon, label, trailing)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppText.numberL, color = scheme.onSurface, maxLines = 1)
            if (unit.isNotEmpty()) {
                Text(
                    " $unit",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (caption != null) {
            Text(caption, style = AppText.bodyS, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (fraction != null) {
            Spacer(Modifier.height(Spacing.s))
            TileMeter(fraction)
        }
    }
}
