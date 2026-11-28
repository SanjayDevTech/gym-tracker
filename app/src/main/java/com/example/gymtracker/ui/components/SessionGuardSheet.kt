package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gymtracker.data.db.SessionDraftEntity
import com.example.gymtracker.data.db.SessionDraftPayload
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.serialization.json.Json

/** What the guard needs to say about the draft that is in the way. */
data class RunningDraft(
    val planId: Long?,
    val title: String,
    val startedAtMillis: Long,
    val doneSets: Int,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun from(draft: SessionDraftEntity): RunningDraft {
            val payload = runCatching {
                json.decodeFromString(SessionDraftPayload.serializer(), draft.payloadJson)
            }.getOrNull()
            return RunningDraft(
                planId = draft.planId,
                title = payload?.planName ?: "Quick log",
                startedAtMillis = draft.startedAtMillis,
                doneSets = payload?.groups?.sumOf { g -> g.sets.count { it.isCompleted } } ?: 0,
            )
        }
    }
}

/**
 * Session guard. Raised by Start on a plan, the editor's Start workout, or
 * Quick log while a *different* workout is already running. Resume is the primary action; discard
 * is a second state of the same sheet rather than a sheet over a sheet.
 *
 * @param startingTitle what the user tried to start ("Pull B", "a quick log").
 */
@Composable
fun SessionGuardSheet(
    running: RunningDraft,
    startingTitle: String,
    onResume: () -> Unit,
    onDiscardAndStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val minutes = ((System.currentTimeMillis() - running.startedAtMillis) / 60_000L).coerceAtLeast(0)
    val sets = "${running.doneSets} ${plural(running.doneSets, "set")}"

    AppSheet(onDismiss = onDismiss) {
        SheetTitleRow(if (confirming) "Discard $sets?" else "A workout is already running", onClose = onDismiss)
        Text(
            if (confirming) "${running.title} is lost, then $startingTitle starts."
            else "${running.title} · $minutes min · $sets",
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (confirming) {
            AppButton(
                text = "Discard & start",
                onClick = onDiscardAndStart,
                destructive = true,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
            )
            AppOutlinedButton(
                text = "Cancel",
                onClick = { confirming = false },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
            )
        } else {
            AppButton(
                text = "Resume ${running.title}",
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
            )
            AppOutlinedButton(
                text = "Discard it and start $startingTitle",
                onClick = { if (running.doneSets == 0) onDiscardAndStart() else confirming = true },
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
            )
        }
    }
}
