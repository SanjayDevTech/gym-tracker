package com.example.gymtracker.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Layout tokens — design-plan §1 spacing, radii, sizes and elevation.
 *
 * Replaces `Dimens`. If a value isn't here, the answer is one of these, not a
 * new one. Raw `.dp` is allowed only for sizes (icons, charts, row minimums),
 * never for gaps (house rule 5).
 */

/** `4 · 8 · 12 · 16 · 20 · 24 · 32`. Nothing else. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Screen gutter. */
    val gutter = 16.dp

    /** Gap between sections — the rule under each head does the separating. */
    val section = 24.dp
}

/** Four radii plus the pill. */
object Radius {
    /** Badges, status cells, the set badge. */
    val sm = 8.dp

    /** Input underlays, snackbar, menus. */
    val md = 14.dp

    /** Genuine cards only (expected: none on pages). */
    val lg = 20.dp

    /** Bottom sheets — top corners only. */
    val sheet = 24.dp

    /** FAB and primary buttons only. */
    val pill = 999.dp
}

/**
 * Size minimums. Used with `heightIn(min = …)` / `sizeIn(min…)`, never as fixed
 * heights (house rule 7): the label grows with font scale and the box must too.
 */
object Sizes {
    val button = 48.dp
    val iconButton = 48.dp
    val touchTarget = 48.dp

    /** Icons inside a control. */
    val icon = 24.dp

    /** Bottom list inset that keeps the last row clear of an extended FAB. */
    val fabClearance = 96.dp

    /** Logger rest transport's Skip button. */
    val transportButton = 56.dp
}

/**
 * Zero elevation anywhere. Structure is a 1dp `outlineVariant` hairline.
 * Kept as named tokens so the rule is greppable rather than folklore.
 */
object Elevation {
    val none = 0.dp
    val hairline = 1.dp
}
