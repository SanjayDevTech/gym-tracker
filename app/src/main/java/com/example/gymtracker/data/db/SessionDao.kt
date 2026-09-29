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
}
