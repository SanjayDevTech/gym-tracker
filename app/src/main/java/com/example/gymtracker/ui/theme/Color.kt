package com.example.gymtracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB689),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF723600),
    onPrimaryContainer = Color(0xFFFFDBC0),
    secondary = Color(0xFFD7C2B2),
    onSecondary = Color(0xFF3C2D22),
    secondaryContainer = Color(0xFF544337),
    onSecondaryContainer = Color(0xFFF5DECF),
    tertiary = Color(0xFFBACAA5),
    onTertiary = Color(0xFF263417),
    background = Color.Black,
    onBackground = Color(0xFFECE0D5),
    surface = Color.Black,
    onSurface = Color(0xFFECE0D5),
    surfaceVariant = Color(0xFF52443B),
    onSurfaceVariant = Color(0xFFD6C2B5),
    outline = Color(0xFF9F8D80),
    outlineVariant = Color(0xFF52443B),
)

/**
 * Dark mode is always AMOLED: pure-black background/surface, near-black container tints
 * so cards still register against the void.
 */
fun ColorScheme.withAmoledSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF181410),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0805),
    surfaceContainer = Color(0xFF120F0B),
    surfaceContainerHigh = Color(0xFF1A1612),
    surfaceContainerHighest = Color(0xFF221C17),
)
