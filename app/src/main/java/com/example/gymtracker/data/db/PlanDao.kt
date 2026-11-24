package com.example.gymtracker.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlannedWithExercise(
    @Embedded val planned: PlannedExerciseEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id",
    )
    val exercise: ExerciseEntity,
)

data class PlanWithExercises(
    @Embedded val plan: PlanEntity,
    @Relation(
        entity = PlannedExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "plan_id",
    )
    val planned: List<PlannedWithExercise>,
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY created_at_millis ASC")
    fun observePlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun findPlan(id: Long): PlanEntity?

    @Query("SELECT * FROM plans WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findPlanByName(name: String): PlanEntity?

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    fun observePlanWithExercises(id: Long): Flow<PlanWithExercises?>

    /** Every plan with its planned exercises, one-shot (Room batches the relations). */
    @Transaction
    @Query("SELECT * FROM plans ORDER BY created_at_millis ASC")
    suspend fun allPlansWithExercises(): List<PlanWithExercises>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanned(planned: PlannedExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedAll(planned: List<PlannedExerciseEntity>): List<Long>

    @Update
    suspend fun updatePlanned(planned: PlannedExerciseEntity)

    @Query("DELETE FROM planned_exercises WHERE id = :id")
    suspend fun deletePlanned(id: Long)

    @Query("DELETE FROM planned_exercises WHERE plan_id = :planId")
    suspend fun deletePlannedForPlan(planId: Long)

    /** Take one exercise out of every plan (its soft delete). */
    @Query("DELETE FROM planned_exercises WHERE exercise_id = :exerciseId")
    suspend fun deletePlannedForExercise(exerciseId: Long): Int

    /** Take every soft-deleted exercise out of every plan (repair after restore / older builds). */
    @Query("DELETE FROM planned_exercises WHERE exercise_id IN (SELECT id FROM exercises WHERE is_deleted = 1)")
    suspend fun deletePlannedForDeletedExercises(): Int

    @Query("SELECT * FROM planned_exercises ORDER BY plan_id ASC, sort_index ASC")
    fun observeAllPlanned(): Flow<List<PlannedExerciseEntity>>
}
