package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.theme.Radius

/**
 * Snackbar host — Foodprint's: an ink slab at [Radius.md] with the action in the accent.
 * Snackbars are the app's only transient message — no Toasts.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(Radius.md),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.tertiary,
            actionContentColor = MaterialTheme.colorScheme.tertiary,
            dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}
