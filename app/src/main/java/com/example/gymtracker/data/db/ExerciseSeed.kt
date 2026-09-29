package com.example.gymtracker.data.db

import android.content.Context
import com.example.gymtracker.R
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.PreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

object ExerciseSeed {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Read seed JSON and upsert canonical entries keyed by name. **Built-in clientUuid is
     * sourced from the JSON** (deterministic UUIDv5(name)) so it's stable across builds and
     * across reinstalls — this is what makes cross-install backup/restore work without needing
     * to ship the built-in catalog in the backup file.
     *
     * Existing rows have their clientUuid migrated to the canonical seed UUID if they differ
     * (this fixes old installs whose seed rows were created with random UUIDs). isCustom and
     * isDeleted are always preserved. Custom rows (isCustom = true) are never touched.
     *
     * No-op if the stored seedVersion is at or past payload.seedVersion.
     */
    suspend fun ensureSeeded(
        context: Context,
        repository: com.example.gymtracker.data.GymRepository,
        prefs: PreferencesStore,
    ) {
        val raw = context.resources.openRawResource(R.raw.exercises_seed)
            .bufferedReader().use { it.readText() }
        val payload = json.decodeFromString(SeedPayload.serializer(), raw)
        val stored = prefs.seedVersion.first()
        // Re-seed when the table is empty (covers destructive DB migrations where DataStore
        // survives but exercises don't), otherwise gate on version.
        val empty = repository.countExercises() == 0
        if (!empty && stored >= payload.seedVersion) return

        for (seed in payload.exercises) {
            val existing = repository.findExerciseByName(seed.name)
            // Skip if the row is user-custom — seed never overwrites custom data.
            if (existing != null && existing.isCustom) continue
            val toUpsert = ExerciseEntity(
                id = existing?.id ?: 0,
                clientUuid = seed.clientUuid,
                name = seed.name,
                movementType = seed.movementType.toMovementType(),
                primaryMuscles = seed.primaryMuscles.toMuscles(),
                secondaryMuscles = seed.secondaryMuscles.toMuscles(),
                equipment = seed.equipment.toEquipment(),
                cues = seed.cues,
                isCustom = false,
                isDeleted = existing?.isDeleted ?: false,
            )
            // Existing rows are UPDATEd in place. An INSERT with OnConflict.REPLACE would
            // delete-then-reinsert the row, and the delete is rejected by the RESTRICT foreign
            // keys on logged_sets / planned_exercises whenever the exercise has been logged or
            // planned — the crash a naive re-seed produces. Only brand-new rows are inserted.
            if (existing != null) {
                repository.updateExercise(toUpsert)
            } else {
                repository.insertExercise(toUpsert)
            }
        }
        prefs.setSeedVersion(payload.seedVersion)
    }

    private fun String.toMovementType(): MovementType =
        runCatching { MovementType.valueOf(this) }.getOrDefault(MovementType.Compound)

    private fun String.toEquipment(): Equipment =
        runCatching { Equipment.valueOf(this) }.getOrDefault(Equipment.Other)

    private fun List<String>.toMuscles(): List<Muscle> =
        mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() }
}
