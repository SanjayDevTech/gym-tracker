package com.example.gymtracker.ui.screens.logger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Nav-facing entry for the logger: owns the ViewModel (scoped to the back-stack entry) and feeds
 * it the exercise picker's result. [pickedExerciseIds] is the picker's one-shot result; call
 * [onPickedHandled] once it is applied so it is not applied twice.
 *
 * System back minimises exactly like the ↓ button, so the caller's minimise handling (Train's
 * "still running" snackbar) sees both. On the first workout ever it also asks for the
 * notification permission (§6.1 step 3) — here, with a reason, rather than at launch.
 */
@Composable
fun SessionLoggerHost(
    planId: Long?,
    pickedExerciseIds: List<Long>?,
    onPickedHandled: () -> Unit,
    onMinimise: () -> Unit,
    onDiscarded: () -> Unit,
    onFinished: (sessionId: Long) -> Unit,
    onAddExercises: () -> Unit,
    onOpenExercise: (exerciseId: Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SessionLoggerViewModel = viewModel(
        key = "logger-$planId",
        factory = AppViewModelFactory(
            mapOf(SessionLoggerViewModel::class.java to {
                SessionLoggerViewModel(container.repository, container.prefs, container.applicationScope, planId)
            })
        )
    )
    LaunchedEffect(pickedExerciseIds) {
        if (pickedExerciseIds != null) {
            viewModel.addExercises(pickedExerciseIds)
            onPickedHandled()
        }
    }
    BackHandler(onBack = onMinimise)
    NotificationPermissionPrompt()
    LoggerScreen(
        viewModel = viewModel,
        onMinimise = onMinimise,
        onDiscarded = onDiscarded,
        onFinished = onFinished,
        onAddExercises = onAddExercises,
        onOpenExercise = onOpenExercise,
    )
}

/** Asks once, ever, on API 33+ when POST_NOTIFICATIONS is not yet granted. */
@Composable
private fun NotificationPermissionPrompt() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val prefs = LocalAppContainer.current.prefs
    val scope = rememberCoroutineScope()
    var show by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Either way the service runs; only the notification's visibility depends on the grant.
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !prefs.notificationPermissionAsked.first()) show = true
    }
    if (!show) return

    fun close(request: Boolean) {
        show = false
        scope.launch { prefs.setNotificationPermissionAsked() }
        if (request) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    AppSheet(onDismiss = { close(request = false) }) {
        Text("Keep the workout in your notifications", style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.s))
        Text(
            "The clock and rest timer stay a glance away while the app is closed.",
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppButton(
            text = "Allow notifications",
            onClick = { close(request = true) },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
        )
        AppTextButton(
            text = "Not now",
            onClick = { close(request = false) },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        )
    }
}
