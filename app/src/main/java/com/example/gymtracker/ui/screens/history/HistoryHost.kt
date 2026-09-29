package com.example.gymtracker.ui.screens.history

import androidx.compose.runtime.Composable

@Composable
fun HistoryHost(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    HistoryScreen(onBack = onBack, onOpenSession = onOpenSession)
}

@Composable
fun SessionDetailHost(
    sessionId: Long,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    SessionDetailScreen(sessionId = sessionId, onBack = onBack, onOpenExercise = onOpenExercise)
}
