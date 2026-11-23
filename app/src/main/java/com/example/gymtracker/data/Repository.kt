package com.example.gymtracker.data

import com.example.gymtracker.data.db.AppDatabase
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.ExerciseSetWithDate
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlanWithExercises
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.SessionDraftEntity
import com.example.gymtracker.data.db.SessionEntity
import com.example.gymtracker.data.db.SessionWithSets
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** The user-editable fields of a custom exercise (§4.19). */
data class CustomExerciseFields(
    val name: String,
    val movementType: MovementType,
    val equipment: Equipment,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
)

class GymRepository(private val db: AppDatabase) {
    private val exerciseDao = db.exerciseDao()
    private val planDao = db.planDao()
    private val sessionDao = db.sessionDao()
    private val sessionDraftDao = db.sessionDraftDao()

    // Exercises
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()
    fun observeExercise(id: Long): Flow<ExerciseEntity?> = exerciseDao.observeById(id)
    suspend fun findExercise(id: Long): ExerciseEntity? = exerciseDao.findById(id)
    /** Name match over every row, deleted included — the seed only; use [findActiveExerciseByName]. */
    suspend fun findExerciseByName(name: String): ExerciseEntity? = exerciseDao.findByName(name)
    suspend fun findExerciseByClientUuid(clientUuid: String): ExerciseEntity? = exerciseDao.findByClientUuid(clientUuid)
    suspend fun countExercises(): Int = exerciseDao.count()
    suspend fun insertExercise(exercise: ExerciseEntity): Long = exerciseDao.insert(exercise)
    suspend fun insertExercises(exercises: List<ExerciseEntity>): List<Long> = exerciseDao.insertAll(exercises)
    suspend fun updateExercise(exercise: ExerciseEntity) = exerciseDao.update(exercise)
    /**
     * Soft delete: the row stays (logged sets reference it, RESTRICT) but it leaves every plan,
     * in the same transaction, so no plan ever lists a deleted exercise. A running draft keeps it.
     */
    suspend fun softDeleteExercise(id: Long) = db.withTransaction {
        planDao.deletePlannedForExercise(id)
        exerciseDao.setDeleted(id, true)
    }

    /** Drop plan rows that point at soft-deleted exercises (see [softDeleteExercise]). */
    suspend fun removeDeletedExercisesFromPlans(): Int = planDao.deletePlannedForDeletedExercises()
    suspend fun restoreExercise(id: Long) = exerciseDao.setDeleted(id, false)
    fun observeDeletedExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeDeleted()
    fun observeAllExercisesIncludingDeleted(): Flow<List<ExerciseEntity>> = exerciseDao.observeAllIncludingDeleted()
    suspend fun findActiveExerciseByName(name: String): ExerciseEntity? = exerciseDao.findActiveByName(name)

    /** Insert a user-made exercise (phase 9): fresh random UUID, `isCustom = true`, no cues. */
    suspend fun createCustomExercise(fields: CustomExerciseFields): Long = exerciseDao.insert(
        ExerciseEntity(
            clientUuid = UUID.randomUUID().toString(),
            name = fields.name.trim(),
            movementType = fields.movementType,
            primaryMuscles = fields.primaryMuscles,
            secondaryMuscles = fields.secondaryMuscles,
            equipment = fields.equipment,
            isCustom = true,
        )
    )

    /**
     * Update a custom exercise's editable fields. Refuses (returns false) for built-ins — the
     * seed's deterministic UUIDs and `seedVersion` reseeding depend on them never changing — and
     * for soft-deleted rows. UUID, cues and flags are kept.
     */
    suspend fun updateCustom(id: Long, fields: CustomExerciseFields): Boolean {
        val existing = exerciseDao.findById(id) ?: return false
        if (!existing.isCustom || existing.isDeleted) return false
        exerciseDao.update(
            existing.copy(
                name = fields.name.trim(),
                movementType = fields.movementType,
                primaryMuscles = fields.primaryMuscles,
                secondaryMuscles = fields.secondaryMuscles,
                equipment = fields.equipment,
            )
        )
        return true
    }

    // Plans
    fun observePlans(): Flow<List<PlanEntity>> = planDao.observePlans()
    fun observePlanWithExercises(id: Long): Flow<PlanWithExercises?> = planDao.observePlanWithExercises(id)
    suspend fun findPlan(id: Long): PlanEntity? = planDao.findPlan(id)
    suspend fun findPlanByName(name: String): PlanEntity? = planDao.findPlanByName(name)
    suspend fun insertPlan(plan: PlanEntity): Long = planDao.insertPlan(plan)
    suspend fun updatePlan(plan: PlanEntity) = planDao.updatePlan(plan)
    suspend fun deletePlan(id: Long) = planDao.deletePlan(id)
    suspend fun insertPlanned(planned: PlannedExerciseEntity): Long = planDao.insertPlanned(planned)
    suspend fun insertPlannedAll(planned: List<PlannedExerciseEntity>): List<Long> = planDao.insertPlannedAll(planned)
    suspend fun updatePlanned(planned: PlannedExerciseEntity) = planDao.updatePlanned(planned)
    suspend fun deletePlanned(id: Long) = planDao.deletePlanned(id)
    fun observeAllPlanned(): Flow<List<PlannedExerciseEntity>> = planDao.observeAllPlanned()

    /** Replace a plan's entire planned-exercise list in one transaction (used when a logged
     *  session's structure is pushed back into its parent plan). */
    suspend fun replacePlanned(planId: Long, planned: List<PlannedExerciseEntity>) =
        db.withTransaction {
            planDao.deletePlannedForPlan(planId)
            if (planned.isNotEmpty()) planDao.insertPlannedAll(planned)
        }

    /** Run [block] inside a single DB transaction (all-or-nothing multi-table writes). */
    suspend fun <R> withTransaction(block: suspend () -> R): R = db.withTransaction { block() }

    // Sessions
    fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()
    fun observeAllSessionsWithSets(): Flow<List<SessionWithSets>> = sessionDao.observeAllSessionsWithSets()
    fun observeSessionsInRange(start: Long, end: Long): Flow<List<SessionEntity>> =
        sessionDao.observeInRange(start, end)
    fun observeSessionsWithSetsInRange(start: Long, end: Long): Flow<List<SessionWithSets>> =
        sessionDao.observeSessionsWithSetsInRange(start, end)
    fun observeSessionWithSets(id: Long): Flow<SessionWithSets?> = sessionDao.observeSessionWithSets(id)
    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)
    suspend fun updateSessionNotes(id: Long, notes: String) = sessionDao.updateNotes(id, notes)
    suspend fun recentExerciseIds(limit: Int = 8): List<Long> = sessionDao.recentExerciseIds(limit)

    fun observeExerciseSetsWithDate(exerciseId: Long): Flow<List<ExerciseSetWithDate>> =
        sessionDao.observeExerciseSetsWithDate(exerciseId)

    /** Map setIndex → last logged set for this exercise across history. */
    suspend fun lastLoggedSetsFor(exerciseId: Long): Map<Int, LoggedSetEntity> =
        sessionDao.lastLoggedSetsFor(exerciseId).associateBy { it.setIndex }

    /** Inserts a session + all its sets in a single transaction. */
    suspend fun saveSession(
        session: SessionEntity,
        setsBuilder: (sessionId: Long) -> List<LoggedSetEntity>,
    ): Long = db.withTransaction {
        sessionDao.insertSessionWithSets(session, setsBuilder)
    }

    // Session draft (singleton, id = 1)
    fun observeSessionDraft(): Flow<SessionDraftEntity?> = sessionDraftDao.observe()
    suspend fun findSessionDraft(): SessionDraftEntity? = sessionDraftDao.find()
    suspend fun upsertSessionDraft(draft: SessionDraftEntity) = sessionDraftDao.upsert(draft)
    suspend fun clearSessionDraft() = sessionDraftDao.clear()
}
