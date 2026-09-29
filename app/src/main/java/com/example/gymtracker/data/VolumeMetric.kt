package com.example.gymtracker.data

/**
 * How training volume is measured for the muscle map and analytics totals.
 *
 * - [Sets] counts each completed working set once (1.0 primary / 0.5 secondary), regardless of
 *   load. Bodyweight work (pull-ups, planks, hanging leg raises) counts fully. This is the
 *   hypertrophy-standard "hard sets per muscle" metric and the sensible default.
 * - [Tonnage] sums reps × weight (kg·reps). Load-weighted, but structurally blind to bodyweight
 *   sets — useful for tracking load progression on weighted lifts.
 */
enum class VolumeMetric(val label: String) {
    Sets("Sets"),
    Tonnage("Tonnage"),
}
