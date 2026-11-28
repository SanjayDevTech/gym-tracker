package com.example.gymtracker.ui.screens.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ResumeClocksTest {
    @Test
    fun resumingSlidesBothClocksByThePause() {
        val paused = SessionLoggerState(startedAtMillis = 1_000L, lastSetCompletedAtMillis = 5_000L, pausedAtMillis = 8_000L)
        val resumed = resumeClocks(paused, now = 11_000L) // paused for 3 s
        assertEquals(4_000L, resumed.startedAtMillis)
        assertEquals(8_000L, resumed.lastSetCompletedAtMillis)
        assertEquals(0L, resumed.pausedAtMillis)
    }

    @Test
    fun clearedRestStaysClearedAndRunningStateIsUntouched() {
        val paused = SessionLoggerState(startedAtMillis = 1_000L, lastSetCompletedAtMillis = 0L, pausedAtMillis = 2_000L)
        assertEquals(0L, resumeClocks(paused, now = 9_000L).lastSetCompletedAtMillis)

        val running = SessionLoggerState(startedAtMillis = 1_000L, lastSetCompletedAtMillis = 5_000L)
        assertSame(running, resumeClocks(running, now = 9_000L))
    }
}
