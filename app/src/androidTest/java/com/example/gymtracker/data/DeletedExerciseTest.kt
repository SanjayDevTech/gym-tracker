package com.example.gymtracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtracker.data.db.AppDatabase
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Soft-deleting a custom exercise takes it out of plans; name lookups skip it; history stays. */
@RunWith(AndroidJUnit4::class)
class DeletedExerciseTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: GymRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
        repo = GymRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun exercise(name: String, uuid: String) = repo.insertExercise(
        ExerciseEntity(
            clientUuid = uuid,
            name = name,
            movementType = MovementType.Compound,
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            equipment = Equipment.Barbell,
            isCustom = true,
        ),
    )

    private suspend fun plan(name: String, vararg exerciseIds: Long): Long {
        val id = repo.insertPlan(PlanEntity(clientUuid = name, name = name, createdAtMillis = 0))
        repo.insertPlannedAll(
            exerciseIds.mapIndexed { i, ex ->
                PlannedExerciseEntity(planId = id, exerciseId = ex, sortIndex = i, targetSets = 3, targetRepsLow = 8, targetRepsHigh = 12)
            },
        )
        return id
    }

    @Test
    fun softDeleteRemovesFromEveryPlanButKeepsHistory() = runBlocking {
        val landmine = exercise("Landmine press", "a")
        val curl = exercise("Curl", "b")
        val push = plan("Push", landmine, curl)
        val pull = plan("Pull", landmine)
        repo.saveSession(SessionEntity(clientUuid = "s", dateEpochDay = 1, loggedAtMillis = 1)) { sid ->
            listOf(LoggedSetEntity(sessionId = sid, exerciseId = landmine, setIndex = 0, repsDone = 8, isCompleted = true))
        }

        repo.softDeleteExercise(landmine)

        assertEquals(listOf(curl), repo.observePlanWithExercises(push).first()!!.planned.map { it.exercise.id })
        assertTrue(repo.observePlanWithExercises(pull).first()!!.planned.isEmpty())
        assertTrue(repo.findExercise(landmine)!!.isDeleted)
        assertEquals(1, repo.observeExerciseSetsWithDate(landmine).first().size)
    }

    @Test
    fun nameLookupsSkipDeletedButUuidLookupFindsThem() = runBlocking {
        val id = exercise("Landmine press", "a")
        repo.softDeleteExercise(id)

        assertNull(repo.findActiveExerciseByName("Landmine press"))
        assertEquals(id, repo.findExerciseByClientUuid("a")?.id)
    }

    @Test
    fun repairDropsPlanRowsOfAlreadyDeletedExercises() = runBlocking {
        val landmine = exercise("Landmine press", "a")
        val push = plan("Push", landmine)
        // A build before this fix: deleted without touching plans.
        db.exerciseDao().setDeleted(landmine, true)

        assertEquals(1, repo.removeDeletedExercisesFromPlans())
        assertTrue(repo.observePlanWithExercises(push).first()!!.planned.isEmpty())
    }
}
