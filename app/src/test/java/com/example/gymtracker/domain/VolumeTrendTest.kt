package com.example.gymtracker.domain

import com.example.gymtracker.data.VolumeMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class VolumeTrendTest {
    // 2026-11-23 is a Monday.
    private val monday = LocalDate.of(2026, 11, 23).toEpochDay()

    private fun set(reps: Int? = 10, weight: Float? = 50f, completed: Boolean = true, warmup: Boolean = false) =
        SessionWindowSet(exerciseId = 1, repsDone = reps, weightKg = weight, isCompleted = completed, isWarmup = warmup)

    private fun session(day: Long, vararg sets: SessionWindowSet) = SessionWindow(day, sets.toList())

    private fun sets(n: Int) = Array(n) { set() }

    @Test
    fun `total counts completed working sets once and skips warm-ups and unticked`() {
        val sessions = listOf(session(monday, set(), set(), set(warmup = true), set(completed = false)))
        assertEquals(2f, VolumeTrend.total(sessions, monday..monday, VolumeMetric.Sets), 0.001f)
        assertEquals(1000f, VolumeTrend.total(sessions, monday..monday, VolumeMetric.Tonnage), 0.001f)
    }

    @Test
    fun `tonnage ignores bodyweight sets`() {
        val sessions = listOf(session(monday, set(weight = null), set(reps = 5, weight = 100f)))
        assertEquals(500f, VolumeTrend.total(sessions, monday..monday, VolumeMetric.Tonnage), 0.001f)
    }

    @Test
    fun `reference is the weekly average of the four weeks before, scaled to the period`() {
        val sessions = (1..4).map { w -> session(monday - w * 7L, *sets(8)) }
        val week = monday..(monday + 6)
        assertEquals(8f, VolumeTrend.referenceFor(sessions, week, VolumeMetric.Sets)!!, 0.001f)
        val day = monday..monday
        assertEquals(8f / 7f, VolumeTrend.referenceFor(sessions, day, VolumeMetric.Sets)!!, 0.001f)
        assertNull(VolumeTrend.referenceFor(emptyList(), week, VolumeMetric.Sets))
    }

    @Test
    fun `weekly bars compare each week with the four before it`() {
        val sessions = listOf(
            session(monday - 28, *sets(4)),
            session(monday - 21, *sets(4)),
            session(monday - 14, *sets(4)),
            session(monday - 7, *sets(4)),
            session(monday + 2, *sets(5)),
        )
        val bars = VolumeTrend.weeklyBars(sessions, monday - 7, monday + 3, VolumeMetric.Sets)
        assertEquals(listOf(monday - 7, monday), bars.map { it.weekStart })
        assertEquals(5f, bars.last().total, 0.001f)
        assertEquals(4f, bars.last().priorAverage, 0.001f)
        assertEquals(0.25f, bars.last().changeVsPrior!!, 0.001f)
        // The first bar has three non-empty weeks before it: (0 + 4 + 4 + 4) / 4 = 3.
        assertEquals(3f, bars.first().priorAverage, 0.001f)
    }

    @Test
    fun `change is null without a reference`() {
        assertNull(VolumeTrend.change(10f, null))
        assertNull(VolumeTrend.change(10f, 0f))
        assertEquals(-0.5f, VolumeTrend.change(5f, 10f)!!, 0.001f)
    }
}
