package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius

/** One ⋮ entry. [destructive] draws it in `error` (Discard workout, Delete …). */
data class MenuItem(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

/**
 * ⋮ with a `surfaceContainer` menu at [Radius.md] — the one overflow menu: logger bar and blocks,
 * Plan editor, Session detail, Exercise detail.
 */
@Composable
fun OverflowMenu(
    items: List<MenuItem>,
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

