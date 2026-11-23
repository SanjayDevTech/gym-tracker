package com.example.gymtracker.ui.screens.logger.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Meter
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * The logger's top bar — §3 `LoggerTopBar` (S2-b).
 *
 * `↓` minimise (leaving does not end the session, issue 4) · the elapsed clock centred in
 * [AppText.numberL] over a `PLAN · 7/18` eyebrow · an outlined `Finish` pill · ⋮. Under it a
 * 2dp meter of sets done over total. Finish is a once-a-session action, so it lives up here,
 * away from the thumb that ticks sets (house rule 11).
 */
@Composable
fun LoggerTopBar(
    elapsed: String,
    eyebrow: String,
    progress: Float,
    finishEnabled: Boolean,
    onMinimise: () -> Unit,
    onFinish: () -> Unit,
    menu: List<LoggerMenuItem>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.background)
            .statusBarsPadding(),
    ) {
        // A size: the bar is a two-line title plus a 48dp button row.
        Box(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            IconButton(onClick = onMinimise, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Minimise workout",
                    tint = scheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(elapsed, style = AppText.numberL, color = scheme.onSurface, maxLines = 1)
                Eyebrow(eyebrow, modifier = Modifier.widthIn(max = 160.dp))
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                AppOutlinedButton(text = "Finish", onClick = onFinish, enabled = finishEnabled)
                OverflowMenu(menu)
            }
        }
        Meter(
            fraction = progress,
            height = 2.dp,
            modifier = Modifier.padding(horizontal = Spacing.gutter),
        )
        Spacer(Modifier.height(Spacing.s))
    }
}

/** One ⋮ entry. [destructive] draws it in `error` (Discard workout). */
data class LoggerMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

/** ⋮ with a `surfaceContainer` menu at [Radius.md] — shared by the bar and each block. */
@Composable
fun OverflowMenu(
    items: List<LoggerMenuItem>,
    modifier: Modifier = Modifier,
    contentDescription: String = "More options",
) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = contentDescription, tint = scheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(Radius.md),
            containerColor = scheme.surfaceContainer,
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item.label,
                            style = AppText.body,
                            color = when {
                                !item.enabled -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                                item.destructive -> scheme.error
                                else -> scheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    enabled = item.enabled,
                    onClick = {
                        open = false
                        item.onClick()
                    },
                )
            }
        }
    }
}

