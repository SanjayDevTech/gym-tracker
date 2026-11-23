package com.example.gymtracker.domain

import com.example.gymtracker.data.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsTest {

    private fun set(
        reps: Int? = 5,
        weight: Float? = 100f,
        completed: Boolean = true,
        type: SetType = SetType.Working,
        day: Long = 0L,
    ) = LoggedSetSample(day, reps, weight, completed, type)

    @Test
    fun `first ever log is a record with no delta`() {
        val r = Records.forSession(emptyList<LoggedSetSample>(), listOf(set(reps = 5, weight = 60f)))
        assertTrue(r.hasPr)
        assertEquals(setOf(0), r.prSets.keys)
        assertEquals(PrType.OneRepMax, r.best!!.type)
        assertNull(r.best!!.delta)
    }

    @Test
    fun `heavier set beats history and reports the 1RM delta`() {
        val history = listOf(set(day = 1, reps = 5, weight = 100f))
        val r = Records.forSession(history, listOf(set(reps = 5, weight = 90f), set(reps = 5, weight = 105f)))
        assertEquals(setOf(1), r.prSets.keys)
        assertEquals(setOf(PrType.OneRepMax, PrType.Weight, PrType.Volume), r.prSets[1])
        val best = r.best!!
        assertEquals(PrType.OneRepMax, best.type)
        assertEquals(OneRepMax.epley(105f, 5), best.value, 0.001f)
        assertEquals(OneRepMax.epley(105f, 5) - OneRepMax.epley(100f, 5), best.delta!!, 0.001f)
    }

    @Test
    fun `volume alone can be the best record`() {
        // 1RM and weight out of reach; 10 × 60 = 600 beats the 500 volume record.
        val baseline = PersonalRecords(estimatedOneRepMaxKg = 200f, heaviestWeightKg = 150f, bestSetVolumeKg = 500f)
        val r = Records.forSession(baseline, listOf(set(reps = 10, weight = 60f)))
        assertEquals(setOf(PrType.Volume), r.prSets[0])
        assertEquals(PrType.Volume, r.best!!.type)
        assertEquals(100f, r.best!!.delta!!, 0.001f)
    }

    @Test
    fun `matching the baseline is not a record`() {
        val history = listOf(set(day = 1, reps = 5, weight = 100f))
        val r = Records.forSession(history, listOf(set(reps = 5, weight = 100f)))
        assertFalse(r.hasPr)
        assertNull(r.best)
    }

    @Test
    fun `warm-ups and unticked sets never count`() {
        val r = Records.forSession(
            emptyList<LoggedSetSample>(),
            listOf(set(type = SetType.Warmup), set(completed = false)),
        )
        assertFalse(r.hasPr)
    }

    @Test
    fun `bodyweight reps record`() {
        val history = listOf(set(day = 1, reps = 12, weight = null))
        val r = Records.forSession(history, listOf(set(reps = 15, weight = null)))
        assertEquals(PrType.Reps, r.best!!.type)
        assertEquals(3f, r.best!!.delta!!, 0.001f)
    }

    @Test
    fun `pr count is exercises with at least one record`() {
        val history = listOf(set(day = 1, reps = 5, weight = 100f))
        val twoRecordSets = Records.forSession(emptyList<LoggedSetSample>(), listOf(set(reps = 5, weight = 60f), set(reps = 20, weight = 40f)))
        val none = Records.forSession(history, listOf(set(reps = 5, weight = 80f)))
        val one = Records.forSession(history, listOf(set(reps = 6, weight = 100f)))
        assertTrue(twoRecordSets.prSets.size >= 2)
        assertEquals(2, Records.prCount(listOf(twoRecordSets, none, one)))
    }

    @Test
    fun `baseline is the history's all-time records`() {
        val history = listOf(set(day = 1, reps = 5, weight = 100f), set(day = 2, reps = 3, weight = 110f))
        val b = Records.baseline(history)
        assertEquals(110f, b.heaviestWeightKg!!, 0.001f)
        assertEquals(500f, b.bestSetVolumeKg!!, 0.001f)
    }
}
