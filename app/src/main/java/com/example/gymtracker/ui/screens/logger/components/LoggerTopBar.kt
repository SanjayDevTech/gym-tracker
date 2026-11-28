package com.example.gymtracker.ui.screens.logger.components

import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.components.atoms.pressable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Lucide
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.OverflowMenu
import com.example.gymtracker.ui.components.atoms.RoundIconButton
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.components.atoms.Meter
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/**
 * The logger's top bar — §3 `LoggerTopBar` (S2-b).
 *
 * A round `↓` minimise (leaving does not end the session, issue 4) · centred, the workout title
 * (one line, ellipsised) over a muted meta line `3 of 12 sets · Today` whose date part turns
 * `tertiary` when the session is not today (the clocks live in the bottom dock) · a
 * pill (`Discard`: soft, in `error`, until a set is ticked; then `Finish`: ink) · a round ⋮. Under
 * it a meter of sets done over total. Finish is a once-a-session action, so it lives up here, away
 * from the thumb that ticks sets (house rule 11).
 */
@Composable
fun LoggerTopBar(
    title: String,
    meta: String?,
    dateLabel: String,
    dateHighlighted: Boolean,
    progress: Float,
    onMinimise: () -> Unit,
    menu: List<MenuItem>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            // Claim taps on empty areas so they don't fall through to the list (see LoggerDock).
            .pointerInput(Unit) {}
            .background(scheme.background)
            .statusBarsPadding(),
    ) {
        // [⌄] title + progress, left-aligned, and the menu. Finish / Discard live in the bottom
        // dock, where one thumb reaches them.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = Spacing.s, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(icon = Lucide.ChevronDown, contentDescription = "Minimise workout", onClick = onMinimise)
            Spacer(Modifier.width(Spacing.xs))
            Column(Modifier.weight(1f)) {
                Text(title, style = AppText.titleL, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MetaLine(meta, dateLabel, if (dateHighlighted) scheme.tertiary else scheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(Spacing.s))
            // The menu's own 48dp button over a round card disc, so it reads as RoundIconButton.
            Box(Modifier.size(Sizes.touchTarget), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(cardColor()))
                OverflowMenu(menu)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Meter(
            fraction = progress,
            color = scheme.secondary,
            modifier = Modifier.padding(horizontal = Spacing.gutter),
        )
        Spacer(Modifier.height(Spacing.m))
    }
}

/** `3 of 12 sets · Today` — muted, smaller than the title; the date part in [dateColor]. */
@Composable
private fun MetaLine(meta: String?, dateLabel: String, dateColor: Color) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row {
        if (meta != null) {
            Text(
                "$meta · ",
                style = AppText.bodyS,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(dateLabel, style = AppText.bodyS, color = dateColor, maxLines = 1)
    }
}
