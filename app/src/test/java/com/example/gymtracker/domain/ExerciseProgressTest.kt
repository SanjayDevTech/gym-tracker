package com.example.gymtracker.domain

import com.example.gymtracker.data.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseProgressTest {

    private fun sample(
        day: Long,
        reps: Int? = 5,
        weight: Float? = 100f,
        completed: Boolean = true,
        type: SetType = SetType.Working,
    ) = LoggedSetSample(day, reps, weight, completed, type)

    // --- OneRepMax ---

    @Test
    fun `epley single rep is its own max`() {
        assertEquals(100f, OneRepMax.epley(100f, 1), 0.001f)
    }

    @Test
    fun `epley extrapolates upward with reps`() {
        // 100 * (1 + 5/30) = 116.667
        assertEquals(116.667f, OneRepMax.epley(100f, 5), 0.01f)
    }

    // --- records ---

    @Test
    fun `empty history yields no records`() {
        val r = ExerciseProgress.records(emptyList())
        assertNull(r.estimatedOneRepMaxKg)
        assertNull(r.heaviestWeightKg)
        assertNull(r.bestSetVolumeKg)
        assertNull(r.maxReps)
    }

    @Test
    fun `records pick the bests across sets`() {
        val r = ExerciseProgress.records(
            listOf(
                sample(day = 1, reps = 5, weight = 100f),   // 1RM 116.7, vol 500
                sample(day = 2, reps = 10, weight = 80f),   // 1RM 106.7, vol 800, maxReps 10
                sample(day = 3, reps = 3, weight = 110f),   // 1RM 121, heaviest 110, vol 330
            ),
        )
        assertEquals(121f, r.estimatedOneRepMaxKg!!, 0.5f)
        assertEquals(110f, r.heaviestWeightKg!!, 0.001f)
        assertEquals(800f, r.bestSetVolumeKg!!, 0.001f)
        assertEquals(10, r.maxReps)
    }

    @Test
    fun `warmups and incomplete sets are excluded`() {
        val r = ExerciseProgress.records(
            listOf(
                sample(day = 1, reps = 20, weight = 200f, type = SetType.Warmup),   // ignored
                sample(day = 1, reps = 15, weight = 200f, completed = false),        // ignored
                sample(day = 1, reps = 5, weight = 100f),                            // counts
            ),
        )
        assertEquals(100f, r.heaviestWeightKg!!, 0.001f)
        assertEquals(5, r.maxReps)
    }

    @Test
    fun `bodyweight sets give max reps but no weight records`() {
        val r = ExerciseProgress.records(
            listOf(
                sample(day = 1, reps = 12, weight = null),
                sample(day = 2, reps = 15, weight = 0f),
            ),
        )
        assertNull(r.estimatedOneRepMaxKg)
        assertNull(r.heaviestWeightKg)
        assertNull(r.bestSetVolumeKg)
        assertEquals(15, r.maxReps)
    }

    // --- series ---

    @Test
    fun `series collapses to one point per day, best value, ascending`() {
        val points = ExerciseProgress.series(
            listOf(
                sample(day = 5, reps = 5, weight = 100f),
                sample(day = 5, reps = 3, weight = 110f),   // heaviest that day
                sample(day = 1, reps = 8, weight = 90f),
            ),
            ProgressMetric.Heaviest,
        )
        assertEquals(2, points.size)
        assertEquals(1L, points.first().dateEpochDay)   // ascending
        assertEquals(90f, points.first().value, 0.001f)
        assertEquals(110f, points.last().value, 0.001f)
    }

    @Test
    fun `weight-based series ignores bodyweight, reps series keeps them`() {
        val sets = listOf(
            sample(day = 1, reps = 10, weight = null),
            sample(day = 2, reps = 8, weight = 50f),
        )
        assertEquals(1, ExerciseProgress.series(sets, ProgressMetric.OneRepMax).size)
        assertEquals(2, ExerciseProgress.series(sets, ProgressMetric.Reps).size)
    }

    @Test
    fun `volume series is reps times weight`() {
        val points = ExerciseProgress.series(
            listOf(sample(day = 1, reps = 8, weight = 50f)),
            ProgressMetric.Volume,
        )
        assertEquals(400f, points.single().value, 0.001f)
    }

    // --- sessionPrs ---

    @Test
    fun `set beating heaviest gets weight and 1RM crowns on the best set`() {
        val baseline = PersonalRecords(
            estimatedOneRepMaxKg = 110f,
            heaviestWeightKg = 100f,
            bestSetVolumeKg = 800f,
            maxReps = 12,
        )
        val sets = listOf(
            sample(day = 0, reps = 5, weight = 100f),   // ties old heaviest, no crown
            sample(day = 0, reps = 5, weight = 120f),   // beats heaviest + 1RM
        )
        val prs = ExerciseProgress.sessionPrs(baseline, sets)
        assertTrue(prs[1]!!.contains(PrType.Weight))
        assertTrue(prs[1]!!.contains(PrType.OneRepMax))
        assertNull(prs[0])
    }

    @Test
    fun `only one set is crowned per metric - the best`() {
        val baseline = PersonalRecords(heaviestWeightKg = 50f)
        val sets = listOf(
            sample(day = 0, reps = 5, weight = 60f),
            sample(day = 0, reps = 5, weight = 80f),   // the best — only this one crowned for Weight
        )
        val prs = ExerciseProgress.sessionPrs(baseline, sets)
        assertNull(prs[0])
        assertTrue(prs[1]!!.contains(PrType.Weight))
    }

    @Test
    fun `equalling the record is not a PR`() {
        val baseline = PersonalRecords(heaviestWeightKg = 100f, estimatedOneRepMaxKg = 116.7f, bestSetVolumeKg = 500f)
        val sets = listOf(sample(day = 0, reps = 5, weight = 100f)) // exactly the old bests
        assertTrue(ExerciseProgress.sessionPrs(baseline, sets).isEmpty())
    }

    @Test
    fun `first ever log with empty baseline is a PR`() {
        val prs = ExerciseProgress.sessionPrs(PersonalRecords(), listOf(sample(day = 0, reps = 5, weight = 60f)))
        assertTrue(prs[0]!!.contains(PrType.Weight))
        assertTrue(prs[0]!!.contains(PrType.OneRepMax))
    }

    @Test
    fun `warmups and incomplete sets never earn crowns`() {
        val baseline = PersonalRecords()
        val sets = listOf(
            sample(day = 0, reps = 5, weight = 200f, type = SetType.Warmup),
            sample(day = 0, reps = 5, weight = 200f, completed = false),
        )
        assertTrue(ExerciseProgress.sessionPrs(baseline, sets).isEmpty())
    }

    @Test
    fun `bodyweight reps PR awarded when no weighted sets`() {
        val baseline = PersonalRecords(maxReps = 10)
        val sets = listOf(sample(day = 0, reps = 14, weight = null))
        val prs = ExerciseProgress.sessionPrs(baseline, sets)
        assertTrue(prs[0]!!.contains(PrType.Reps))
    }

    @Test
    fun `no reps crown when weighted sets present`() {
        val baseline = PersonalRecords(maxReps = 5, heaviestWeightKg = 50f)
        val sets = listOf(sample(day = 0, reps = 20, weight = 60f))
        val prs = ExerciseProgress.sessionPrs(baseline, sets)
        assertTrue(prs[0]!!.contains(PrType.Weight))
        assertTrue(!prs[0]!!.contains(PrType.Reps))
    }

    @Test
    fun `no scoring sets yields empty series`() {
        assertTrue(
            ExerciseProgress.series(
                listOf(sample(day = 1, type = SetType.Warmup)),
                ProgressMetric.OneRepMax,
            ).isEmpty(),
        )
    }
}
