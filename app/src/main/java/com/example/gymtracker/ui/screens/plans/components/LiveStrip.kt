package com.example.gymtracker.ui.screens.plans.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.screens.plans.LiveSession
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Live strip — design-plan §3 `LiveStrip` (S5-d), was the Resume banner. No card: a
 * `LIVE · PUSH A` eyebrow in primary with the elapsed clock ticking every second, a `bodyS` line
 * of sets done and rest state, then a full-width Resume button.
 */
@Composable
fun LiveStrip(
    live: LiveSession,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(live.startedAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - now % 1_000L)
        }
    }
    val detail = buildString {
        append("${live.doneSets} of ${live.totalSets} sets")
        if (live.lastSetCompletedAtMillis > 0L) {
            append(" · resting ")
            append(formatClock(now - live.lastSetCompletedAtMillis))
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Live · ${live.title}", color = scheme.primary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.m))
            Text(formatClock(now - live.startedAtMillis), style = AppText.numberM, color = scheme.onSurface)
        }
        Text(detail, style = AppText.bodyS, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.m))
        AppButton(
            text = "Resume",
            onClick = onResume,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
