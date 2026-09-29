package com.example.gymtracker.data.plan

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PlanImportPayload(
    val version: Int = 1,
    val name: String,
    val notes: String = "",
    val exercises: List<PlanImportExercise> = emptyList(),
)

@Serializable
data class PlanImportExercise(
    val name: String,
    val sets: Int = 3,
    val repsLow: Int = 8,
    val repsHigh: Int = 12,
    val notes: String = "",
    // Optional hints — used only to pre-fill the "Create custom" form when no name match.
    val movementType: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String? = null,
)

object PlanImportParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed class Result {
        data class Ok(val payload: PlanImportPayload) : Result()
        data class Error(val message: String) : Result()
    }

    fun parse(text: String): Result {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.Error("Paste a plan JSON to import.")
        return runCatching {
            json.decodeFromString(PlanImportPayload.serializer(), trimmed)
        }.fold(
            onSuccess = { payload ->
                when {
                    payload.name.isBlank() -> Result.Error("Plan has no name.")
                    payload.exercises.isEmpty() -> Result.Error("Plan has no exercises.")
                    else -> Result.Ok(payload)
                }
            },
            onFailure = { Result.Error("Invalid JSON: ${it.message ?: "unknown error"}") },
        )
    }
}
