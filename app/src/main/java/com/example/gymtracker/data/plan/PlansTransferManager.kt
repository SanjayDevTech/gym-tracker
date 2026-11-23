package com.example.gymtracker.data.plan

import android.content.Context
import android.net.Uri
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Exports every plan to a single JSON bundle and imports one back. Uses the name-based
 * [PlanImportPayload] shape (no local DB ids), so a bundle is portable across installs and
 * interchangeable with a single-plan share file.
 *
 * Import is **upsert by plan name** (case-insensitive): a plan whose name already exists is
 * updated in place — kept at the same row id so any logged sessions stay linked to it — while new
 * names are added. Exercises are resolved by name among live (non-deleted) exercises; an unknown
 * name is recreated as a custom exercise from the payload's hint fields. Sessions and preferences are untouched (that's what the
 * full Backup is for).
 */
class PlansTransferManager(
    private val context: Context,
    private val repository: GymRepository,
) {
    private val exportJson = Json { prettyPrint = true; encodeDefaults = true }

    data class ImportResult(
        val plansAdded: Int,
        val plansUpdated: Int,
        val exercisesCreated: Int,
    ) {
        val plansTotal: Int get() = plansAdded + plansUpdated
    }

    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val (text, count) = buildBundleJson()
            context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open output stream")
            count
        }
    }

    suspend fun importFrom(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: error("Could not open input stream")
            applyBundle(text)
        }
    }

    private suspend fun buildBundleJson(): Pair<String, Int> {
        val plans = repository.observePlans().first()
        val payloads = plans.mapNotNull { p ->
            repository.observePlanWithExercises(p.id).first()?.let { PlanExport.toPayload(it) }
        }
        val bundle = PlansBundle(
            version = 1,
            exportedAtMillis = System.currentTimeMillis(),
            plans = payloads,
        )
        return exportJson.encodeToString(PlansBundle.serializer(), bundle) to payloads.size
    }

    private suspend fun applyBundle(text: String): ImportResult {
        val bundle = when (val r = PlansBundleParser.parse(text)) {
            is PlansBundleParser.Result.Error -> error(r.message)
            is PlansBundleParser.Result.Ok -> r.bundle
        }
        var added = 0
        var updated = 0
        var created = 0

        repository.withTransaction {
            for (plan in bundle.plans) {
                val planName = plan.name.trim()
                if (planName.isBlank()) continue

                // Resolve each exercise to a row id, creating a custom for unknown names.
                val plannedTemplates = mutableListOf<PlannedExerciseEntity>()
                var sortIndex = 0
                for (src in plan.exercises) {
                    val exName = src.name.trim()
                    if (exName.isBlank()) continue
                    // Live rows only: a plan never maps onto a deleted exercise.
                    val existing = repository.findActiveExerciseByName(exName)
                    val exerciseId = if (existing != null) {
                        existing.id
                    } else {
                        created++
                        repository.insertExercise(
                            ExerciseEntity(
                                id = 0,
                                clientUuid = UUID.randomUUID().toString(),
                                name = exName,
                                movementType = parseEnum(src.movementType, MovementType.Compound),
                                primaryMuscles = src.primaryMuscles.mapNotNull { parseEnumOrNull<Muscle>(it) },
                                secondaryMuscles = src.secondaryMuscles.mapNotNull { parseEnumOrNull<Muscle>(it) },
                                equipment = parseEnum(src.equipment, Equipment.Other),
                                cues = "",
                                isCustom = true,
                                isDeleted = false,
                            )
                        )
                    }
                    val lo = src.repsLow.coerceAtLeast(1)
                    plannedTemplates += PlannedExerciseEntity(
                        planId = 0, // filled in once the plan id is known
                        exerciseId = exerciseId,
                        sortIndex = sortIndex++,
                        targetSets = src.sets.coerceAtLeast(1),
                        targetRepsLow = lo,
                        targetRepsHigh = src.repsHigh.coerceAtLeast(lo),
                        notes = src.notes,
                    )
                }

                val existingPlan = repository.findPlanByName(planName)
                if (existingPlan != null) {
                    repository.updatePlan(existingPlan.copy(name = planName, notes = plan.notes))
                    repository.replacePlanned(
                        existingPlan.id,
                        plannedTemplates.map { it.copy(planId = existingPlan.id) },
                    )
                    updated++
                } else {
                    val planId = repository.insertPlan(
                        PlanEntity(
                            clientUuid = UUID.randomUUID().toString(),
                            name = planName,
                            notes = plan.notes,
                            createdAtMillis = System.currentTimeMillis(),
                        )
                    )
                    if (plannedTemplates.isNotEmpty()) {
                        repository.insertPlannedAll(plannedTemplates.map { it.copy(planId = planId) })
                    }
                    added++
                }
            }
        }
        return ImportResult(plansAdded = added, plansUpdated = updated, exercisesCreated = created)
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private inline fun <reified T : Enum<T>> parseEnumOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value) }.getOrNull()

    companion object {
        fun suggestedFileName(): String = "gym-plans-${System.currentTimeMillis()}.json"
    }
}
