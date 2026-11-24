package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Actions — design-plan §2.
 *
 * Pill, **zero elevation in all four states** (default / pressed / focused /
 * hovered), [AppText.label]. 48dp is a **minimum** (`heightIn`), never a height
 * (house rule 7): at large font scales the label's line box outgrows a fixed 48dp
 * and would be clipped. No tonal fills anywhere in the family, so these replace
 * every `FilledTonalButton`.
 */

private val ButtonIconSize = 18.dp

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
        Spacer(Modifier.width(Spacing.s))
    }
    Text(text, style = AppText.label)
}

/**
 * The primary action. Filled `primary`, pill, flat. [destructive] swaps the fill
 * to `error` — used only by [ConfirmSheet]-style confirmations.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        colors = if (destructive) {
            ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError)
        } else {
            ButtonDefaults.buttonColors()
        },
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.none,
            pressedElevation = Elevation.none,
            focusedElevation = Elevation.none,
            hoveredElevation = Elevation.none,
            disabledElevation = Elevation.none,
        ),
    ) { ButtonContent(text, icon) }
}

/** The secondary action. Hairline outline, no fill. [color] is the label (`error` for Discard). */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.none,
            pressedElevation = Elevation.none,
            focusedElevation = Elevation.none,
            hoveredElevation = Elevation.none,
            disabledElevation = Elevation.none,
        ),
    ) { ButtonContent(text, icon) }
}

/**
 * The tertiary action — a text link that still has a real 48dp tap target.
 * [color] defaults to `primary`; pass `error` for a destructive link or
 * `onSurfaceVariant` for a quiet one.
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) { ButtonContent(text, icon) }
}

/** Extended FAB: primary / onPrimary, pill, zero elevation in all four states. */
@Composable
fun AppFab(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    expanded: Boolean = true,
) {
    ExtendedFloatingActionButton(
        text = { Text(text, style = AppText.label) },
        icon = { if (icon != null) Icon(icon, contentDescription = null) },
        onClick = onClick,
        expanded = expanded,
        modifier = modifier.heightIn(min = Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = Elevation.none,
            pressedElevation = Elevation.none,
            focusedElevation = Elevation.none,
            hoveredElevation = Elevation.none,
        ),
    )
}

/** The one spinner: 24dp, 2dp stroke, primary on an outlineVariant track. */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    size: Dp = Sizes.icon,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeWidth = 2.dp,
    )
}
