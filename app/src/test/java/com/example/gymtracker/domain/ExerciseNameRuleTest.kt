package com.example.gymtracker.domain

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.domain.ExerciseNameRule.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseNameRuleTest {
    private fun ex(id: Long, name: String, deleted: Boolean = false, custom: Boolean = false) = ExerciseEntity(
        id = id,
        clientUuid = "uuid-$id",
        name = name,
        movementType = MovementType.Compound,
        primaryMuscles = emptyList(),
        secondaryMuscles = emptyList(),
        equipment = Equipment.Barbell,
        isCustom = custom,
        isDeleted = deleted,
    )

    private val library = listOf(
        ex(1, "Bench Press"),
        ex(2, "Landmine Press", custom = true),
        ex(3, "Old Lift", deleted = true, custom = true),
    )

    @Test
    fun blankAndWhitespaceNamesAreRejected() {
        assertEquals(Result.Blank, ExerciseNameRule.validate("", library))
        assertEquals(Result.Blank, ExerciseNameRule.validate("   \t", library))
    }

    @Test
    fun okNameIsTrimmed() {
        assertEquals(Result.Ok("Cable Fly"), ExerciseNameRule.validate("  Cable Fly ", library))
    }

    @Test
    fun duplicateIsCaseInsensitiveAndIgnoresSurroundingSpace() {
        assertEquals(Result.Duplicate("Bench Press"), ExerciseNameRule.validate("bench press", library))
        assertEquals(Result.Duplicate("Bench Press"), ExerciseNameRule.validate("  BENCH PRESS  ", library))
    }

    @Test
    fun softDeletedNamesAreFreeToReuse() {
        assertEquals(Result.Ok("Old Lift"), ExerciseNameRule.validate("Old Lift", library))
    }

    @Test
    fun editingMayKeepItsOwnNameButNotTakeAnother() {
        assertEquals(Result.Ok("landmine press"), ExerciseNameRule.validate("landmine press", library, editingId = 2))
        assertEquals(Result.Duplicate("Bench Press"), ExerciseNameRule.validate("Bench press", library, editingId = 2))
    }
}
