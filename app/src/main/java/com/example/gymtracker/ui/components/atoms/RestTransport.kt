package com.example.gymtracker.ui.components.atoms

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * The rest clock's transport — design-plan §2 `RestTransport` (S5-g).
 *
 * `− 15 s` · a 56dp ink `Skip` pill · `+ 15 s`. Skip is the action under the thumb between every
 * set, so it is the filled one; the step buttons are soft pills — they adjust a value.
 */
@Composable
fun RestTransport(
    onMinus: () -> Unit,
    onSkip: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    stepSeconds: Int = 15,
    enabled: Boolean = true,
    skipEnabled: Boolean = enabled,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        AppOutlinedButton(
            text = "−${NBSP}$stepSeconds${NBSP}s",
            onClick = onMinus,
            enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = Sizes.transportButton),
        )
        Button(
            onClick = onSkip,
            enabled = skipEnabled,
            shape = RoundedCornerShape(Radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
            // Sizes, not gaps: the transport's centre key is wider than a step pill.
            modifier = Modifier
                .heightIn(min = Sizes.transportButton)
                .widthIn(min = 128.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = Elevation.none,
                pressedElevation = Elevation.none,
                focusedElevation = Elevation.none,
                hoveredElevation = Elevation.none,
                disabledElevation = Elevation.none,
            ),
        ) { Text("Skip", style = AppText.button) }
        AppOutlinedButton(
            text = "+${NBSP}$stepSeconds${NBSP}s",
            onClick = onPlus,
            enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = Sizes.transportButton),
        )
    }
}

/**
 * The rest ring: [remaining] of the rest still to go (1 → 0), drawn clockwise from twelve in
 * [color] over a translucent track of the same colour, with [content] centred. The sweep
 * animates linearly across each one-second tick, so the ring drains smoothly; [full] draws it
 * whole (rest reached).
 */
@Composable
fun RestRing(
    remaining: Float,
    color: Color,
    size: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    full: Boolean = false,
    content: @Composable () -> Unit,
) {
    val sweep by animateFloatAsState(
        targetValue = if (full) 1f else remaining.coerceIn(0f, 1f),
        animationSpec = tween(1_000, easing = LinearEasing),
        label = "restRing",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.toPx()
            val topLeft = Offset(w / 2f, w / 2f)
            val arc = Size(this.size.width - w, this.size.height - w)
            val line = Stroke(width = w, cap = StrokeCap.Round)
            drawArc(color.copy(alpha = 0.16f), -90f, 360f, false, topLeft, arc, style = line)
            if (sweep > 0f) drawArc(color, -90f, 360f * sweep, false, topLeft, arc, style = line)
        }
        content()
    }
}
