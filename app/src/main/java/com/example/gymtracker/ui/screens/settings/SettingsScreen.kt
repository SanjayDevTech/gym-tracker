package com.example.gymtracker.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.Stepper
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.components.atoms.ValueText
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

private val RestPresets: List<Int?> = listOf(60, 90, 120, 180)

private enum class SettingsSheet { WeeklyTarget, DefaultRest }

/**
 * Settings — design-plan §4.14. Four sections of rows (house rule 1): UNITS, WORKOUT, DATA,
 * ABOUT. A row is a label, its current value and a chevron (or the control itself), so every
 * setting reads without opening anything. Weekly target and Default rest open a one-stepper
 * sheet; the plan/backup file operations live on their own page ([onOpenData], §4.15).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }

    Scaffold(
        topBar = { AppTopBar(title = "Settings", onBack = onBack) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Section("Units") {
                SettingRowFrame("Weight") {
                    UnderlineSelector(
                        options = WeightUnit.entries.toList(),
                        selected = state.weightUnit,
                        onSelect = viewModel::setWeightUnit,
                        label = { it.label },
                    )
                }
                SettingRowFrame("Volume") {
                    UnderlineSelector(
                        options = VolumeMetric.entries.toList(),
                        selected = state.volumeMetric,
                        onSelect = viewModel::setVolumeMetric,
                        label = { it.label },
                    )
                }
                Text(
                    when (state.volumeMetric) {
                        VolumeMetric.Sets ->
                            "Counts each working set once, so bodyweight work (pull-ups, planks) counts fully."
                        VolumeMetric.Tonnage ->
                            "Weighs reps × load, failure sets included. Bodyweight sets add nothing."
                    },
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
            }

            Section("Workout") {
                SettingRow(
                    label = "Weekly target",
                    value = state.weeklyTargetSessions.toString(),
                    unit = plural(state.weeklyTargetSessions, "session"),
                    onClick = { sheet = SettingsSheet.WeeklyTarget },
                )
                SettingRow(
                    label = "Default rest",
                    value = formatClock(state.defaultRestSeconds * 1000L),
                    onClick = { sheet = SettingsSheet.DefaultRest },
                )
                SettingRowFrame("Haptics") {
                    Switch(checked = state.haptics, onCheckedChange = { viewModel.setHaptics(it) })
                }
            }

            Section("Data") {
                SettingRow(label = "Plans & backup", onClick = onOpenData)
            }

            Section("About") {
                SettingRowFrame("Version") {
                    Text(version, style = AppText.body, color = scheme.onSurfaceVariant)
                }
            }
        }
    }

    when (sheet) {
        SettingsSheet.WeeklyTarget -> StepperSheet(
            title = "Weekly target",
            message = "Sessions a week that count as a week on target.",
            initial = state.weeklyTargetSessions,
            range = WeeklyTarget.MIN_TARGET..WeeklyTarget.MAX_TARGET,
            step = 1,
            format = { it.toString() },
            decrementLabel = "Fewer sessions",
            incrementLabel = "More sessions",
            onSave = { viewModel.setWeeklyTargetSessions(it); sheet = null },
            onDismiss = { sheet = null },
        )
        SettingsSheet.DefaultRest -> StepperSheet(
            title = "Default rest",
            message = "The rest target every new workout starts with. Change it per workout from the logger.",
            initial = state.defaultRestSeconds,
            range = SessionLoggerViewModel.REST_MIN_SECONDS..SessionLoggerViewModel.REST_MAX_SECONDS,
            step = SessionLoggerViewModel.REST_STEP_SECONDS,
            format = { formatClock(it * 1000L) },
            presets = RestPresets,
            decrementLabel = "Shorter rest",
            incrementLabel = "Longer rest",
            onSave = { viewModel.setDefaultRestSeconds(it); sheet = null },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

/** A row that opens something: label · value (+ unit) · chevron. */
@Composable
private fun SettingRow(
    label: String,
    onClick: () -> Unit,
    value: String? = null,
    unit: String? = null,
) {
    SettingRowFrame(label, Modifier.clickable(onClick = onClick)) {
        if (value != null) {
            ValueText(value = value, unit = unit, style = AppText.numberM)
            Spacer(Modifier.width(Spacing.s))
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The shared row box: label left, [trailing] right. No horizontal padding — rows run to the
 * screen gutter, as there is no card to sit inside.
 */
@Composable
private fun SettingRowFrame(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.m))
        trailing()
    }
}

/** One value, one [Stepper] (plus optional presets), saved on Done; dismissing discards. */
@Composable
private fun StepperSheet(
    title: String,
    message: String,
    initial: Int,
    range: IntRange,
    step: Int,
    format: (Int) -> String,
    decrementLabel: String,
    incrementLabel: String,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
    presets: List<Int?>? = null,
) {
    var value by remember { mutableIntStateOf(initial.coerceIn(range)) }
    AppSheet(onDismiss = onDismiss) {
        Text(title, style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.s))
        Text(message, style = AppText.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xxl))
        Stepper(
            value = value,
            onValueChange = { value = it },
            range = range,
            step = step,
            format = format,
            decrementLabel = decrementLabel,
            incrementLabel = incrementLabel,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (presets != null) {
            Spacer(Modifier.height(Spacing.s))
            UnderlineSelector(
                options = presets,
                selected = value.takeIf { it in presets },
                onSelect = { it?.let { v -> value = v } },
                fillWidth = true,
                label = { format(it ?: 0) },
            )
        }
        AppButton(
            text = "Done",
            onClick = { onSave(value) },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
        )
    }
}

/** `1.0 (1)` — versionName (versionCode). */
private fun appVersion(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    "${info.versionName} (${info.longVersionCode})"
}.getOrDefault(EM_DASH)
