package com.example.gymtracker.data

/**
 * Human names for the enums the UI shows: names, not enums. `FrontDelts` →
 * "Front delts", `UpperBack` → "Upper back". Every
 * enum that reaches a screen goes through one of these, never `.name`.
 */

/** `UpperBack` → "Upper back": split on case changes, sentence-case the rest. */
private fun sentenceCase(enumName: String): String =
    enumName.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }

val Muscle.displayName: String
    get() = sentenceCase(name)

val Equipment.displayName: String
    get() = sentenceCase(name)

val MovementType.displayName: String
    get() = sentenceCase(name)

val SetType.displayName: String
    get() = when (this) {
        SetType.Warmup -> "Warm-up"
        SetType.Working -> "Working"
        SetType.Failure -> "Failure"
    }
