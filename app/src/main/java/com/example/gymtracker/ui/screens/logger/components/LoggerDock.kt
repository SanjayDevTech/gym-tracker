package com.example.gymtracker.ui.screens.logger.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Meter
import com.example.gymtracker.ui.components.atoms.RestTransport
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** The dock's top hairline: primary at low alpha, a step brighter than list rules. */
private const val DOCK_EDGE_ALPHA = 0.3f

/**
 * The logger's bottom dock — §3 `LoggerDock`, §4.3, §6.3.
 *
 * `REST` / `TARGET 1:30` eyebrows, the rest clock in [AppText.numberXL] (tap → Rest sheet), a
 * 2dp meter toward the target, and the [RestTransport]. Below target the meter is `tertiary`
 * (the goal colour); at target the meter and the clock turn `primary` and stay there.
 *
 * [compact] collapses everything to one 56dp line while the IME is open so the focused set row
 * is never hidden. [restElapsedMillis] is null while rest is cleared (no set yet, or Skip).
 *
 * The caller owns insets: this composable must be the *only* IME consumer on the screen. It
 * paints its background down through [insets] (nav bar, or keyboard) so scrolled content never
 * shows beneath it, and keeps its content above them. The top edge is a primary-tinted hairline,
 * so the dock doesn't read as one more list divider.
 */
@Composable
fun LoggerDock(
    restElapsedMillis: Long?,
    targetSeconds: Int,
    enabled: Boolean,
    compact: Boolean,
    onClockClick: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSkip: () -> Unit,
    insets: WindowInsets,
    modifier: Modifier = Modifier,
    stepSeconds: Int = 15,
) {
    val scheme = MaterialTheme.colorScheme
    val targetMillis = targetSeconds * 1000L
    val reached = restElapsedMillis != null && restElapsedMillis >= targetMillis
    val clock = restElapsedMillis?.let { formatClock(it) } ?: EM_DASH
    val clockColor by animateColorAsState(
        when {
            restElapsedMillis == null -> scheme.onSurfaceVariant
            reached -> scheme.primary
            else -> scheme.onSurface
        },
        animationSpec = tween(200),
        label = "restClock",
    )
    val meterColor = if (reached) scheme.primary else scheme.tertiary
    val fraction = restElapsedMillis?.let { (it.toFloat() / targetMillis).coerceIn(0f, 1f) } ?: 0f

    Column(modifier.fillMaxWidth().background(scheme.background).windowInsetsPadding(insets)) {
        AppDivider(color = scheme.primary.copy(alpha = DOCK_EDGE_ALPHA))
        if (compact) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.transportButton)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onClockClick)
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Eyebrow("Rest", modifier = Modifier.padding(end = Spacing.s))
                    Text(clock, style = AppText.numberM, color = clockColor)
                    Text(
                        " / ${formatClock(targetMillis)}",
                        style = AppText.bodyS,
                        color = scheme.tertiary,
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Meter(fraction = fraction, color = meterColor, height = 2.dp)
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter)
                    .padding(top = Spacing.m, bottom = Spacing.s),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Rest", modifier = Modifier.weight(1f))
                    Eyebrow("Target ${formatClock(targetMillis)}", color = scheme.tertiary)
                }
                Text(
                    clock,
                    style = AppText.numberXL,
                    color = clockColor,
                    modifier = Modifier.clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClickLabel = "Rest settings",
                        onClick = onClockClick,
                    ),
                )
                Spacer(Modifier.height(Spacing.s))
                Meter(fraction = fraction, color = meterColor, height = 2.dp)
                Spacer(Modifier.height(Spacing.s))
                RestTransport(
                    onMinus = onMinus,
                    onSkip = onSkip,
                    onPlus = onPlus,
                    stepSeconds = stepSeconds,
                    enabled = enabled,
                    skipEnabled = enabled && restElapsedMillis != null,
                )
            }
        }
    }
}
