package com.example.gymtracker.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Layout tokens — Foodprint's scale, so both apps share one rhythm. If a value isn't here, the
 * answer is one of these, not a new one. Raw `.dp` is for sizes, never for gaps.
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

    /** Card interior padding. */
    val cardPadding = 16.dp

    /** Gap between cards on a page. */
    val section = 12.dp
}

/** Soft, generous corners — the pastel-card look. */
object Radius {
    /** Badges, meter caps, status cells. */
    val sm = 10.dp

    /** Inner rows, stat tiles, list rows, inputs. */
    val md = 16.dp

    /** Cards. */
    val lg = 24.dp

    /** Bottom sheets — top corners only. */
    val sheet = 28.dp

    /** Chips, buttons, segmented controls, the bar. */
    val pill = 999.dp
}

/**
 * Size minimums. Used with `heightIn(min = …)`, never as fixed heights: the label grows with font
 * scale and the box must too.
 */
object Sizes {
    val button = 52.dp
    val iconButton = 48.dp
    val touchTarget = 48.dp

    /** Icons inside a control. */
    val icon = 20.dp

    /** Bottom list inset that keeps the last row clear of an extended FAB. */
    val fabClearance = 96.dp

    /** Logger rest transport's Skip button. */
    val transportButton = 56.dp
}

/** Flat by default; cards separate from the canvas by fill, not shadow. */
object Elevation {
    val none = 0.dp
    val hairline = 1.dp
}
