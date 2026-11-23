package com.example.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale — design-plan §1, food-tracker's `AppText` verbatim.
 *
 * System font (no bundled font, §8). Every numeric style carries `tnum` so
 * digits don't jitter as a clock ticks or a value animates. The canvas is quiet
 * and the numbers are loud: a weight read from a bench, 60cm away, at a glance.
 */

private const val TNUM = "tnum"

object AppText {
    /** A screen's one hero — rest clock, summary duration, est. 1RM. */
    val numberXXL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 64.sp, lineHeight = 60.sp, letterSpacing = (-2).sp,
        fontFeatureSettings = TNUM,
    )

    /** Section heroes — the week count. */
    val numberXL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = TNUM,
    )

    /**
     * Set-row values and stat rows. Deliberately larger than food uses for
     * inputs: a weight must be legible from arm's length.
     */
    val numberL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 32.sp, fontFeatureSettings = TNUM,
    )

    /** Dense grids, stat columns. */
    val numberM = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = TNUM,
    )

    /** Screen and sheet titles. */
    val titleL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp,
    )

    /** Exercise and plan names. */
    val titleM = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    )

    val body = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    )

    /** Units, captions, axes. Numeric, so it gets tnum too. */
    val bodyS = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM,
    )

    /** Buttons, selectors. */
    val label = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp,
    )

    /**
     * Section heads and column heads. Always rendered UPPERCASE — by the
     * component, not the caller. 1.2sp tracking: at 11sp anything less is
     * invisible, and the tracked micro-label is half the editorial voice.
     */
    val eyebrow = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp,
    )
}

/**
 * Material slots pointed at [AppText] so stock components inherit it. Reach for
 * [AppText] directly when a slot name would be a lie about intent.
 */
val AppTypography = Typography(
    displayLarge = AppText.numberXXL,
    displayMedium = AppText.numberL,
    displaySmall = AppText.numberM,
    headlineLarge = AppText.numberL,
    headlineMedium = AppText.numberM,
    headlineSmall = AppText.titleL,
    titleLarge = AppText.titleL,
    titleMedium = AppText.titleM,
    titleSmall = AppText.label,
    bodyLarge = AppText.body,
    bodyMedium = AppText.body,
    bodySmall = AppText.bodyS,
    labelLarge = AppText.label,
    labelMedium = AppText.label,
    labelSmall = AppText.eyebrow,
)
