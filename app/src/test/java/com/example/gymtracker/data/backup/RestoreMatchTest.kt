package com.example.gymtracker.data.backup

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.db.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Backup restore never name-matches a deleted exercise, but keeps id (clientUuid) matching. */
class RestoreMatchTest {

    private fun snapshot(uuid: String = "backup-uuid", name: String = "Landmine press", deleted: Boolean = false) =
        ExerciseSnapshot(uuid, name, "Compound", emptyList(), emptyList(), "Barbell", "", deleted)

    private fun row(id: Long, uuid: String, name: String = "Landmine press", deleted: Boolean = false, custom: Boolean = true) =
        ExerciseEntity(
            id = id,
            clientUuid = uuid,
            name = name,
            movementType = MovementType.Compound,
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            equipment = Equipment.Barbell,
            isCustom = custom,
            isDeleted = deleted,
        )

    @Test
    fun sameUuidWinsEvenWhenThatRowIsDeleted() {
        val deleted = row(7, "backup-uuid", deleted = true)
        val live = row(9, "other")
        assertEquals(7L, RestoreMatch.customTarget(snapshot(), sameUuid = deleted, liveSameName = live)?.id)
    }

    @Test
    fun deletedBackupRowRestoresOntoItsOwnUuid() {
        val mine = row(7, "backup-uuid", name = "Old name", deleted = false)
        assertEquals(7L, RestoreMatch.customTarget(snapshot(deleted = true), sameUuid = mine, liveSameName = null)?.id)
    }

    @Test
    fun liveBackupRowMergesOntoLiveSameName() {
        val live = row(9, "local-uuid")
        assertEquals(9L, RestoreMatch.customTarget(snapshot(), sameUuid = null, liveSameName = live)?.id)
    }

    @Test
    fun deletedBackupRowNeverTakesOverALiveExerciseByName() {
        val live = row(9, "local-uuid")
        assertNull(RestoreMatch.customTarget(snapshot(deleted = true), sameUuid = null, liveSameName = live))
    }

    @Test
    fun deletedLocalRowIsNeverANameMatch() {
        val deleted = row(7, "local-uuid", deleted = true)
        assertNull(RestoreMatch.customTarget(snapshot(), sameUuid = null, liveSameName = deleted))
    }

    @Test
    fun noMatchInsertsNew() {
        assertNull(RestoreMatch.customTarget(snapshot(), sameUuid = null, liveSameName = null))
    }

    @Test
    fun plansDropDeletedExercises() {
        assertNull(RestoreMatch.plannedTarget(row(7, "a", deleted = true)))
        assertNull(RestoreMatch.plannedTarget(null))
        assertEquals(9L, RestoreMatch.plannedTarget(row(9, "b"))?.id)
    }
}
