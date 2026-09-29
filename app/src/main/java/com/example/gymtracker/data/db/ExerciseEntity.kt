package com.example.gymtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["client_uuid"], unique = true),
        Index(value = ["name"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_uuid") val clientUuid: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "movement_type") val movementType: MovementType,
    @ColumnInfo(name = "primary_muscles") val primaryMuscles: List<Muscle>,
    @ColumnInfo(name = "secondary_muscles") val secondaryMuscles: List<Muscle>,
    @ColumnInfo(name = "equipment") val equipment: Equipment,
    @ColumnInfo(name = "cues") val cues: String = "",
    @ColumnInfo(name = "is_custom") val isCustom: Boolean = false,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
