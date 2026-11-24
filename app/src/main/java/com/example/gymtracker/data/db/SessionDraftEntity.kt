package com.example.gymtracker.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Singleton row (id always 1) holding the in-flight session being logged. */
@Entity(tableName = "session_drafts")
data class SessionDraftEntity(
    @PrimaryKey val id: Long = 1L,
    val planId: Long?,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val payloadJson: String,
)

@Dao
interface SessionDraftDao {
    @Query("SELECT * FROM session_drafts WHERE id = 1")
    fun observe(): Flow<SessionDraftEntity?>

    @Query("SELECT * FROM session_drafts WHERE id = 1")
    suspend fun find(): SessionDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: SessionDraftEntity)

    @Query("DELETE FROM session_drafts WHERE id = 1")
    suspend fun clear()
}

@Serializable
data class SessionDraftPayload(
    val planName: String? = null,
    val dateEpochDay: Long,
    val groups: List<SessionDraftGroup>,
    // Wall-clock of the last completed set, so the rest timer survives leaving the logger.
    val lastSetCompletedAtMillis: Long = 0L,
    // This workout's rest target (§6.3). Seeded from the default-rest pref; drafts written
    // before the field existed decode to today's old constant.
    val restTargetSeconds: Int = 90,
)

@Serializable
data class SessionDraftGroup(
    val exerciseId: Long,
    val exerciseName: String,
    val movementType: String,
    val targetRepsLow: Int,
    val targetRepsHigh: Int,
    val sets: List<SessionDraftSet>,
    // Stable id so reorder animations and list keys survive across draft round-trips.
    val uid: String = "",
    // "Skip exercise" in the logger. Drafts written before the field existed decode to false.
    val skipped: Boolean = false,
)

@Serializable
data class SessionDraftSet(
    val repsInput: String = "",
    val weightInput: String = "",
    val hintReps: Int? = null,
    val hintWeightKg: Float? = null,
    val isCompleted: Boolean = false,
    val setType: String = "Working",
)
