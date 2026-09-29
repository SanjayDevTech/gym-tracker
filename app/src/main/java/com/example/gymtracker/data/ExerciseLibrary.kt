package com.example.gymtracker.data

/** 18 anatomical regions the body diagram shades. */
enum class Muscle {
    Chest,
    FrontDelts,
    SideDelts,
    RearDelts,
    Biceps,
    Triceps,
    Forearms,
    UpperBack,
    Lats,
    Traps,
    LowerBack,
    Abs,
    Obliques,
    Glutes,
    Quads,
    Hamstrings,
    Calves,
    Adductors,
}

enum class MovementType { Compound, Isolation, Cardio, Mobility }

/** Per-movement-type label for the per-set count unit (what targetReps* counts). */
val MovementType.repUnit: String
    get() = when (this) {
        MovementType.Compound, MovementType.Isolation -> "reps"
        MovementType.Cardio -> "min"
        MovementType.Mobility -> "sec"
    }

/** Short label for the per-set count unit (used in tight UI like the logger's text-field label). */
val MovementType.repUnitShort: String
    get() = when (this) {
        MovementType.Compound, MovementType.Isolation -> "reps"
        MovementType.Cardio -> "min"
        MovementType.Mobility -> "sec"
    }

/**
 * Format a planned target as a human subtitle:
 *   Compound/Isolation → "3 × 8–12 reps"
 *   Cardio            → "3 × 8–10 min" (or "30 min" when sets == 1 — intervals vs. one bout)
 *   Mobility          → "3 × 30 sec"
 * Collapses degenerate ranges (low == high) to a single number.
 */
fun formatTarget(sets: Int, low: Int, high: Int, type: MovementType): String {
    val range = if (low == high) "$low" else "$low–$high"
    val setsPrefix = if (sets == 1 && type == MovementType.Cardio) "" else "$sets × "
    return "$setsPrefix$range ${type.repUnit}"
}

enum class Equipment {
    Barbell,
    Dumbbell,
    Machine,
    Cable,
    Bodyweight,
    Kettlebell,
    Band,
    Other,
}
