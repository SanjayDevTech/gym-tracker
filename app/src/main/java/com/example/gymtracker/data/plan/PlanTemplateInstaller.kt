package com.example.gymtracker.data.plan

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.domain.ExerciseNameRule
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.UUID

/**
 * Copies a [TemplateOption]'s plans in as ordinary plans, in one transaction. Unlike the plans
 * bundle import (which upserts by name), this never touches an existing plan: a clashing name
 * gets a number ("Push A 2"). Exercises match live library rows by name; a built-in the user has
 * deleted is recreated as a custom copy keeping its muscles and equipment, as plan import does.
 */
class PlanTemplateInstaller(private val repository: GymRepository) {

    /** Returns the new plans' ids, in template order. */
    suspend fun add(option: TemplateOption): List<Long> {
        val taken = repository.observePlans().first().mapTo(mutableSetOf()) { nameKey(it.name) }
        val allExercises = repository.observeAllExercisesIncludingDeleted().first()
        val live = allExercises.filter { !it.isDeleted }.toMutableList()
        // Train lists plans by creation time; stagger by 1 ms so they keep template order.
        val now = System.currentTimeMillis()
        return repository.withTransaction {
            option.plans.mapIndexed { index, plan ->
                val name = uniqueName(plan.name, taken)
                taken += nameKey(name)
                val planId = repository.insertPlan(
                    PlanEntity(
                        clientUuid = UUID.randomUUID().toString(),
                        name = name,
                        notes = plan.notes,
                        createdAtMillis = now + index,
                    )
                )
                val rows = plan.exercises.mapIndexed { sortIndex, src ->
                    val exercise = ExerciseNameRule.findLiveMatch(src.name, live)
                        ?: recreate(src.name, allExercises).also { live += it }
                    PlannedExerciseEntity(
                        planId = planId,
                        exerciseId = exercise.id,
                        sortIndex = sortIndex,
                        targetSets = src.sets,
                        targetRepsLow = src.repsLow,
                        targetRepsHigh = src.repsHigh,
                        notes = src.notes,
                    )
                }
                repository.insertPlannedAll(rows)
                planId
            }
        }
    }

    /** A custom copy of the deleted built-in named [name]. */
    private suspend fun recreate(name: String, all: List<ExerciseEntity>): ExerciseEntity {
        val key = ExerciseNameRule.normalizedName(name)
        val source = all.firstOrNull { ExerciseNameRule.normalizedName(it.name) == key }
        val row = ExerciseEntity(
            clientUuid = UUID.randomUUID().toString(),
            name = source?.name ?: name,
            movementType = source?.movementType ?: MovementType.Compound,
            primaryMuscles = source?.primaryMuscles.orEmpty(),
            secondaryMuscles = source?.secondaryMuscles.orEmpty(),
            equipment = source?.equipment ?: Equipment.Other,
            cues = source?.cues.orEmpty(),
            isCustom = true,
            isDeleted = false,
        )
        return row.copy(id = repository.insertExercise(row))
    }

    companion object {
        private fun nameKey(name: String) = name.trim().lowercase(Locale.ROOT)

        /** [name], or "[name] 2", "[name] 3"… — the first whose key isn't in [taken]. */
        fun uniqueName(name: String, taken: Set<String>): String {
            if (nameKey(name) !in taken) return name
            var n = 2
            while (nameKey("$name $n") in taken) n++
            return "$name $n"
        }
    }
}
