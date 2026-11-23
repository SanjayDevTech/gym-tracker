package com.example.gymtracker.domain

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.domain.ExerciseNameRule.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun normalizedNameTrimsAndFoldsCase() {
        assertEquals("bench press", ExerciseNameRule.normalizedName("  Bench PRESS\t"))
        assertEquals(ExerciseNameRule.normalizedName("bench press"), ExerciseNameRule.normalizedName(" Bench Press "))
    }

    @Test
    fun liveMatchIgnoresCaseAndSurroundingSpace() {
        assertEquals(1L, ExerciseNameRule.findLiveMatch("bench press", library)?.id)
        assertEquals(1L, ExerciseNameRule.findLiveMatch("  BENCH PRESS \n", library)?.id)
        assertEquals(2L, ExerciseNameRule.findLiveMatch("landmine PRESS", library)?.id)
        assertNull(ExerciseNameRule.findLiveMatch("Bench", library))
        assertNull(ExerciseNameRule.findLiveMatch("   ", library))
    }

    @Test
    fun liveMatchSkipsDeletedRows() {
        assertNull(ExerciseNameRule.findLiveMatch("old lift", library))
        val withDeletedTwin = listOf(ex(5, "curl", deleted = true), ex(6, "Curl", custom = true))
        assertEquals(6L, ExerciseNameRule.findLiveMatch("CURL", withDeletedTwin)?.id)
    }

    @Test
    fun caseOnlyDuplicatesPickBuiltInThenLowestId() {
        val dupes = listOf(
            ex(9, "bench press", custom = true),
            ex(4, "BENCH PRESS ", custom = true),
            ex(12, "Bench Press"),
        )
        assertEquals(12L, ExerciseNameRule.findLiveMatch("Bench press", dupes)?.id)
        assertEquals(12L, ExerciseNameRule.findLiveMatch("Bench press", dupes.reversed())?.id)
        val customsOnly = dupes.filter { it.isCustom }
        assertEquals(4L, ExerciseNameRule.findLiveMatch("bench PRESS", customsOnly)?.id)
        assertEquals(4L, ExerciseNameRule.findLiveMatch("bench PRESS", customsOnly.reversed())?.id)
    }
}
