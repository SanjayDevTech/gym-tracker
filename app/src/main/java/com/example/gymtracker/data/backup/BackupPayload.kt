package com.example.gymtracker.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    // v3: sessions carry optional startedAtMillis. v2 files still import (field defaults).
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
)
