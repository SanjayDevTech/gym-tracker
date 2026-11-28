package com.example.gymtracker.ui.screens.logger

import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggerReadoutsTest {

    @Test fun elapsedReadsZeroUntilTheSessionHasAStart() {
        assertEquals("0:00", elapsedLabel(nowMillis = 90_000L, startedAtMillis = 0L))
        assertEquals("1:30", elapsedLabel(nowMillis = 100_000L, startedAtMillis = 10_000L))
        assertEquals("1:00:05", elapsedLabel(nowMillis = 3_606_000L, startedAtMillis = 1_000L))
    }

    @Test fun restIsNullWhileClearedAndNeverNegative() {
        assertNull(restElapsedMillis(nowMillis = 5_000L, lastSetCompletedAtMillis = 0L))
        assertEquals(4_000L, restElapsedMillis(nowMillis = 5_000L, lastSetCompletedAtMillis = 1_000L))
        // A set ticked "after" the last clock reading (the clock refreshes once a second).
        assertEquals(0L, restElapsedMillis(nowMillis = 5_000L, lastSetCompletedAtMillis = 5_400L))
    }

    @Test fun restReachesItsTargetAtExactlyTheTarget() {
        assertFalse(restReached(restElapsedMillis = null, targetSeconds = 90))
        assertFalse(restReached(restElapsedMillis = 89_999L, targetSeconds = 90))
        assertTrue(restReached(restElapsedMillis = 90_000L, targetSeconds = 90))
        // Back from a locked screen long past the target: still reached (the haptic's armed
        // flag decides whether it buzzes).
        assertTrue(restReached(restElapsedMillis = 600_000L, targetSeconds = 90))
    }

    @Test fun prKeysSplitIntoSetIndexesPerGroup() {
        val byGroup = prSetIndexesByGroup(setOf("a-1#0", "a-1#2", "b-2#1"))
        assertEquals(mapOf("a-1" to setOf(0, 2), "b-2" to setOf(1)), byGroup)
        assertTrue(prSetIndexesByGroup(emptySet()).isEmpty())
    }

    @Test fun restClockCountsDownThenOver() {
        assertEquals("1:30", restClockLabel(restElapsedMillis = null, targetSeconds = 90))
        assertEquals("1:30", restClockLabel(restElapsedMillis = 0L, targetSeconds = 90))
        assertEquals("0:01", restClockLabel(restElapsedMillis = 89_500L, targetSeconds = 90))
        assertEquals("+0:00", restClockLabel(restElapsedMillis = 90_000L, targetSeconds = 90))
        assertEquals("+0:12", restClockLabel(restElapsedMillis = 102_000L, targetSeconds = 90))
        assertEquals(0.5f, restRemainingFraction(restElapsedMillis = 45_000L, targetSeconds = 90), 0.001f)
        assertEquals(0f, restRemainingFraction(restElapsedMillis = 120_000L, targetSeconds = 90), 0.001f)
        assertEquals(0f, restRemainingFraction(restElapsedMillis = null, targetSeconds = 90), 0.001f)
    }

    @Test fun nextSetIsTheFirstUntickedSetWithTypedOrHintedLoad() {
        fun group(name: String, type: MovementType, sets: List<SetDraft>, skipped: Boolean = false) =
            ExerciseGroup(uid = name, exerciseId = 1, exerciseName = name, movementType = type,
                targetSets = sets.size, targetRepsLow = 8, targetRepsHigh = 12, sets = sets, skipped = skipped)
        val groups = listOf(
            group("Squat", MovementType.Compound, listOf(SetDraft(isCompleted = true), SetDraft(isCompleted = true))),
            group("Curl", MovementType.Isolation, listOf(SetDraft()), skipped = true),
            group("Bench", MovementType.Compound, listOf(SetDraft(isCompleted = true), SetDraft(weightInput = "60", hintReps = 8))),
        )
        assertEquals("Bench · set 2 · 60 kg × 8 reps", nextSetLabel(groups, WeightUnit.Kg))
        val plank = listOf(group("Plank", MovementType.Mobility, listOf(SetDraft(repsInput = "45"))))
        assertEquals("Plank · set 1 · 45 sec", nextSetLabel(plank, WeightUnit.Kg))
        assertEquals(null, nextSetLabel(groups.take(2), WeightUnit.Kg))
    }
}
