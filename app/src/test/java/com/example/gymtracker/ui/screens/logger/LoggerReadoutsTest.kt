package com.example.gymtracker.ui.screens.logger

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
}
