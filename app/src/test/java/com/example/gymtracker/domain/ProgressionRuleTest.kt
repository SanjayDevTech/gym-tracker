package com.example.gymtracker.domain

import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionRuleTest {

    private val plan = PlannedExerciseEntity(
        id = 1L,
        planId = 1L,
        exerciseId = 1L,
        sortIndex = 0,
        targetSets = 3,
        targetRepsLow = 8,
        targetRepsHigh = 12,
    )

    private fun set(
        reps: Int,
        weight: Float? = 60f,
        completed: Boolean = true,
        type: SetType = SetType.Working,
        index: Int = 0,
    ) = LoggedSetEntity(
        sessionId = 0L,
        exerciseId = 1L,
        setIndex = index,
        repsDone = reps,
        weightKg = weight,
        isCompleted = completed,
        setType = type,
    )

    @Test
    fun `clean ceiling on both sessions is ready`() {
        val recent = listOf(
            listOf(set(12), set(12)), // newest
            listOf(set(12), set(12)),
        )
        assertTrue(ProgressionRule.readyToProgress(plan, recent))
    }

    @Test
    fun `failure in latest session holds the weight`() {
        val recent = listOf(
            listOf(set(12), set(12, type = SetType.Failure)), // newest — ground to failure
            listOf(set(12), set(12)),
        )
        assertFalse(ProgressionRule.readyToProgress(plan, recent))
    }

    @Test
    fun `failure only in older session still progresses`() {
        val recent = listOf(
            listOf(set(12), set(12)), // newest — clean
            listOf(set(12), set(12, type = SetType.Failure)),
        )
        assertTrue(ProgressionRule.readyToProgress(plan, recent))
    }

    @Test
    fun `warmup sets are ignored when judging readiness`() {
        val recent = listOf(
            listOf(set(5, weight = 40f, type = SetType.Warmup), set(12), set(12)),
            listOf(set(5, weight = 40f, type = SetType.Warmup), set(12), set(12)),
        )
        assertTrue(ProgressionRule.readyToProgress(plan, recent))
    }

    @Test
    fun `below ceiling is not ready`() {
        val recent = listOf(
            listOf(set(10), set(9)),
            listOf(set(12), set(12)),
        )
        assertFalse(ProgressionRule.readyToProgress(plan, recent))
    }

    @Test
    fun `fewer than n sessions is not ready`() {
        val recent = listOf(listOf(set(12), set(12)))
        assertFalse(ProgressionRule.readyToProgress(plan, recent))
    }

    @Test
    fun `bodyweight falls back to rep-only rule`() {
        val recent = listOf(
            listOf(set(12, weight = null), set(12, weight = null)),
            listOf(set(12, weight = null), set(12, weight = null)),
        )
        assertTrue(ProgressionRule.readyToProgress(plan, recent))
    }
}
