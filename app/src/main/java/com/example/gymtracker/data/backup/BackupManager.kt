package com.example.gymtracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.AppDatabase
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: GymRepository,
    private val prefs: PreferencesStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = snapshotAll()
            val text = json.encodeToString(BackupPayload.serializer(), payload)
            context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open output stream for backup uri")
        }
    }

    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: error("Could not open input stream for backup uri")
            val payload = json.decodeFromString(BackupPayload.serializer(), text)
            applyPayload(payload)
        }
    }

    private suspend fun snapshotAll(): BackupPayload {
        val exercises = repository.observeExercises().first() +
            repository.observeDeletedExercises().first()
        val byId = exercises.associateBy { it.id }
        val plans = repository.observePlans().first()
        val planSnapshots = plans.map { p ->
            val withExercises = repository.observePlanWithExercises(p.id).first()
            PlanSnapshot(
                clientUuid = p.clientUuid,
                name = p.name,
                notes = p.notes,
                createdAtMillis = p.createdAtMillis,
                planned = withExercises?.planned
                    ?.sortedBy { it.planned.sortIndex }
                    ?.map { pe ->
                        PlannedSnapshot(
                            exerciseClientUuid = byId[pe.planned.exerciseId]?.clientUuid ?: pe.exercise.clientUuid,
                            sortIndex = pe.planned.sortIndex,
                            targetSets = pe.planned.targetSets,
                            targetRepsLow = pe.planned.targetRepsLow,
                            targetRepsHigh = pe.planned.targetRepsHigh,
                            notes = pe.planned.notes,
                        )
                    }
                    .orEmpty(),
            )
        }

        val sessionsAll = repository.observeSessions().first()
        val planByIdLocal = plans.associateBy { it.id }
        val sessionSnapshots = sessionsAll.map { s ->
            val withSets = repository.observeSessionWithSets(s.id).first()
            val planClientUuid = s.planId?.let { planByIdLocal[it]?.clientUuid }
            SessionSnapshot(
                clientUuid = s.clientUuid,
                planClientUuid = planClientUuid,
                dateEpochDay = s.dateEpochDay,
                loggedAtMillis = s.loggedAtMillis,
                notes = s.notes,
                startedAtMillis = s.startedAtMillis,
                sets = withSets?.sets?.sortedBy { it.setIndex }?.map { st ->
                    SetSnapshot(
                        exerciseClientUuid = byId[st.exerciseId]?.clientUuid ?: "",
                        setIndex = st.setIndex,
                        repsDone = st.repsDone,
                        weightKg = st.weightKg,
                        isCompleted = st.isCompleted,
                        rpe = st.rpe,
                        setType = st.setType.name,
                    )
                } ?: emptyList(),
            )
        }

        val customs = exercises.filter { it.isCustom }.map {
            ExerciseSnapshot(
                clientUuid = it.clientUuid,
                name = it.name,
                movementType = it.movementType.name,
                primaryMuscles = it.primaryMuscles.map { m -> m.name },
                secondaryMuscles = it.secondaryMuscles.map { m -> m.name },
                equipment = it.equipment.name,
                cues = it.cues,
                isDeleted = it.isDeleted,
            )
        }

        return BackupPayload(
            version = 3,
            exportedAtMillis = System.currentTimeMillis(),
            prefs = PrefsSnapshot(
                haptics = prefs.hapticsEnabled.first(),
                weightUnit = prefs.weightUnit.first().name,
                seedVersion = prefs.seedVersion.first(),
                volumeMetric = prefs.volumeMetric.first().name,
                weeklyTargetSessions = prefs.weeklyTargetSessions.first(),
            ),
            customExercises = customs,
            plans = planSnapshots,
            sessions = sessionSnapshots,
        )
    }

    private suspend fun applyPayload(payload: BackupPayload) {
        database.withTransaction {
            // 1) Restore custom exercises: onto the same clientUuid if present (deleted rows
            //    included — history restored in step 3 refers to them by that id), else onto a
            //    live exercise of the same name, else as a new row. See [RestoreMatch].
            //    A match on a built-in is never rewritten: the backup's id is aliased to it instead.
            val builtInAliases = mutableMapOf<String, ExerciseEntity>()
            for (ce in payload.customExercises) {
                val existing = RestoreMatch.customTarget(
                    snapshot = ce,
                    sameUuid = repository.findExerciseByClientUuid(ce.clientUuid),
                    liveSameName = repository.findActiveExerciseByName(ce.name),
                )
                val builtIn = RestoreMatch.builtInAlias(existing)
                if (builtIn != null) {
                    builtInAliases[ce.clientUuid] = builtIn
                    continue
                }
                val row = ExerciseEntity(
                    id = existing?.id ?: 0,
                    clientUuid = ce.clientUuid,
                    name = ce.name,
                    movementType = parseEnum(ce.movementType, MovementType.Compound),
                    primaryMuscles = ce.primaryMuscles.mapNotNull { parseEnumOrNull<Muscle>(it) },
                    secondaryMuscles = ce.secondaryMuscles.mapNotNull { parseEnumOrNull<Muscle>(it) },
                    equipment = parseEnum(ce.equipment, Equipment.Other),
                    cues = ce.cues,
                    isCustom = true,
                    isDeleted = ce.isDeleted,
                )
                // UPDATE in place, never REPLACE: REPLACE deletes first, which the RESTRICT keys on
                // logged_sets / planned_exercises reject once the exercise has been used.
                if (existing != null) repository.updateExercise(row) else repository.insertExercise(row)
            }

            // 2) Plans + planned exercises (resolve exercise refs by clientUuid).
            val allExercises = repository.observeExercises().first() + repository.observeDeletedExercises().first()
            val exerciseByUuid = allExercises.associateBy { it.clientUuid } + builtInAliases

            for (p in payload.plans) {
                if (p.isDeleted) continue   // v1 backups may carry archived plans; skip them.
                val planId = repository.insertPlan(
                    PlanEntity(
                        clientUuid = p.clientUuid,
                        name = p.name,
                        notes = p.notes,
                        createdAtMillis = p.createdAtMillis,
                    )
                )
                val plannedRows = p.planned.mapNotNull { pl ->
                    val ex = RestoreMatch.plannedTarget(exerciseByUuid[pl.exerciseClientUuid]) ?: return@mapNotNull null
                    PlannedExerciseEntity(
                        planId = planId,
                        exerciseId = ex.id,
                        sortIndex = pl.sortIndex,
                        targetSets = pl.targetSets,
                        targetRepsLow = pl.targetRepsLow,
                        targetRepsHigh = pl.targetRepsHigh,
                        notes = pl.notes,
                    )
                }
                if (plannedRows.isNotEmpty()) repository.insertPlannedAll(plannedRows)
            }

            // Plans already on the device may hold an exercise the backup restored as deleted.
            repository.removeDeletedExercisesFromPlans()

            // 3) Sessions + sets — by clientUuid over every row, deleted included, so history
            //    of a deleted exercise comes back attached to it.
            val allPlans = repository.observePlans().first()
            val planByUuid = allPlans.associateBy { it.clientUuid }
            val refreshedExercises = repository.observeExercises().first() + repository.observeDeletedExercises().first()
            val exerciseByUuid2 = refreshedExercises.associateBy { it.clientUuid } + builtInAliases

            for (s in payload.sessions) {
                val planId = s.planClientUuid?.let { planByUuid[it]?.id }
                val session = SessionEntity(
                    clientUuid = s.clientUuid.ifBlank { UUID.randomUUID().toString() },
                    planId = planId,
                    dateEpochDay = s.dateEpochDay,
                    loggedAtMillis = s.loggedAtMillis,
                    notes = s.notes,
                    startedAtMillis = s.startedAtMillis,
                )
                repository.saveSession(session) { sid ->
                    s.sets.mapNotNull { snap ->
                        val ex = exerciseByUuid2[snap.exerciseClientUuid] ?: return@mapNotNull null
                        LoggedSetEntity(
                            sessionId = sid,
                            exerciseId = ex.id,
                            setIndex = snap.setIndex,
                            repsDone = snap.repsDone,
                            weightKg = snap.weightKg,
                            isCompleted = snap.isCompleted,
                            rpe = snap.rpe,
                            setType = snap.setType?.let { parseEnumOrNull<SetType>(it) } ?: SetType.Working,
                        )
                    }
                }
            }
        }

        // Restore preferences last (outside the DB transaction).
        prefs.setHapticsEnabled(payload.prefs.haptics)
        prefs.setWeightUnit(parseEnum(payload.prefs.weightUnit, WeightUnit.Kg))
        prefs.setSeedVersion(payload.prefs.seedVersion)
        payload.prefs.volumeMetric?.let { name ->
            parseEnumOrNull<VolumeMetric>(name)?.let { prefs.setVolumeMetric(it) }
        }
        payload.prefs.weeklyTargetSessions?.let { prefs.setWeeklyTargetSessions(it) }
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    private inline fun <reified T : Enum<T>> parseEnumOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value) }.getOrNull()

    companion object {
        fun suggestedFileName(): String {
            val ts = System.currentTimeMillis()
            return "gym-tracker-backup-$ts.json"
        }
    }
}

/** How restore maps backed-up exercises onto local rows. Pure, so it can be unit-tested. */
internal object RestoreMatch {
    /**
     * The local row a backed-up custom exercise is written onto, or null to insert it new.
     * The same clientUuid wins — deleted rows included, since it is the same exercise and restored
     * history points at it. Failing that, a live backup row merges onto a live exercise of the same
     * name. Deleted rows never take part in name matching on either side: a deleted local row
     * would be revived or hijacked, and a deleted backup row would delete the user's live one.
     */
    fun customTarget(
        snapshot: ExerciseSnapshot,
        sameUuid: ExerciseEntity?,
        liveSameName: ExerciseEntity?,
    ): ExerciseEntity? =
        sameUuid ?: liveSameName?.takeIf { !snapshot.isDeleted && !it.isDeleted }

    /**
     * The built-in a backed-up custom exercise resolved to, or null. Built-ins are never rewritten
     * (the seed owns their ids and fields, and they must stay non-editable), so restore points the
     * backup's id at the built-in and its plans and history attach there.
     */
    fun builtInAlias(target: ExerciseEntity?): ExerciseEntity? = target?.takeIf { !it.isCustom }

    /** A plan row's exercise, or null to drop the row: plans never hold a deleted exercise. */
    fun plannedTarget(exercise: ExerciseEntity?): ExerciseEntity? = exercise?.takeIf { !it.isDeleted }
}
