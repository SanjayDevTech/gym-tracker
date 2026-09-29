package com.example.gymtracker.data.db

import kotlinx.serialization.Serializable

@Serializable
data class SeedPayload(
    val seedVersion: Int,
    val exercises: List<SeedExercise>,
)

@Serializable
data class SeedExercise(
    /**
     * Stable identifier for this seed entry. Hardcoded UUIDv5(name) — same across builds and
     * across reinstalls. Enables backup/restore on a fresh install to resolve session set rows
     * back to the canonical built-in exercise by UUID rather than relying on name-matching.
     */
    val clientUuid: String,
    val name: String,
    val movementType: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val cues: String = "",
)
