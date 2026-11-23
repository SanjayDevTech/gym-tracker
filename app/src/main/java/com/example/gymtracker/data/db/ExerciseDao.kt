package com.example.gymtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE is_deleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ExerciseEntity?>

    /** Any row, soft-deleted included — only the seed uses this (built-ins are never deleted). */
    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ExerciseEntity?

    /** By the stable id backups and history refer to; soft-deleted rows included. */
    @Query("SELECT * FROM exercises WHERE client_uuid = :clientUuid LIMIT 1")
    suspend fun findByClientUuid(clientUuid: String): ExerciseEntity?

    /** Like [findByName] but never a soft-deleted row: every name match outside the seed. */
    @Query("SELECT * FROM exercises WHERE name = :name AND is_deleted = 0 LIMIT 1")
    suspend fun findActiveByName(name: String): ExerciseEntity?

    /** Every row, soft-deleted included — for resolving names/muscles of logged history. */
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE ASC")
    fun observeAllIncludingDeleted(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE is_deleted = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeDeleted(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET is_deleted = :deleted WHERE id = :id")
    suspend fun setDeleted(id: Long, deleted: Boolean)
}
