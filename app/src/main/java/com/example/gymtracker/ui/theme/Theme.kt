package com.example.gymtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app theme — Foodprint's, so the two apps read as one family: light and dark, with
 * the choice made in You → Appearance.
 *
 * Material slots carry the roles stock components read; [AppTheme.extras]
 * carries what Material has no slot for — the card fill and the hero gradient.
 */
private val LightColors = lightColorScheme(
    primary = LightInk,
    onPrimary = LightCard,
    primaryContainer = LightSurfaceHigh,
    onPrimaryContainer = LightInk,
    secondary = LightPositive,
    onSecondary = LightCard,
    secondaryContainer = LightPositiveContainer,
    onSecondaryContainer = Color(0xFF0B4A28),
    tertiary = LightAccent,
    onTertiary = LightCard,
    tertiaryContainer = LightAccentContainer,
    onTertiaryContainer = Color(0xFF6B2410),
    error = LightError,
    onError = LightCard,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,

    background = LightCanvas,
    onBackground = LightInk,
    surface = LightCanvas,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightInkSoft,
    surfaceTint = Color.Transparent,

    surfaceBright = LightCard,
    surfaceDim = LightSurfaceHighest,
    surfaceContainerLowest = LightCard,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    inverseSurface = LightInk,
    inverseOnSurface = LightCard,
    inversePrimary = DarkInk,
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = DarkInk,
    onPrimary = DarkCanvas,
    primaryContainer = DarkSurfaceHighest,
    onPrimaryContainer = DarkInk,
    secondary = DarkPositive,
    onSecondary = Color(0xFF052E14),
    secondaryContainer = DarkPositiveContainer,
    onSecondaryContainer = Color(0xFFBDF5CF),
    tertiary = DarkAccent,
    onTertiary = Color(0xFF2A0E04),
    tertiaryContainer = DarkAccentContainer,
    onTertiaryContainer = Color(0xFFFFD6C7),
    error = DarkError,
    onError = Color(0xFF3A0A06),
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,

    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkCanvas,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkInkSoft,
    surfaceTint = Color.Transparent,

    surfaceBright = DarkSurfaceHighest,
    surfaceDim = DarkCanvas,
    surfaceContainerLowest = DarkCard,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    inverseSurface = DarkInk,
    inverseOnSurface = DarkCanvas,
    inversePrimary = LightInk,
    scrim = Color.Black,
)

/**
 * What Material has no slot for.
 *
 * @param card the fill of a card on the page canvas.
 * @param cardInSheet the fill of a card inside a (white) bottom sheet, where
 *        [card] would vanish into the sheet itself.
 * @param mesh base colour first, then the four blob colours of the hero gradient.
 * @param heat the training-volume ramp, none → most: muscle map, calendar, rank bars.
 * @param doneRow the tint behind a completed set in the logger.
 */
@Immutable
data class AppExtras(
    val isDark: Boolean,
    val card: Color,
    val cardInSheet: Color,
    val mesh: List<Color>,
    val heat: List<Color>,
    val doneRow: Color,
)

private val LightExtras = AppExtras(
    isDark = false,
    card = LightCard,
    cardInSheet = LightCanvas,
    mesh = listOf(
        Color(0xFFF7F3FF), // base
        Color(0xFFDCD3FF), // lavender
        Color(0xFFFFD3EA), // pink
        Color(0xFFFFDCC4), // peach
        Color(0xFFD3F2E5), // mint
    ),
    heat = listOf(
        Color(0xFFE9E9EE), // untrained
        Color(0xFFFFD9C7),
        Color(0xFFFFB08F),
        Color(0xFFF98552),
        Color(0xFFE0502A),
    ),
    doneRow = Color(0xFFE8F7EE),
)

private val DarkExtras = AppExtras(
    isDark = true,
    card = DarkCard,
    cardInSheet = Color(0xFF1F1F24),
    mesh = listOf(
        Color(0xFF16141D),
        Color(0xFF3A2D66),
        Color(0xFF4A2142),
        Color(0xFF4D2A1A),
        Color(0xFF173A31),
    ),
    heat = listOf(
        Color(0xFF2A2A31),
        Color(0xFF4A2A1F),
        Color(0xFF7A3A22),
        Color(0xFFC9532C),
        Color(0xFFFF7A4D),
    ),
    doneRow = Color(0xFF10261A),
)

/** Text fields use `extraSmall`; menus and dialogs the larger steps. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.md),
    small = RoundedCornerShape(Radius.md),
    medium = RoundedCornerShape(Radius.lg),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.sheet),
)

private val LocalExtras = staticCompositionLocalOf { LightExtras }

object AppTheme {
    val extras: AppExtras
        @Composable @ReadOnlyComposable get() = LocalExtras.current
}

@Composable
fun GymTrackerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtras provides if (dark) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
        ) {
            // Opaque backdrop so screen transitions never reveal the bare window.
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
        }
    }
}
