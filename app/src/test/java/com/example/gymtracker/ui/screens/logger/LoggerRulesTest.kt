package com.example.gymtracker.ui.screens.logger

import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.db.SessionDraftPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggerRulesTest {

    private fun group(vararg done: Boolean, skipped: Boolean = false, uid: String = "g") = ExerciseGroup(
        uid = uid,
        exerciseId = 1L,
        exerciseName = "Bench press",
        movementType = MovementType.Compound,
        targetSets = done.size,
        targetRepsLow = 8,
        targetRepsHigh = 12,
        sets = done.map { SetDraft(hintReps = 10, isCompleted = it) },
        skipped = skipped,
    )

    // ---- Auto-tick rule ---------------------------------------------------

    @Test fun typingAutoTicksASetThatWouldNotCompleteTheGroup() {
        val (g, outcome) = group(false, false, false).withInput(0) { it.copy(repsInput = "9") }
        assertEquals(InputOutcome.Ticked, outcome)
        assertTrue(g.sets[0].isCompleted)
        assertEquals("9", g.sets[0].repsInput)
    }

    @Test fun typingNeverTicksTheOnlyRemainingUntickedSet() {
        val (g, outcome) = group(true, true, false).withInput(2) { it.copy(weightInput = "60") }
        assertEquals(InputOutcome.NudgeTick, outcome)
        assertFalse(g.sets[2].isCompleted)
        assertFalse(g.allDone)
        assertEquals("60", g.sets[2].weightInput)
    }

    @Test fun theRemainingSetNeedNotBeTheLastOne() {
        val (g, outcome) = group(false, true, true).withInput(0) { it.copy(repsInput = "8") }
        assertEquals(InputOutcome.NudgeTick, outcome)
        assertFalse(g.sets[0].isCompleted)
    }

    @Test fun onlyTheFirstInputIntoThatSetNudges() {
        val (g1, first) = group(true, false).withInput(1) { it.copy(repsInput = "1") }
        val (g2, second) = g1.withInput(1) { it.copy(repsInput = "10") }
        assertEquals(InputOutcome.NudgeTick, first)
        assertEquals(InputOutcome.None, second)
        assertFalse(g2.sets[1].isCompleted)
    }

    @Test fun aSingleSetExerciseIsNeverAutoTicked() {
        val (g, outcome) = group(false).withInput(0) { it.copy(repsInput = "25") }
        assertEquals(InputOutcome.NudgeTick, outcome)
        assertFalse(g.sets[0].isCompleted)
    }

    @Test fun clearingAFieldDoesNothing() {
        val (g, outcome) = group(false, false).withInput(0) { it.copy(repsInput = "") }
        assertEquals(InputOutcome.None, outcome)
        assertFalse(g.sets[0].isCompleted)
    }

    @Test fun editingATickedSetKeepsItTicked() {
        val (g, outcome) = group(true, false).withInput(0) { it.copy(repsInput = "11") }
        assertEquals(InputOutcome.None, outcome)
        assertTrue(g.sets[0].isCompleted)
    }

    // ---- Skip / unskip ------------------------------------------------------

    @Test fun aSkippedGroupIsSettledAndCountsOnlyItsTickedSets() {
        val g = group(true, false, false, skipped = true)
        assertTrue(g.settled)
        assertFalse(g.allDone)
        assertEquals(1, g.countedSets)
    }

    @Test fun tickingAnySetUnskips() {
        val g = group(false, false, skipped = true)
        val ticked = g.withSet(1, g.sets[1].copy(isCompleted = true))
        assertFalse(ticked.skipped)
        assertFalse(ticked.settled)
    }

    @Test fun editingWithoutTickingKeepsTheSkip() {
        val g = group(false, false, skipped = true)
        val edited = g.withSet(0, g.sets[0].copy(weightInput = "50"))
        assertTrue(edited.skipped)
    }

    @Test fun typingThatAutoTicksUnskips() {
        val (g, outcome) = group(false, false, false, skipped = true).withInput(0) { it.copy(repsInput = "9") }
        assertEquals(InputOutcome.Ticked, outcome)
        assertFalse(g.skipped)
    }

    @Test fun progressExcludesASkippedExercisesUntickedSets() {
        val s = SessionLoggerState(
            groups = listOf(
                group(true, true, false, uid = "a"),
                group(true, false, false, skipped = true, uid = "b"),
            ),
        )
        assertEquals(3, s.doneSets)
        assertEquals(4, s.totalSets) // 3 + the skipped one's 1 ticked set
        assertEquals(6, s.allSets)
        assertEquals(3, s.skippedSets)
    }

    @Test fun oldDraftsWithoutTheSkipFlagStillLoad() {
        val old = """{"dateEpochDay":20000,"groups":[{"exerciseId":1,"exerciseName":"Bench",""" +
            """"movementType":"Compound","targetRepsLow":8,"targetRepsHigh":12,"sets":[{}],"uid":"x"}]}"""
        val payload = Json { ignoreUnknownKeys = true }.decodeFromString(SessionDraftPayload.serializer(), old)
        assertFalse(payload.groups.single().skipped)
    }

    // ---- Discard / Finish -----------------------------------------------------

    @Test fun discardUntilASetIsTickedThenFinish() {
        assertEquals(PrimaryAction.Discard, SessionLoggerState().primaryAction)
        assertEquals(PrimaryAction.Discard, SessionLoggerState(groups = listOf(group(false, false))).primaryAction)
        assertEquals(PrimaryAction.Finish, SessionLoggerState(groups = listOf(group(true, false))).primaryAction)
    }

    @Test fun untickingEverythingSwitchesBackToDiscard() {
        val g = group(true, false)
        val unticked = g.withSet(0, g.sets[0].copy(isCompleted = false))
        assertEquals(PrimaryAction.Discard, SessionLoggerState(groups = listOf(unticked)).primaryAction)
    }

    @Test fun aSkippedExerciseDoesNotBlockFinish() {
        val s = SessionLoggerState(groups = listOf(group(true, true, uid = "a"), group(false, false, skipped = true, uid = "b")))
        assertEquals(PrimaryAction.Finish, s.primaryAction)
    }
}
