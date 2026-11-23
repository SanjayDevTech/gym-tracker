package com.example.gymtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.gymtracker.data.SetType
import kotlinx.coroutines.flow.Flow

data class SessionWithSets(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id",
    )
    val sets: List<LoggedSetEntity>,
)

/** A logged set joined to the day of its session — drives per-exercise PRs and progress charts. */
data class ExerciseSetWithDate(
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "reps_done") val repsDone: Int?,
    @ColumnInfo(name = "weight_kg") val weightKg: Float?,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "set_type") val setType: SetType,
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<LoggedSetEntity>): List<Long>

    @Transaction
    suspend fun insertSessionWithSets(
        session: SessionEntity,
        setsBuilder: (sessionId: Long) -> List<LoggedSetEntity>,
    ): Long {
        val sessionId = insertSession(session)
        val rows = setsBuilder(sessionId)
        if (rows.isNotEmpty()) insertSets(rows)
        return sessionId
    }

    @Query("UPDATE sessions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    /** The [limit] most recently logged distinct exercises, newest first (the picker's RECENT). */
    @Query(
        """
        SELECT ls.exercise_id FROM logged_sets ls
        INNER JOIN sessions s ON s.id = ls.session_id
        GROUP BY ls.exercise_id
        ORDER BY MAX(s.date_epoch_day) DESC, MAX(s.logged_at_millis) DESC
        LIMIT :limit
        """
    )
    suspend fun recentExerciseIds(limit: Int): List<Long>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM sessions ORDER BY date_epoch_day DESC, logged_at_millis DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY date_epoch_day DESC, logged_at_millis DESC")
    fun observeAllSessionsWithSets(): Flow<List<SessionWithSets>>

    @Query("SELECT * FROM sessions WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY date_epoch_day DESC, logged_at_millis DESC")
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY date_epoch_day DESC, logged_at_millis DESC")
    fun observeSessionsWithSetsInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    fun observeSessionWithSets(id: Long): Flow<SessionWithSets?>

    @Query("SELECT DISTINCT date_epoch_day FROM sessions ORDER BY date_epoch_day DESC")
    fun observeSessionDays(): Flow<List<Long>>

    @Query(
        """
        SELECT * FROM logged_sets
        WHERE exercise_id = :exerciseId
          AND session_id = (
              SELECT s.id FROM sessions s
              INNER JOIN logged_sets ls ON ls.session_id = s.id
              WHERE ls.exercise_id = :exerciseId
              ORDER BY s.date_epoch_day DESC, s.logged_at_millis DESC
              LIMIT 1
          )
        ORDER BY set_index ASC
        """
    )
    suspend fun lastLoggedSetsFor(exerciseId: Long): List<LoggedSetEntity>

    @Query(
        """
        SELECT ls.* FROM logged_sets ls
        INNER JOIN sessions s ON s.id = ls.session_id
        WHERE ls.exercise_id = :exerciseId
        ORDER BY s.date_epoch_day DESC, s.logged_at_millis DESC, ls.set_index ASC
        LIMIT :limit
        """
    )
    suspend fun recentSetsForExercise(exerciseId: Long, limit: Int = 30): List<LoggedSetEntity>

    /** Full logged history for one exercise, each set carrying its session date (ascending). */
    @Query(
        """
        SELECT s.date_epoch_day, ls.session_id, ls.reps_done, ls.weight_kg, ls.is_completed, ls.set_type
        FROM logged_sets ls
        INNER JOIN sessions s ON s.id = ls.session_id
        WHERE ls.exercise_id = :exerciseId
        ORDER BY s.date_epoch_day ASC, s.logged_at_millis ASC, ls.set_index ASC
        """
    )
    fun observeExerciseSetsWithDate(exerciseId: Long): Flow<List<ExerciseSetWithDate>>

    /** Every session with its volume and completed-set count, oldest first (see [SessionVolume]). */
    @Query(
        """
        SELECT s.id AS session_id, s.plan_id, s.date_epoch_day, s.logged_at_millis, s.started_at_millis,
            COALESCE(SUM(CASE WHEN ls.is_completed = 1 AND ls.set_type != 'Warmup'
                THEN COALESCE(ls.reps_done, 0) * COALESCE(ls.weight_kg, 0) ELSE 0 END), 0) AS volume_kg,
            COALESCE(SUM(CASE WHEN ls.is_completed = 1 THEN 1 ELSE 0 END), 0) AS done_sets
        FROM sessions s
        LEFT JOIN logged_sets ls ON ls.session_id = s.id
        GROUP BY s.id
        ORDER BY s.date_epoch_day ASC, s.logged_at_millis ASC
        """
    )
    fun observeSessionVolumes(): Flow<List<SessionVolume>>
}

/**
 * One session with its training volume folded in SQL — Train's week hero, per-plan sparklines and
 * typical durations. [volumeKg] follows the tonnage rule used elsewhere: Σ reps × load over
 * completed, non-warm-up sets (bodyweight sets add 0). [doneSets] counts every completed set.
 */
data class SessionVolume(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "plan_id") val planId: Long?,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "logged_at_millis") val loggedAtMillis: Long,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long?,
    @ColumnInfo(name = "volume_kg") val volumeKg: Double,
    @ColumnInfo(name = "done_sets") val doneSets: Int,
)
