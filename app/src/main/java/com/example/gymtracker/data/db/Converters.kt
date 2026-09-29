package com.example.gymtracker.data.db

import androidx.room.TypeConverter
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.SetType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())

class Converters {
    @TypeConverter
    fun muscleListToJson(value: List<Muscle>): String =
        json.encodeToString(stringListSerializer, value.map { it.name })

    @TypeConverter
    fun jsonToMuscleList(value: String): List<Muscle> =
        json.decodeFromString(stringListSerializer, value)
            .mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun movementTypeToString(value: MovementType): String = value.name

    @TypeConverter
    fun stringToMovementType(value: String): MovementType =
        runCatching { MovementType.valueOf(value) }.getOrDefault(MovementType.Compound)

    @TypeConverter
    fun equipmentToString(value: Equipment): String = value.name

    @TypeConverter
    fun stringToEquipment(value: String): Equipment =
        runCatching { Equipment.valueOf(value) }.getOrDefault(Equipment.Other)

    @TypeConverter
    fun setTypeToString(value: SetType): String = value.name

    @TypeConverter
    fun stringToSetType(value: String): SetType =
        runCatching { SetType.valueOf(value) }.getOrDefault(SetType.Working)
}
