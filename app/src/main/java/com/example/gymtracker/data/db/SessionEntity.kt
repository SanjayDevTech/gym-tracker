package com.example.gymtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtracker.data.SetType

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["date_epoch_day"]),
        Index(value = ["plan_id"]),
        Index(value = ["client_uuid"], unique = true),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_uuid") val clientUuid: String,
    @ColumnInfo(name = "plan_id") val planId: Long? = null,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "logged_at_millis") val loggedAtMillis: Long,
    @ColumnInfo(name = "notes") val notes: String = "",
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["exercise_id", "session_id"]),
    ],
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "reps_done") val repsDone: Int? = null,
    @ColumnInfo(name = "weight_kg") val weightKg: Float? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "rpe") val rpe: Float? = null,
    @ColumnInfo(name = "set_type", defaultValue = "Working") val setType: SetType = SetType.Working,
)
