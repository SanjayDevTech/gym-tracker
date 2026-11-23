package com.example.gymtracker.data

/**
 * Optional classification for a logged set. [Working] is the default and renders quietly;
 * warm-ups are muted and failure sets drawn in the Effort hue. UI names come from
 * `SetType.displayName` (DisplayNames.kt).
 */
enum class SetType {
    Warmup,
    Working,
    Failure,
}
