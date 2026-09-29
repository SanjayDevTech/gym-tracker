package com.example.gymtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plans",
    indices = [Index(value = ["client_uuid"], unique = true)],
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_uuid") val clientUuid: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
)

@Entity(
    tableName = "planned_exercises",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
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
        Index(value = ["plan_id"]),
        Index(value = ["exercise_id"]),
    ],
)
data class PlannedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_id") val planId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "sort_index") val sortIndex: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "target_reps_low") val targetRepsLow: Int,
    @ColumnInfo(name = "target_reps_high") val targetRepsHigh: Int,
    @ColumnInfo(name = "notes") val notes: String = "",
)
