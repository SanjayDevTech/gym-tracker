package com.example.gymtracker.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * App haptics backed by the system [Vibrator] (we hold the VIBRATE permission), so feedback is
 * actually felt — the Compose [androidx.compose.ui.platform.LocalHapticFeedback] taps were far too
 * faint on most devices. Every cue is gated by the user's "Haptics" preference.
 */
@Immutable
class AppHaptics(
    /** Subtle tick — selection, navigation, light confirmations. */
    val light: () -> Unit,
    /** Firm cue — saving, completing a session. */
    val confirm: () -> Unit,
    /** Crisp click — reordering items, marking a set complete. */
    val reorder: () -> Unit,
    /** Toggle cue — checkbox / set-type changes. */
    val toggle: () -> Unit,
)

@Composable
fun rememberAppHaptics(): AppHaptics {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val enabled by container.prefs.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val vibrator = remember(context) { context.obtainVibrator() }
    return remember(vibrator, enabled) {
        AppHaptics(
            light = { if (enabled) vibrator.play(VibrationEffect.EFFECT_TICK) },
            confirm = { if (enabled) vibrator.play(VibrationEffect.EFFECT_HEAVY_CLICK) },
            reorder = { if (enabled) vibrator.play(VibrationEffect.EFFECT_CLICK) },
            toggle = { if (enabled) vibrator.play(VibrationEffect.EFFECT_TICK) },
        )
    }
}

private fun Context.obtainVibrator(): Vibrator {
    val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    return manager.defaultVibrator
}

private fun Vibrator.play(effectId: Int) {
    if (!hasVibrator()) return
    runCatching { vibrate(VibrationEffect.createPredefined(effectId)) }
}
