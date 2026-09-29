package com.example.gymtracker.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = DarkColors.withAmoledSurfaces(),
        typography = AppTypography,
    ) {
        // Opaque backdrop so screen transitions (scale/fade) never reveal the bare window —
        // the area behind an animating destination is always the theme background.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
