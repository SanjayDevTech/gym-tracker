package com.example.gymtracker.ui.screens.plans

import com.example.gymtracker.data.MuscleGroup
import com.example.gymtracker.data.db.SessionVolume
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.screens.plans.components.agoLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanStatsTest {
    private var nextId = 1L

    private fun session(
        planId: Long?,
        day: Long,
        volume: Double = 0.0,
        sets: Int = 0,
        minutes: Long? = null,
    ): SessionVolume {
        val logged = day * 86_400_000L + 3_600_000L
        return SessionVolume(
            sessionId = nextId++,
            planId = planId,
            dateEpochDay = day,
            loggedAtMillis = logged,
            startedAtMillis = minutes?.let { logged - it * 60_000L },
            volumeKg = volume,
            doneSets = sets,
        )
    }

    @Test
    fun medianIgnoresUntimedSessions() {
        val rows = listOf(
            session(1, 10, minutes = 40),
            session(1, 11, minutes = null),
            session(1, 12, minutes = 60),
            session(1, 13, minutes = 50),
        )
        assertEquals(50 * 60_000L, PlanStats.medianDurationMillis(rows))
    }

    @Test
    fun medianOfEvenCountAveragesTheMiddleTwo() {
        val rows = listOf(session(1, 10, minutes = 40), session(1, 11, minutes = 50))
        assertEquals(45 * 60_000L, PlanStats.medianDurationMillis(rows))
    }

    @Test
    fun medianIsNullWithoutTimedSessions() {
        assertNull(PlanStats.medianDurationMillis(listOf(session(1, 10))))
        assertNull(PlanStats.medianDurationMillis(emptyList()))
    }

    @Test
    fun recentVolumesKeepTheLastEightPerPlanOldestFirst() {
        val rows = (1L..10L).map { session(1, it, volume = it * 100.0) } +
            session(2, 5, volume = 7.0) +
            session(null, 6, volume = 9.0)
        val byPlan = PlanStats.recentVolumesByPlan(rows)
        assertEquals((3L..10L).map { it * 100.0 }, byPlan[1])
        assertEquals(listOf(7.0), byPlan[2])
        assertEquals(setOf(1L, 2L), byPlan.keys)
    }

    @Test
    fun lastSevenDaysIncludesTodayAndSixBefore() {
        val rows = listOf(
            session(1, 93, volume = 1000.0, sets = 9), // 7 days before today: out
            session(1, 94, volume = 200.0, sets = 3),
            session(null, 100, volume = 50.0, sets = 2),
        )
        assertEquals(250.0 to 5, PlanStats.lastSevenDays(rows, today = 100))
    }

    @Test
    fun agoLabels() {
        assertEquals("today", agoLabel(100, today = 100))
        assertEquals("yesterday", agoLabel(99, today = 100))
        assertEquals("3 days ago", agoLabel(97, today = 100))
        assertEquals("2 weeks ago", agoLabel(86, today = 100))
        assertEquals("1 year ago", agoLabel(0, today = 400))
    }

    @Test
    fun metaLines() {
        val n = NBSP
        assertEquals("No exercises", PlanStats.metaLine(0, null))
        assertEquals(
            "6 exercises · Chest, Shoulders · ~52${n}min",
            PlanStats.metaLine(6, 52 * 60_000L, muscles = listOf(MuscleGroup.Chest, MuscleGroup.Shoulders, MuscleGroup.Arms)),
        )
        assertEquals("1 exercise · 1 set", PlanStats.metaLine(1, null, sets = 1))
        assertEquals("No exercises · 0 sets", PlanStats.metaLine(0, null, sets = 0))
    }
}
