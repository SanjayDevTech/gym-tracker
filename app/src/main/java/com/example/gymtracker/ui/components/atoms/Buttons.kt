package com.example.gymtracker.ui.components.atoms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Actions — Foodprint's: [AppButton] (ink pill), [AppOutlinedButton] (soft pill), [AppTextButton]
 * (link with a real tap target). All flat, all sink slightly when pressed. [Sizes.button] is a
 * minimum height, never a fixed one, so labels survive large font scales.
 */

private val ButtonIconSize = 18.dp

private val NoElevation
    @Composable get() = ButtonDefaults.buttonElevation(
        defaultElevation = Elevation.none,
        pressedElevation = Elevation.none,
        focusedElevation = Elevation.none,
        hoveredElevation = Elevation.none,
        disabledElevation = Elevation.none,
    )

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, style: androidx.compose.ui.text.TextStyle) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
        Spacer(Modifier.width(Spacing.s))
    }
    Text(text, style = style)
}

/** The primary action: filled ink pill. [destructive] swaps the fill to `error`. */
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
        modifier = modifier.heightIn(min = Sizes.button).pressScale(),
        shape = RoundedCornerShape(Radius.pill),
        colors = if (destructive) {
            ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError)
        } else {
            ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
        },
        elevation = NoElevation,
    ) { ButtonContent(text, icon, AppText.button) }
}

/**
 * The secondary action: an outlined pill, so it stands apart from the card it sits on whatever
 * the card's colour. [color] is the label (`error` for Discard).
 */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button).pressScale(),
        shape = RoundedCornerShape(Radius.pill),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { ButtonContent(text, icon, AppText.button) }
}

/**
 * The tertiary action — a text link that still has a real tap target. [color] defaults to ink;
 * pass `error` for a destructive link or `onSurfaceVariant` for a quiet one.
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.touchTarget),
        shape = RoundedCornerShape(Radius.pill),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) { ButtonContent(text, icon, AppText.label) }
}

/** Extended FAB: the ink pill, floating, flat. */
@Composable
fun AppFab(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    expanded: Boolean = true,
) {
    ExtendedFloatingActionButton(
        text = { Text(text, style = AppText.button) },
        icon = { if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize)) },
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

/** Sinks a Material button slightly while pressed, without stealing its ripple. */
@Composable
private fun Modifier.pressScale(): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btnPress")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/** The one spinner. */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeWidth = 2.dp,
    )
}
