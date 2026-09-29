package com.example.gymtracker.ui.screens.logger

import androidx.compose.runtime.Composable

@Composable
fun SessionLoggerHost(
    planId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    SessionLoggerScreen(
        planId = planId,
        onBack = onBack,
        onSaved = onSaved,
    )
}
