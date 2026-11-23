package com.example.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette — design-plan §1, "a black scoreboard".
 *
 * ONE scheme, dark only, every Material role set explicitly (see [Theme.kt]):
 * nothing falls through to the baseline purple/red that used to paint warm-up
 * badges, failure badges and every destructive button.
 *
 * Neutrals are food-tracker's verbatim (family resemblance). The accent is volt
 * and means **work done** — a ticked set, a record, a filled day. Targets you set
 * yourself are amber ([AppTertiary]). Nothing else is coloured.
 *
 * The `surfaceContainer*` ramp exists only for modal surfaces (sheets, menus,
 * dialogs) and pressed/focused states. Nothing on a page fills.
 */

// ---- Surfaces -------------------------------------------------------------

val AppBlack = Color(0xFF000000)
val AppSurfaceLow = Color(0xFF070707)
val AppSurface = Color(0xFF0C0C0B)
val AppSurfaceHigh = Color(0xFF141312)
val AppSurfaceHighest = Color(0xFF1C1A18)

/** Not `#FFFFFF`: pure white on pure black haloes, and this is read mid-set. */
val AppOnSurface = Color(0xFFEFE9E0)
val AppOnSurfaceVariant = Color(0xFFA79E92)
val AppOutline = Color(0xFF36312B)
val AppOutlineVariant = Color(0xFF201D1A)

// ---- Accent: Volt (decision 6) --------------------------------------------
// The one hue with high luminance on black that neither food (mint) nor the
// family's warning hues (amber, orange, red) occupy. The orange brand is retired.

val AppPrimary = Color(0xFFC5EA6A)
val AppOnPrimary = Color(0xFF1B2600)
val AppPrimaryContainer = Color(0xFF26330C)
val AppOnPrimaryContainer = Color(0xFFE2F7B0)

/** Deep volt — `inversePrimary`, and the heat ramp's midpoint. */
val AppPrimaryDeep = Color(0xFF5E7A1C)

// ---- Target (shared with food) --------------------------------------------

val AppTertiary = Color(0xFFE0A961)
val AppOnTertiary = Color(0xFF2A1A00)
val AppTertiaryContainer = Color(0xFF2D2310)
val AppOnTertiaryContainer = Color(0xFFF3DBA5)

// ---- Destructive ----------------------------------------------------------

val AppError = Color(0xFFFF6B5E)
val AppOnError = Color(0xFF3A0A06)
val AppErrorContainer = Color(0xFF4A120C)
val AppOnErrorContainer = Color(0xFFFFD9D4)

// ---- Custom tokens --------------------------------------------------------

/** Failure sets and negative deltas. Food's `StatusOver`. */
val Effort = Color(0xFFF08A4B)

/**
 * Completed set-row tint: primary at 8% on black, pre-composited so it is a
 * flat colour (S2-b). The one tinted row in the app — house rule 3's gym exception.
 */
val DoneRow = Color(0xFF101309)

// ---- Data palette: one heat ramp (training volume) ------------------------

val Heat0 = AppOutlineVariant      // no volume
val Heat25 = Color(0xFF3A4520)
val Heat50 = AppPrimaryDeep
val Heat75 = Color(0xFF91B23F)
val Heat100 = AppPrimary

/** The five ramp stops, low → high. */
val HeatRamp: List<Color> = listOf(Heat0, Heat25, Heat50, Heat75, Heat100)
