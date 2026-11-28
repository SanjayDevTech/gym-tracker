package com.example.gymtracker.ui.screens.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.History
import com.composables.icons.lucide.LayoutTemplate
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Ruler
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.SunMoon
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.Utensils
import com.composables.icons.lucide.Vibrate
import com.example.gymtracker.data.ThemeMode
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppSwitch
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.RoundIconButton
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.Stepper
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.components.atoms.staggerIn
import com.example.gymtracker.ui.components.openFoodprint
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.grouped
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.navigation.bottomBarClearance
import com.example.gymtracker.ui.screens.logger.SessionLoggerViewModel
import com.example.gymtracker.ui.screens.plans.components.TextEntrySheet
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.meshGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RestPresets: List<Int?> = listOf(60, 90, 120, 180)

private enum class YouSheet { WeeklyTarget, DefaultRest, Appearance, Name, Foodprint }

/**
 * You — the third tab, Foodprint's You for training: a gradient profile card with all-time
 * figures, then the settings as icon rows that show their current value and open a sheet (short)
 * or a page (long).
 */
@Composable
fun YouScreen(
    viewModel: SettingsViewModel,
    onOpenData: () -> Unit,
    onOpenExercises: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    var sheet by rememberSaveable { mutableStateOf<YouSheet?>(null) }
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("You")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.s,
                bottom = bottomBarClearance(),
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item("profile") {
                ProfileCard(state = state, onEditName = { sheet = YouSheet.Name }, modifier = Modifier.staggerIn(0))
            }
            item("training") {
                Section("Training", Modifier.staggerIn(1)) {
                    SettingRow(
                        label = "Weekly target",
                        icon = Lucide.Target,
                        pastel = Pastel.Mint,
                        value = "${state.weeklyTargetSessions} ${plural(state.weeklyTargetSessions, "session")}",
                        onClick = { sheet = YouSheet.WeeklyTarget },
                    )
                    AppDivider()
                    SettingRow(
                        label = "Default rest",
                        icon = Lucide.Timer,
                        pastel = Pastel.Sky,
                        value = formatClock(state.defaultRestSeconds * 1000L),
                        onClick = { sheet = YouSheet.DefaultRest },
                    )
                    AppDivider()
                    SettingRowFrame("Weight", Lucide.Scale, Pastel.Peach) {
                        ExclusiveButtonGroup(
                            options = WeightUnit.entries.toList(),
                            selected = state.weightUnit,
                            onSelect = viewModel::setWeightUnit,
                            fillWidth = false,
                            label = { it.label },
                        )
                    }
                    AppDivider()
                    SettingRowFrame("Volume", Lucide.Ruler, Pastel.Lavender) {
                        ExclusiveButtonGroup(
                            options = VolumeMetric.entries.toList(),
                            selected = state.volumeMetric,
                            onSelect = viewModel::setVolumeMetric,
                            fillWidth = false,
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
                        modifier = Modifier.padding(start = 48.dp, bottom = Spacing.xs),
                    )
                }
            }
            item("library") {
                Section("Library", Modifier.staggerIn(2)) {
                    SettingRow("Exercises", Lucide.BookOpen, Pastel.Teal, onClick = onOpenExercises)
                    AppDivider()
                    SettingRow("Plan templates", Lucide.LayoutTemplate, Pastel.Rose, onClick = onOpenTemplates)
                    AppDivider()
                    SettingRow(
                        "History",
                        Lucide.History,
                        Pastel.Mint,
                        value = "${state.lifetime.sessions} ${plural(state.lifetime.sessions, "session")}",
                        onClick = onOpenHistory,
                    )
                    AppDivider()
                    SettingRow("Plans & backup", Lucide.Database, Pastel.Sky, onClick = onOpenData)
                }
            }
            item("app") {
                Section("App", Modifier.staggerIn(3)) {
                    SettingRow(
                        label = "Appearance",
                        icon = Lucide.SunMoon,
                        pastel = Pastel.Lavender,
                        value = state.themeMode.displayName,
                        onClick = { sheet = YouSheet.Appearance },
                    )
                    AppDivider()
                    SettingRowFrame("Haptic feedback", Lucide.Vibrate, Pastel.Sand) {
                        AppSwitch(checked = state.haptics, onCheckedChange = { viewModel.setHaptics(it) })
                    }
                    AppDivider()
                    SettingRow(
                        label = "Foodprint",
                        icon = Lucide.Utensils,
                        pastel = Pastel.Mint,
                        value = "Food diary",
                        onClick = { if (!openFoodprint(context)) sheet = YouSheet.Foodprint },
                    )
                }
            }
            item("version") {
                Text(
                    "Lift Up $version · offline-first",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.s, start = Spacing.xs),
                )
            }
        }
    }

    when (sheet) {
        YouSheet.WeeklyTarget -> StepperSheet(
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
        YouSheet.DefaultRest -> StepperSheet(
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
        YouSheet.Appearance -> AppearanceSheet(
            mode = state.themeMode,
            onSelect = viewModel::setThemeMode,
            onDismiss = { sheet = null },
        )
        YouSheet.Name -> TextEntrySheet(
            title = "Your name",
            caption = "Home greets you by your first name. Leave it blank to skip.",
            initial = state.name,
            placeholder = "Name",
            confirmLabel = "Save",
            allowBlank = true,
            onConfirm = { viewModel.setName(it); sheet = null },
            onDismiss = { sheet = null },
        )
        YouSheet.Foodprint -> AppSheet(onDismiss = { sheet = null }) {
            SheetTitleRow("Foodprint isn't installed", onClose = { sheet = null })
            Spacer(Modifier.height(Spacing.s))
            Text(
                "Foodprint is the food diary that pairs with Lift Up: meals, 30 nutrients and streaks, " +
                    "private on your phone. Install it and this row takes you straight there.",
                style = AppText.body,
                color = scheme.onSurfaceVariant,
            )
            AppButton("Got it", onClick = { sheet = null }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl))
        }
        null -> Unit
    }
}

/**
 * The profile card: initials on an ink disc, the name and how long you've been training, then
 * all-time figures on frosted tiles over the soft mesh gradient.
 */
@Composable
private fun ProfileCard(state: SettingsState, onEditName: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val life = state.lifetime
    val initials = state.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient(animate = false)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (initials.isEmpty()) {
                    Icon(Lucide.UserRound, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(26.dp))
                } else {
                    Text(initials, style = AppText.titleL, color = scheme.onPrimary)
                }
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    state.name.ifBlank { "Your training" },
                    style = AppText.titleL,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    life.sinceEpochDay?.let {
                        "Training since " + LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
                    } ?: "No workouts logged yet",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
            }
            RoundIconButton(icon = Lucide.Pencil, contentDescription = "Edit name", onClick = onEditName)
        }
        Spacer(Modifier.height(Spacing.l))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            GlassStat("Sessions", grouped(life.sessions), "", Modifier.weight(1f).fillMaxHeight())
            GlassStat("Sets", grouped(life.sets), "", Modifier.weight(1f).fillMaxHeight())
            GlassStat(
                "Best run",
                if (life.sessions == 0) EM_DASH else "${life.bestRunWeeks}",
                if (life.sessions == 0) "" else "wk",
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            // Lifetime volume runs to six digits, so it gets the wide tile.
            GlassStat("Volume", formatVolumeNumber(life.volumeKg, state.weightUnit), state.weightUnit.label, Modifier.weight(1f).fillMaxHeight())
            GlassStat("Weekly target", "${state.weeklyTargetSessions}", plural(state.weeklyTargetSessions, "session"), Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** A frosted tile on the profile gradient. */
@Composable
private fun GlassStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(cardColor().copy(alpha = 0.62f))
            .padding(horizontal = Spacing.m, vertical = Spacing.s + 2.dp),
    ) {
        Text(label, style = AppText.eyebrow, color = scheme.onSurfaceVariant, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppText.numberM, color = scheme.onSurface, maxLines = 1)
            if (unit.isNotEmpty()) Text(NBSP + unit, style = AppText.bodyS, color = scheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Icon disc · label · value · chevron. */
@Composable
private fun SettingRow(
    label: String,
    icon: ImageVector,
    pastel: Pastel,
    value: String? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    SettingRowFrame(label, icon, pastel, Modifier.pressable(onClick = onClick)) {
        if (value != null) {
            Text(
                value,
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            Spacer(Modifier.width(Spacing.s))
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

/** The shared row: a pastel icon disc, the label, then [trailing]. */
@Composable
private fun SettingRowFrame(
    label: String,
    icon: ImageVector,
    pastel: Pastel,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PastelIcon(icon, pastel, size = 36.dp)
        Spacer(Modifier.width(Spacing.m))
        Text(
            label,
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Light / Dark / Match system as three tappable preview cards. */
@Composable
private fun AppearanceSheet(mode: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismiss = onDismiss) {
        SheetTitleRow("Appearance", onClose = onDismiss)
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            ThemeMode.entries.forEach { option ->
                ThemeOption(option, option == mode, { onSelect(option) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Text(
            "Light is the default. \"Match system\" follows your phone's dark-mode setting.",
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeOption(option: ThemeMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ring by animateColorAsState(if (selected) scheme.primary else Color.Transparent, label = "themeRing")
    val (top, bottom) = when (option) {
        ThemeMode.Light -> Color(0xFFF4F4F7) to Color(0xFFFFFFFF)
        ThemeMode.Dark -> Color(0xFF0B0B0E) to Color(0xFF17171B)
        ThemeMode.System -> Color(0xFFF4F4F7) to Color(0xFF17171B)
    }
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(cardColor())
            .border(2.dp, ring, RoundedCornerShape(Radius.md))
            .semantics { role = Role.RadioButton; this.selected = selected }
            .pressable(onClick = onClick)
            .padding(Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(top)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(Radius.sm)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(topStart = Radius.sm, topEnd = Radius.sm))
                    .background(bottom),
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Icon(
            when (option) {
                ThemeMode.System -> Lucide.Smartphone
                ThemeMode.Light -> Lucide.Sun
                ThemeMode.Dark -> Lucide.Moon
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(option.displayName, style = AppText.label, color = scheme.onSurface)
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
        SheetTitleRow(title, onClose = onDismiss)
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
            ExclusiveButtonGroup(
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
