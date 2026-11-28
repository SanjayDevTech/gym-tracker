package com.example.gymtracker.ui.screens.analytics

import com.example.gymtracker.ui.components.TileHead
import com.example.gymtracker.ui.components.RichTile
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.components.StatTile
import com.composables.icons.lucide.Weight
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Activity
import com.example.gymtracker.ui.theme.meshGradient
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.components.atoms.staggerIn
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.Metric
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AnimatedCount
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.example.gymtracker.ui.components.atoms.RoundIconButton
import com.example.gymtracker.ui.navigation.bottomBarClearance
import androidx.compose.foundation.layout.width
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
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.StatRow
import com.example.gymtracker.ui.components.atoms.StatusCell
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.components.atoms.heatRamp
import com.example.gymtracker.ui.components.BodyMap
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
 * Progress (was Analytics). The range selector and period pager are
 * pinned and drive every section. A bar screen: its Scaffold adds no bar height or nav inset (the
 * NavHost consumed them) and there is no FAB.
 */
@Composable
fun ProgressScreen(onOpenExercises: () -> Unit, onOpenHistory: () -> Unit, modifier: Modifier = Modifier) {
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
            ProgressHeader(
                caption = periodCaption(selection),
                label = periodLabel(selection.window.rangeEpochDays(offset = selection.offset)),
                onPrevious = vm::previousPeriod,
                onNext = vm::nextPeriod,
                canGoNext = selection.offset > 0,
                canGoPrevious = canGoPrevious,
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Pinned under the header: the range. The period itself is the pill in the header.
            ExclusiveButtonGroup(
                options = AnalyticsWindow.entries.toList(),
                selected = selection.window,
                onSelect = vm::setWindow,
                fillWidth = true,
                label = { it.label },
                modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.xs),
            )
            val s = state ?: return@Column // Loading: top bar + selectors only.
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter)
                    .padding(top = Spacing.m, bottom = bottomBarClearance()),
                verticalArrangement = Arrangement.spacedBy(Spacing.section),
            ) {
                ConsistencyHero(s, Modifier.staggerIn(0))
                ProgressTiles(s, unit, Modifier.staggerIn(1))
                CalendarSection(s, Modifier.staggerIn(1))
                VolumeSection(s, unit, onMetric = vm::setMetric, modifier = Modifier.staggerIn(2))
                MusclesSection(
                    s,
                    modifier = Modifier.staggerIn(3),
                    selected = drilled,
                    // Tapping the selected muscle again clears it (and closes its sheet).
                    onMuscle = { drilled = if (drilled == it) null else it },
                )
                ShortcutTiles(onOpenHistory, onOpenExercises, Modifier.staggerIn(4))
            }
        }
    }

    drilled?.let { muscle ->
        val breakdown = remember(muscle, state) { vm.breakdownFor(muscle) }
        AppSheet(onDismiss = { drilled = null }) {
            SheetTitleRow(muscle.displayName, onClose = { drilled = null })
            Spacer(Modifier.height(Spacing.m))
            if (breakdown != null) BreakdownBody(breakdown, unit)
        }
    }
}

/**
 * Training days on Foodprint's soft gradient hero: the count as one big counting number and the
 * weekly target as a chip. The other figures sit in [ProgressTiles] under it.
 */
@Composable
private fun ConsistencyHero(s: ProgressState, modifier: Modifier = Modifier) {
    val c = s.consistency
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient(animate = false)
            .dotTexture(color = scheme.onSurface.copy(alpha = 0.10f))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Training days", style = AppText.eyebrow, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                "Target ${c.target} a week",
                style = AppText.label,
                color = scheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(cardColor().copy(alpha = 0.75f))
                    .padding(horizontal = Spacing.m, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedCount(value = c.trainingDays, style = AppText.numberXXL, color = scheme.onSurface)
            Text(
                " ${plural(c.trainingDays, "day")}",
                style = AppText.titleM,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/** The period's figures as bento tiles: weeks on target, best run, volume and muscles trained. */
@Composable
private fun ProgressTiles(s: ProgressState, unit: WeightUnit, modifier: Modifier = Modifier) {
    val c = s.consistency
    val v = s.volume
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.section)) {
            StatTile(
                Rich.Sessions, Lucide.Target, "On target",
                if (c.onTarget.of == 0) EM_DASH else "${c.onTarget.met}",
                Modifier.weight(1f).fillMaxHeight(),
                unit = if (c.onTarget.of == 0) "" else "of ${c.onTarget.of} wk",
                fraction = if (c.onTarget.of == 0) null else c.onTarget.met.toFloat() / c.onTarget.of,
            )
            StatTile(
                Rich.Streak, Lucide.Flame, "Best run",
                if (c.bestRun == 0) EM_DASH else "${c.bestRun}",
                Modifier.weight(1f).fillMaxHeight(),
                unit = if (c.bestRun == 0) "" else plural(c.bestRun, "week"),
            )
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.section)) {
            StatTile(
                Rich.Volume, Lucide.Weight, "Volume",
                if (s.hasWork) volumeNumber(v.total, v.metric, unit) else EM_DASH,
                Modifier.weight(1f).fillMaxHeight(),
                unit = if (s.hasWork) volumeUnit(v.total, v.metric, unit).orEmpty() else "",
                caption = v.change?.let { "${percent(it)} vs 4 wk" },
            )
            val trained = s.muscles.ranked.size
            StatTile(
                Rich.Muscles, Lucide.Activity, "Muscles",
                "$trained",
                Modifier.weight(1f).fillMaxHeight(),
                unit = "of ${Muscle.entries.size}",
                fraction = trained.toFloat() / Muscle.entries.size,
            )
        }
    }
}

/** The 12-week calendar in its own card. */
@Composable
private fun CalendarSection(s: ProgressState, modifier: Modifier = Modifier) {
    val c = s.consistency
    Section(
        title = "Last 12 weeks",
        modifier = modifier,
        trailing = {
            Text(
                "✓ week on target",
                style = AppText.bodyS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        TrainingCalendar(
            sessionDays = c.trainedDays,
            metWeeks = c.metWeeks,
            endEpochDay = s.period.endInclusive,
            today = s.today,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun VolumeSection(
    s: ProgressState,
    unit: WeightUnit,
    onMetric: (VolumeMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val v = s.volume
    Section(
        title = "Volume",
        modifier = modifier,
        trailing = {
            ExclusiveButtonGroup(
                options = VolumeMetric.entries.toList(),
                selected = v.metric,
                onSelect = onMetric,
                fillWidth = false,
                label = { it.label },
            )
        },
    ) {
        if (!s.hasWork) {
            NothingInPeriod()
            return@Section
        }
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
            "Tap a week for its change vs the 4 before",
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

@Composable
private fun MusclesSection(
    s: ProgressState,
    selected: Muscle?,
    onMuscle: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = s.muscles
    val scheme = MaterialTheme.colorScheme
    Section(
        title = "Muscles",
        modifier = modifier,
        trailing = {
            if (s.hasWork) Text("${m.ranked.size} trained", style = AppText.bodyS, color = scheme.onSurfaceVariant)
        },
    ) {
        if (!s.hasWork) {
            NothingInPeriod()
            return@Section
        }
        // Remembered so toggling "Show all" (state read in this lambda) hands both BodyDiagrams
        // the same map and they skip instead of recomposing and redrawing every region.
        val ramp = heatRamp()
        val fills = remember(m.levels, ramp) { heatFill(m.levels, ramp) }
        // The drilled muscle stays outlined while its breakdown sheet is open.
        BodyMap(fills = fills, selectedMuscle = selected, onMuscleTap = onMuscle)
        Spacer(Modifier.height(Spacing.m))
        HeatLegend()
        Spacer(Modifier.height(Spacing.l))

        val week = s.selection.window == AnalyticsWindow.Week
        val top = m.ranked.firstOrNull()?.second ?: 0f
        // In WEEK the scale reaches past the band so both ticks always fit.
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
                    MuscleVolume.Guideline.Within -> scheme.secondary
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
            AppOutlinedButton(
                text = if (expanded) "Show top $TOP_MUSCLES" else "Show all ${m.ranked.size}",
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
            )
        }
        if (m.neglected.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.l))
            Text("Not trained this period", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
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
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Less", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.s))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            heatRamp().forEach {
                Box(Modifier.size(width = 18.dp, height = 8.dp).clip(RoundedCornerShape(Radius.pill)).background(it))
            }
        }
        Spacer(Modifier.width(Spacing.s))
        Text("More", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
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

/**
 * Foodprint's Today header for Progress: a small caption and the page title on the left, the
 * date pill (`‹ 21 – 27 Nov ›`) on the right. The title is measured first, so on a narrow screen
 * the pill's label ellipsises rather than the title.
 */
@Composable
private fun ProgressHeader(
    caption: String,
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
    canGoPrevious: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.m, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(caption, style = AppText.bodyS, color = scheme.onSurfaceVariant, maxLines = 1)
            Text("Progress", style = AppText.headline, color = scheme.onSurface, maxLines = 1)
        }
        PeriodPager(
            label = label,
            onPrevious = onPrevious,
            onNext = onNext,
            canGoNext = canGoNext,
            canGoPrevious = canGoPrevious,
            modifier = Modifier.padding(start = Spacing.m).weight(1f, fill = false),
        )
    }
}

/** `This week`, `Last 30 days`… for the current period; `Looking back` for an earlier one. */
private fun periodCaption(selection: PeriodSelection): String = if (selection.offset > 0) "Looking back" else when (selection.window) {
    AnalyticsWindow.Today -> "Today"
    AnalyticsWindow.Week -> "Last 7 days"
    AnalyticsWindow.Month -> "Last 30 days"
    AnalyticsWindow.ThreeMonth -> "Last 3 months"
    AnalyticsWindow.Year -> "Last 12 months"
}

/** History and the exercise library as two icon tiles at the foot of the page. */
@Composable
private fun ShortcutTiles(onOpenHistory: () -> Unit, onOpenExercises: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        RichTile(Rich.Sessions, Modifier.weight(1f).fillMaxHeight(), onClick = onOpenHistory) {
            TileHead(Lucide.History, "History")
            Spacer(Modifier.height(Spacing.m))
            Text("Every session, week by week", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RichTile(Rich.Body, Modifier.weight(1f).fillMaxHeight(), onClick = onOpenExercises) {
            TileHead(Lucide.BookOpen, "Exercises")
            Spacer(Modifier.height(Spacing.m))
            Text("The library, records and charts", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
