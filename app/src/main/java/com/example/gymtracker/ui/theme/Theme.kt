package com.example.gymtracker.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app theme — design-plan §1, phase 1.
 *
 * One scheme, always dark, no arguments. `MaterialExpressiveTheme` is gone (its
 * motion scheme — spring overshoot, shape-morph — is banned by §1's motion
 * table), and so is `withAmoledSurfaces()`: the scheme below is OLED from the
 * start, with every role set so no baseline colour leaks through.
 */
private val AppColors = darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    inversePrimary = AppPrimaryDeep,
    secondary = AppPrimary,
    onSecondary = AppOnPrimary,
    secondaryContainer = AppPrimaryContainer,
    onSecondaryContainer = AppOnPrimaryContainer,
    tertiary = AppTertiary,
    onTertiary = AppOnTertiary,
    tertiaryContainer = AppTertiaryContainer,
    onTertiaryContainer = AppOnTertiaryContainer,
    error = AppError,
    onError = AppOnError,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppOnErrorContainer,

    background = AppBlack,
    onBackground = AppOnSurface,
    surface = AppBlack,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppOnSurfaceVariant,
    surfaceTint = AppPrimary,

    surfaceBright = AppSurfaceHighest,
    surfaceDim = AppBlack,
    surfaceContainerLowest = AppBlack,
    surfaceContainerLow = AppSurfaceLow,
    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceHigh,
    surfaceContainerHighest = AppSurfaceHighest,

    outline = AppOutline,
    outlineVariant = AppOutlineVariant,

    inverseSurface = AppOnSurface,
    inverseOnSurface = AppBlack,

    scrim = AppBlack,
)

@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
    ) {
        // Opaque backdrop so screen transitions never reveal the bare window —
        // the area behind an animating destination is always the theme background.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
