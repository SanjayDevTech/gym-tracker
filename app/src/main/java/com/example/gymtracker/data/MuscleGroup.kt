package com.example.gymtracker.data

/**
 * Coarse muscle grouping used for at-a-glance tags on plan/session cards. The body diagram
 * still works at the 17-region [Muscle] granularity; this is purely for compact labels.
 */
enum class MuscleGroup(val label: String) {
    Chest("Chest"),
    Back("Back"),
    Shoulders("Shoulders"),
    Arms("Arms"),
    Legs("Legs"),
    Core("Core"),
}

val Muscle.group: MuscleGroup
    get() = when (this) {
        Muscle.Chest -> MuscleGroup.Chest
        Muscle.UpperBack, Muscle.Lats, Muscle.Traps, Muscle.LowerBack -> MuscleGroup.Back
        Muscle.FrontDelts, Muscle.SideDelts, Muscle.RearDelts -> MuscleGroup.Shoulders
        Muscle.Biceps, Muscle.Triceps, Muscle.Forearms -> MuscleGroup.Arms
        Muscle.Glutes, Muscle.Quads, Muscle.Hamstrings, Muscle.Calves, Muscle.Adductors -> MuscleGroup.Legs
        Muscle.Abs, Muscle.Obliques -> MuscleGroup.Core
    }

/**
 * Distinct muscle groups covered by a set of muscles, ordered by how many muscles map into each
 * group (most-emphasized first) so the leading tags describe what a plan/session mostly trains.
 */
fun Iterable<Muscle>.toMuscleGroups(): List<MuscleGroup> =
    groupingBy { it.group }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<MuscleGroup, Int>> { it.value }.thenBy { it.key.ordinal })
        .map { it.key }
