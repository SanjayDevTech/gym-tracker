package com.example.gymtracker.data.plan

import com.example.gymtracker.data.db.PlanWithExercises
import kotlinx.serialization.json.Json

/** Serializes a plan to the same JSON shape the importer accepts, so a plan can round-trip. */
object PlanExport {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    /** Builds the portable, name-based payload for one plan. Shared by single-plan share and the
     *  "export all plans" bundle so both produce interchangeable JSON. */
    fun toPayload(plan: PlanWithExercises): PlanImportPayload = PlanImportPayload(
        version = 1,
        name = plan.plan.name,
        notes = plan.plan.notes,
        exercises = plan.planned
            .sortedBy { it.planned.sortIndex }
            .map { p ->
                PlanImportExercise(
                    name = p.exercise.name,
                    sets = p.planned.targetSets,
                    repsLow = p.planned.targetRepsLow,
                    repsHigh = p.planned.targetRepsHigh,
                    notes = p.planned.notes,
                    movementType = p.exercise.movementType.name,
                    primaryMuscles = p.exercise.primaryMuscles.map { it.name },
                    secondaryMuscles = p.exercise.secondaryMuscles.map { it.name },
                    equipment = p.exercise.equipment.name,
                )
            },
    )

    fun toJson(plan: PlanWithExercises): String =
        json.encodeToString(PlanImportPayload.serializer(), toPayload(plan))

    fun suggestedFileName(planName: String): String {
        val safe = planName.trim()
            .replace(Regex("[^A-Za-z0-9-_ ]"), "")
            .replace(' ', '-')
            .take(40)
            .ifBlank { "plan" }
        return "$safe.json"
    }
}
