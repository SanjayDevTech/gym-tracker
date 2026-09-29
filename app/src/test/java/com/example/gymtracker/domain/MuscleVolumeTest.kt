package com.example.gymtracker.domain

import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.VolumeMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleVolumeTest {

    private val window = 0L..10L

    // Exercise 1: pull-up — primary Lats, secondary Biceps (bodyweight).
    // Exercise 2: barbell row — primary UpperBack, secondary Lats (weighted).
    private val muscleMap = mapOf(
        1L to ExerciseMuscleMap(primary = listOf(Muscle.Lats), secondary = listOf(Muscle.Biceps)),
        2L to ExerciseMuscleMap(primary = listOf(Muscle.UpperBack), secondary = listOf(Muscle.Lats)),
    )

    private fun set(
        exerciseId: Long,
        reps: Int? = 10,
        weight: Float? = null,
        completed: Boolean = true,
        warmup: Boolean = false,
    ) = SessionWindowSet(exerciseId, reps, weight, completed, warmup)

    private fun day(d: Long, vararg sets: SessionWindowSet) = SessionWindow(d, sets.toList())

    // --- Sets metric: the bodyweight fix ---

    @Test
    fun `sets metric counts bodyweight sets that tonnage ignores`() {
        val sessions = listOf(day(1, set(exerciseId = 1, reps = 12, weight = null)))

        val tonnage = MuscleVolume.computeForWindow(sessions, window, muscleMap, VolumeMetric.Tonnage)
        assertTrue("bodyweight set is invisible to tonnage", tonnage.isEmpty())

        val sets = MuscleVolume.computeForWindow(sessions, window, muscleMap, VolumeMetric.Sets)
        assertEquals(1f, sets[Muscle.Lats]!!, 0.001f)        // primary
        assertEquals(0.5f, sets[Muscle.Biceps]!!, 0.001f)    // secondary
    }

    @Test
    fun `sets metric is the default`() {
        val sessions = listOf(day(1, set(exerciseId = 1, weight = null)))
        val default = MuscleVolume.computeForWindow(sessions, window, muscleMap)
        assertEquals(1f, default[Muscle.Lats]!!, 0.001f)
    }

    @Test
    fun `sets metric ignores load magnitude - heavy and bodyweight both count one`() {
        val sessions = listOf(
            day(1, set(exerciseId = 1, weight = null)),         // bodyweight pull-up
            day(2, set(exerciseId = 2, weight = 100f)),         // heavy row
        )
        val sets = MuscleVolume.computeForWindow(sessions, window, muscleMap, VolumeMetric.Sets)
        // Lats = pull-up primary (1.0) + row secondary (0.5) = 1.5
        assertEquals(1.5f, sets[Muscle.Lats]!!, 0.001f)
        assertEquals(1f, sets[Muscle.UpperBack]!!, 0.001f)
    }

    @Test
    fun `warmup and incomplete sets never count under sets metric`() {
        val sessions = listOf(
            day(1,
                set(exerciseId = 1, warmup = true),
                set(exerciseId = 1, completed = false),
            ),
        )
        assertTrue(MuscleVolume.computeForWindow(sessions, window, muscleMap, VolumeMetric.Sets).isEmpty())
    }

    @Test
    fun `out-of-window sets excluded under sets metric`() {
        val sessions = listOf(day(99, set(exerciseId = 1)))
        assertTrue(MuscleVolume.computeForWindow(sessions, window, muscleMap, VolumeMetric.Sets).isEmpty())
    }

    // --- Tonnage metric: unchanged behavior ---

    @Test
    fun `tonnage weights reps by load and applies secondary multiplier`() {
        val sessions = listOf(day(1, set(exerciseId = 2, reps = 5, weight = 100f)))
        val t = MuscleVolume.computeForWindow(sessions, window, muscleMap, VolumeMetric.Tonnage)
        assertEquals(500f, t[Muscle.UpperBack]!!, 0.001f)   // primary 1.0
        assertEquals(250f, t[Muscle.Lats]!!, 0.001f)        // secondary 0.5
    }

    @Test
    fun `bodyweight set counts surface separately regardless of metric`() {
        val sessions = listOf(day(1, set(exerciseId = 1, weight = null)))
        val bw = MuscleVolume.bodyweightSetCountsForWindow(sessions, window, muscleMap)
        assertEquals(1, bw[Muscle.Lats])
        assertEquals(1, bw[Muscle.Biceps])
    }

    @Test
    fun `normalize scales the max muscle to one`() {
        val raw = mapOf(Muscle.Lats to 3f, Muscle.Biceps to 1.5f)
        val n = MuscleVolume.normalize(raw)
        assertEquals(1f, n[Muscle.Lats]!!, 0.001f)
        assertEquals(0.5f, n[Muscle.Biceps]!!, 0.001f)
        assertFalse(n.containsKey(Muscle.Quads))
    }
}
