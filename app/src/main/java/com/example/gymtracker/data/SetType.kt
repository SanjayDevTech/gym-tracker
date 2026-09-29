package com.example.gymtracker.data

/**
 * Optional classification for a logged set. [Working] is the default and renders quietly;
 * the others get a subtle accent so warm-ups and failure sets stand out at a glance.
 */
enum class SetType {
    Warmup,
    Working,
    Failure;

    /** Full label used in menus. */
    val label: String
        get() = when (this) {
            Warmup -> "Warm-up set"
            Working -> "Working set"
            Failure -> "Failure set"
        }

    /** Compact tag shown on the set row. Working stays blank to keep the common case quiet. */
    val shortLabel: String
        get() = when (this) {
            Warmup -> "W"
            Working -> ""
            Failure -> "F"
        }
}
