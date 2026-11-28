package com.example.gymtracker.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    // v3 (additive, all optional): session startedAtMillis, set type, volume metric and weekly
    // target. v2 files still import — every v3 field has a default meaning "not in the file".
    val version: Int = 3,
    val exportedAtMillis: Long,
    val prefs: PrefsSnapshot,
    val customExercises: List<ExerciseSnapshot>,
    val plans: List<PlanSnapshot>,
    val sessions: List<SessionSnapshot>,
)

@Serializable
data class PrefsSnapshot(
    val haptics: Boolean,
    val weightUnit: String,
    val seedVersion: Int,
    // Legacy field from earlier exports; ignored on restore.
    val themeMode: String = "",
    // v3. Null when the file predates it — the current preference is then left alone.
    val volumeMetric: String? = null,
    val weeklyTargetSessions: Int? = null,
    // Null when the file predates them — the current preference is then left alone. `theme` is a
    // new name on purpose: `themeMode` above holds values from older exports that restore ignores.
    val theme: String? = null,
    val userName: String? = null,
    val defaultRestSeconds: Int? = null,
)

@Serializable
data class ExerciseSnapshot(
    val clientUuid: String,
    val name: String,
    val movementType: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val cues: String,
    val isDeleted: Boolean,
)

@Serializable
data class PlanSnapshot(
    val clientUuid: String,
    val name: String,
    val notes: String,
    val createdAtMillis: Long,
    val planned: List<PlannedSnapshot>,
    // Legacy field from v1 backups; kept for deserialization compatibility, ignored on restore.
    val isDeleted: Boolean = false,
)

@Serializable
data class PlannedSnapshot(
    val exerciseClientUuid: String,
    val sortIndex: Int,
    val targetSets: Int,
    val targetRepsLow: Int,
    val targetRepsHigh: Int,
    val notes: String,
)

@Serializable
data class SessionSnapshot(
    val clientUuid: String,
    val planClientUuid: String?,
    val dateEpochDay: Long,
    val loggedAtMillis: Long,
    val notes: String,
    val sets: List<SetSnapshot>,
    val startedAtMillis: Long? = null,
)

@Serializable
data class SetSnapshot(
    val exerciseClientUuid: String,
    val setIndex: Int,
    val repsDone: Int?,
    val weightKg: Float?,
    val isCompleted: Boolean,
    val rpe: Float? = null,
    // v3. Absent in v2 files, which restore as Working (the column default).
    val setType: String? = null,
)
