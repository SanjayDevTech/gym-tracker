package com.example.gymtracker.session

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the Session logger is on screen and resumed — the one fact [SessionTrackingService]
 * needs to decide if the Rest timer notification should vibrate (§4.18: only when the logger is
 * not in the foreground; in the logger the dock's own haptic covers it).
 *
 * Process-wide and in-memory: it resets to false with the process, which is the right answer
 * for a service that outlives the UI.
 */
object LoggerPresence {
    private val _resumed = MutableStateFlow(false)
    val resumed: StateFlow<Boolean> = _resumed.asStateFlow()

    internal fun set(value: Boolean) {
        _resumed.value = value
    }
}

/**
 * Call once from the logger's destination. Tracks that back-stack entry's lifecycle, so a pushed
 * page (picker, exercise detail), the home button and screen-off all count as "not resumed".
 */
@Composable
fun ReportLoggerPresence() {
    LifecycleResumeEffect(Unit) {
        LoggerPresence.set(true)
        onPauseOrDispose { LoggerPresence.set(false) }
    }
}
