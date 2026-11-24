package com.example.gymtracker.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.displayName
import com.example.gymtracker.domain.AnalyticsWindow
import com.example.gymtracker.domain.MuscleVolume
import com.example.gymtracker.domain.rangeEpochDays
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.BarChart
import com.example.gymtracker.ui.components.BarDatum
import com.example.gymtracker.ui.components.BodyDiagram
import com.example.gymtracker.ui.components.BodyView
import com.example.gymtracker.ui.components.MuscleRankRow
import com.example.gymtracker.ui.components.PeriodPager
import com.example.gymtracker.ui.components.TrainingCalendar
import com.example.gymtracker.ui.components.heatFill
import com.example.gymtracker.ui.components.periodLabel
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.StatRow
import com.example.gymtracker.ui.components.atoms.StatusCell
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.components.atoms.ValueText
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatVolume
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.grouped
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Effort
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Muscle rows shown before "Show all". */
private const val TOP_MUSCLES = 6

/** Neglected muscles named before "+N". */
private const val MAX_NEGLECTED = 5

/**
 * Progress (§4.12; was Analytics) — bar destination 3. The range selector and period pager are
 * pinned and drive every section. A bar screen: its Scaffold adds no bar height or nav inset (the
 * NavHost consumed them) and there is no FAB.
 */
@Composable
fun ProgressScreen(onOpenExercises: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val vm: ProgressViewModel = viewModel(
        factory = AppViewModelFactory(
            mapOf(ProgressViewModel::class.java to { ProgressViewModel(container.repository, container.prefs) }),
        ),
    )
    val selection by vm.selection.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val canGoPrevious by vm.canGoPrevious.collectAsStateWithLifecycle()
    val unit by container.prefs.weightUnit.collectAsStateWithLifecycle(initialValue = WeightUnit.Kg)
    var drilled by remember { mutableStateOf<Muscle?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Progress",
                actions = { AppTextButton(text = "Exercises", onClick = onOpenExercises) },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Pinned: range + pager.
            Column(Modifier.padding(horizontal = Spacing.gutter)) {
                UnderlineSelector(
                    options = AnalyticsWindow.entries.toList(),
                    selected = selection.window,
                    onSelect = vm::setWindow,
                    fillWidth = true,
                    label = { it.label },
                )
                PeriodPager(
                    label = periodLabel(selection.window.rangeEpochDays(offset = selection.offset)),
                    onPrevious = vm::previousPeriod,
                    onNext = vm::nextPeriod,
                    canGoNext = selection.offset > 0,
                    canGoPrevious = canGoPrevious,
                )
                AppDivider()
            }
            val s = state ?: return@Column // Loading: top bar + selectors only.
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter)
                    .padding(top = Spacing.section, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.section),
            ) {
                ConsistencyBlock(s)
                VolumeSection(s, unit, onMetric = vm::setMetric)
                MusclesSection(
                    s,
                    selected = drilled,
                    // Tapping the selected muscle again clears it (and closes its sheet).
                    onMuscle = { drilled = if (drilled == it) null else it },
                )
            }
        }
    }

    drilled?.let { muscle ->
        val breakdown = remember(muscle, state) { vm.breakdownFor(muscle) }
        AppSheet(onDismiss = { drilled = null }) {
            Text(muscle.displayName, style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Spacing.l))
            if (breakdown != null) BreakdownBody(breakdown, unit)
        }
    }
}

/** TRAINING DAYS hero, on-target reference, then the 12-week calendar (decision 7). */
@Composable
private fun ConsistencyBlock(s: ProgressState) {
    val c = s.consistency
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Training days")
                Text("${c.trainingDays}", style = AppText.numberXL, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow("On target")
                ValueText(
                    value = if (c.onTarget.of == 0) EM_DASH else "${c.onTarget.met}/${c.onTarget.of}",
                    unit = if (c.onTarget.of == 0) null else "wk",
                    style = AppText.numberM,
                )
                Spacer(Modifier.height(Spacing.xs))
                Eyebrow("Best run")
                ValueText(
                    value = if (c.bestRun == 0) EM_DASH else "${c.bestRun}",
                    unit = if (c.bestRun == 0) null else "wk",
                    style = AppText.numberM,
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        Text(
            "Target ${c.target} ${plural(c.target, "session")} a week",
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.s))
        AppDivider()
        Spacer(Modifier.height(Spacing.l))
        TrainingCalendar(
            sessionDays = c.trainedDays,
            metWeeks = c.metWeeks,
            endEpochDay = s.period.endInclusive,
            today = s.today,
        )
    }
}

@Composable
private fun VolumeSection(s: ProgressState, unit: WeightUnit, onMetric: (VolumeMetric) -> Unit) {
    val v = s.volume
    Section(
        title = "Volume",
        trailing = {
            UnderlineSelector(
                options = VolumeMetric.entries.toList(),
                selected = v.metric,
                onSelect = onMetric,
                label = { it.label },
            )
        },
    ) {
        if (!s.hasWork) {
            NothingInPeriod()
            return@Section
        }
        Row(verticalAlignment = Alignment.Bottom) {
            ValueText(
                value = volumeNumber(v.total, v.metric, unit),
                unit = volumeUnit(v.total, v.metric, unit),
                style = AppText.numberL,
                modifier = Modifier.weight(1f),
            )
            v.change?.let { Eyebrow("${percent(it)} vs 4-wk", color = changeColor(it)) }
        }
        Spacer(Modifier.height(Spacing.l))
        var selected by rememberSaveable(s.selection, v.metric) { mutableStateOf<Int?>(null) }
        val labelEvery = ceil(v.bars.size / 8f).toInt().coerceAtLeast(1)
        val reference = v.bars.lastOrNull()?.priorAverage?.takeIf { it > 0f }
        BarChart(
            bars = v.bars.mapIndexed { i, w ->
                BarDatum(
                    value = w.total,
                    label = if ((v.bars.lastIndex - i) % labelEvery == 0) "${LocalDate.ofEpochDay(w.weekStart).dayOfMonth}" else null,
                    inProgress = w.weekStart == v.runningWeek,
                )
            },
            reference = reference,
            referenceCaption = "avg 4 wk",
            selected = selected,
            onSelect = { selected = it },
            tagFor = { i -> v.bars[i].changeVsPrior?.let(::percent) ?: "No base" },
            description = "Weekly ${v.metric.label.lowercase()}, ${v.bars.size} weeks; last week " +
                "${volumeNumber(v.bars.last().total, v.metric, unit)} ${volumeUnit(v.bars.last().total, v.metric, unit).orEmpty()}",
        )
        Text(
            "Weeks start Monday · tap a bar for its change vs the 4 weeks before",
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

@Composable
private fun MusclesSection(s: ProgressState, selected: Muscle?, onMuscle: (Muscle) -> Unit) {
    val m = s.muscles
    val scheme = MaterialTheme.colorScheme
    Section(title = "Muscles") {
        if (!s.hasWork) {
            NothingInPeriod()
            return@Section
        }
        val fills = heatFill(m.levels)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            listOf(BodyView.Front to "Front", BodyView.Back to "Back").forEach { (view, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Eyebrow(label)
                    Spacer(Modifier.height(Spacing.xs))
                    BodyDiagram(
                        view = view,
                        fills = fills,
                        // The drilled muscle stays outlined while its breakdown sheet is open.
                        selectedMuscle = selected,
                        onMuscleTap = onMuscle,
                        modifier = Modifier.fillMaxWidth().aspectRatio(100f / 220f),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.m))
        HeatLegend()
        Spacer(Modifier.height(Spacing.l))

        val week = s.selection.window == AnalyticsWindow.Week
        val top = m.ranked.firstOrNull()?.second ?: 0f
        // In WEEK the scale reaches past the band so both ticks always fit (decision 11).
        val scale = if (week) maxOf(top, MuscleVolume.GUIDELINE_HIGH_SETS * 1.25f) else top.coerceAtLeast(1f)
        val band = if (week) (MuscleVolume.GUIDELINE_LOW_SETS / scale)..(MuscleVolume.GUIDELINE_HIGH_SETS / scale) else null
        var expanded by rememberSaveable { mutableStateOf(false) }
        val shown = if (expanded) m.ranked else m.ranked.take(TOP_MUSCLES)
        shown.forEach { (muscle, sets) ->
            val rounded = sets.roundToInt()
            MuscleRankRow(
                name = muscle.displayName,
                value = "$rounded",
                unit = plural(rounded, "set"),
                fraction = sets / scale,
                band = band,
                valueColor = if (!week) scheme.onSurface else when (MuscleVolume.guideline(sets)) {
                    MuscleVolume.Guideline.Below -> scheme.onSurfaceVariant
                    MuscleVolume.Guideline.Within -> scheme.primary
                    MuscleVolume.Guideline.Above -> Effort
                },
                onClick = { onMuscle(muscle) },
            )
        }
        if (week) {
            Text(
                "Ticks mark the 10–20 working sets a week guideline",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        if (m.ranked.size > TOP_MUSCLES) {
            AppTextButton(
                text = if (expanded) "Show top $TOP_MUSCLES" else "Show all ${m.ranked.size}",
                onClick = { expanded = !expanded },
                color = scheme.onSurface,
            )
        }
        if (m.neglected.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.l))
            Eyebrow("Not trained this period")
            Spacer(Modifier.height(Spacing.xs))
            val names = m.neglected.take(MAX_NEGLECTED).joinToString(" · ") { it.displayName }
            val more = m.neglected.size - MAX_NEGLECTED
            Text(
                if (more > 0) "$names · +$more" else names,
                style = AppText.body,
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun HeatLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Less", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { StatusCell(heatColor(it)) }
        }
        Spacer(Modifier.width(Spacing.s))
        Text("More", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NothingInPeriod() {
    Text("Nothing in this period", style = AppText.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The muscle breakdown sheet, restyled: StatRows, display names, volume in the user's unit. */
@Composable
private fun BreakdownBody(b: MuscleBreakdown, unit: WeightUnit) {
    StatRow(label = "Working sets", value = "${b.sets.roundToInt()}", caption = "Secondary muscles count 0.3 a set")
    StatRow(label = "Tonnage", value = formatVolumeNumber(b.tonnageKg.toDouble(), unit), unit = unit.label)
    Spacer(Modifier.height(Spacing.m))
    if (b.exercises.isEmpty()) {
        Text(
            "No completed sets worked this muscle in this period.",
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Section(title = "Exercises") {
        b.exercises.forEach { c ->
            val caption = buildList {
                if (c.tonnageKg > 0.0) add(formatVolume(c.tonnageKg, unit))
                if (c.bodyweightSets > 0) add("${c.bodyweightSets} bodyweight")
            }.joinToString(" · ").ifEmpty { null }
            StatRow(
                label = c.exercise.name,
                value = "${c.sets}",
                unit = plural(c.sets, "set"),
                caption = caption,
                valueStyle = AppText.numberM,
            )
        }
    }
}

// ---- Formatting ----------------------------------------------------------------------------

private fun volumeNumber(value: Float, metric: VolumeMetric, unit: WeightUnit): String = when (metric) {
    VolumeMetric.Sets -> grouped(value)
    VolumeMetric.Tonnage -> formatVolumeNumber(value.toDouble(), unit)
}

private fun volumeUnit(value: Float, metric: VolumeMetric, unit: WeightUnit): String? = when (metric) {
    VolumeMetric.Sets -> plural(value.roundToInt(), "working set")
    VolumeMetric.Tonnage -> unit.label
}

/** `+12%`, `−8%`, `±0%`. */
private fun percent(change: Float): String {
    val p = (change * 100f).roundToInt()
    return when {
        p > 0 -> "+$p%"
        p < 0 -> "−${abs(p)}%"
        else -> "±0%"
    }
}

@Composable
private fun changeColor(change: Float): Color = when {
    (change * 100f).roundToInt() > 0 -> MaterialTheme.colorScheme.primary
    (change * 100f).roundToInt() < 0 -> Effort
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
