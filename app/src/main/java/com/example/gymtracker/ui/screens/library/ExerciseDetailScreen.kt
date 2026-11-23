package com.example.gymtracker.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.displayName
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.domain.ProgressMetric
import com.example.gymtracker.domain.ProgressPoint
import com.example.gymtracker.ui.components.BodyDiagram
import com.example.gymtracker.ui.components.BodyView
import com.example.gymtracker.ui.components.LineChart
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.OverflowMenu
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.ConfirmSheet
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.StatRow
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.components.atoms.ValueText
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.formatWeightNumber
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Effort
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/** Body diagram height on this page (§4.11: 160dp). A size. */
private val DiagramHeight = 160.dp

/** Width of the RECENT date column, so the set lists align. A size. */
private val RecentDateWidth = 64.dp

/** Chart range selector (§4.11, S6-b). */
private enum class ChartRange(val label: String, val days: Long?) {
    OneMonth("1M", 30), ThreeMonths("3M", 90), OneYear("1Y", 365), All("All", null),
}

/**
 * Exercise detail (§4.11) — e1RM hero, a dated chart between its two selectors, records with
 * dates, recent sets grouped by session, the muscles on the body diagram, cues. A leaf page: its
 * Scaffold applies the system insets once. Custom exercises get a ⋮ with Edit (§4.19) and Delete
 * (soft delete behind a [ConfirmSheet]; history keeps the name) — phase 9.
 */
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel,
    weightUnit: WeightUnit,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state?.exercise
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = exercise?.name.orEmpty(),
                subtitle = exercise?.let { "${it.movementType.displayName} · ${it.equipment.displayName}" },
                onBack = onBack,
                actions = {
                    // Phase 9: custom exercises only. Built-ins are never editable or deletable
                    // (the seed's deterministic UUIDs and seedVersion reseeding depend on it).
                    if (exercise != null && exercise.isCustom && !exercise.isDeleted) {
                        OverflowMenu(
                            listOf(
                                MenuItem("Edit", { onEdit(exercise.id) }),
                                MenuItem("Delete", { confirmDelete = true }, destructive = true),
                            ),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        val s = state ?: return@Scaffold // Loading: top bar only (house rule 8).
        val ex = exercise ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(top = Spacing.s, bottom = Spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            if (s.readyToProgress) ReadyBlock()
            ProgressBlock(s, ex, weightUnit)
            if (s.hasHistory) {
                RecordsSection(s, ex, weightUnit)
                RecentSection(s, ex, weightUnit)
            }
            MusclesSection(ex)
            if (ex.cues.isNotBlank()) {
                Section(title = "Cues") {
                    Text(ex.cues, style = AppText.body, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (confirmDelete && exercise != null) {
        ConfirmSheet(
            title = "Delete ${exercise.name}?",
            message = deleteMessage(state?.planCount ?: 0),
            confirmLabel = "Delete exercise",
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** "Its logged sets stay in History." plus, when it is planned, how many plans it leaves. */
internal fun deleteMessage(planCount: Int): String = buildString {
    append("Its logged sets stay in History.")
    if (planCount > 0) append(" It will be removed from $planCount ${plural(planCount, "plan")}.")
}

@Composable
private fun ReadyBlock() {
    Column {
        Eyebrow("Ready to progress", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Top of the range two sessions running. Add weight next time.",
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.l))
        AppDivider()
    }
}

/** Hero (S4-b) · rule · metric selector · chart · range selector. */
@Composable
private fun ProgressBlock(s: ExerciseDetailState, ex: ExerciseEntity, unit: WeightUnit) {
    val type = ex.movementType
    val metrics = if (s.weighted) {
        listOf(ProgressMetric.OneRepMax, ProgressMetric.Heaviest, ProgressMetric.Volume, ProgressMetric.Reps)
    } else {
        listOf(ProgressMetric.Reps)
    }
    val heroMetric = metrics.first()
    val heroPoints = s.series[heroMetric].orEmpty()
    val today = DateUtil.todayEpochDay()

    Column {
        // Hero left, reference right.
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow(heroLabel(heroMetric, type))
                val last = heroPoints.lastOrNull()
                ValueText(
                    value = last?.let { metricNumber(heroMetric, it.value, unit) } ?: EM_DASH,
                    unit = last?.let { metricUnit(heroMetric, type, unit) },
                    style = AppText.numberXXL,
                )
            }
            val best = s.records[heroMetric]
            if (best != null) {
                Spacer(Modifier.width(Spacing.m))
                Column(horizontalAlignment = Alignment.End) {
                    Eyebrow("Best")
                    ValueText(
                        value = metricNumber(heroMetric, best.value, unit),
                        unit = metricUnit(heroMetric, type, unit),
                        style = AppText.numberM,
                    )
                    threeMonthDelta(heroPoints)?.let { delta ->
                        val sign = if (delta > 0f) "+" else if (delta < 0f) "−" else "±"
                        Eyebrow(
                            "$sign${metricNumber(heroMetric, abs(delta), unit)} in 3M",
                            color = when {
                                delta > 0f -> MaterialTheme.colorScheme.primary
                                delta < 0f -> Effort
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.l))
        AppDivider()

        if (!s.hasHistory) {
            Text(
                "Log this exercise to see progress",
                style = AppText.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.xxl),
            )
            return@Column
        }

        var chosen by rememberSaveable(ex.id) { mutableStateOf(heroMetric) }
        val metric = chosen.takeIf { it in metrics } ?: heroMetric
        val defaultRange = remember(ex.id) {
            if (heroPoints.count { today - it.dateEpochDay < 90 } >= 2) ChartRange.ThreeMonths else ChartRange.All
        }
        var range by rememberSaveable(ex.id) { mutableStateOf(defaultRange) }

        if (metrics.size > 1) {
            UnderlineSelector(
                options = metrics,
                selected = metric,
                onSelect = { chosen = it },
                label = { metricTab(it, type) },
            )
        } else {
            Spacer(Modifier.height(Spacing.l))
        }
        val all = s.series[metric].orEmpty()
        val points = range.days?.let { d -> all.filter { today - it.dateEpochDay < d } } ?: all
        if (points.isEmpty()) {
            Text(
                "Nothing logged in this range",
                style = AppText.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.heightIn(min = 120.dp).padding(vertical = Spacing.xxl),
            )
        } else {
            val target = s.repTarget?.takeIf { metric == ProgressMetric.Reps }
            LineChart(
                points = points,
                formatValue = { "${metricNumber(metric, it, unit)} ${metricUnit(metric, type, unit)}" },
                formatDate = ::shortDate,
                band = target?.let { it.first.toFloat()..it.last.toFloat() },
                bandCaption = target?.let { "Target ${it.first}–${it.last}" },
                description = "${metricTab(metric, type)} over ${points.size} sessions, latest " +
                    "${metricNumber(metric, points.last().value, unit)} ${metricUnit(metric, type, unit)}",
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
        UnderlineSelector(
            options = ChartRange.entries.toList(),
            selected = range,
            onSelect = { range = it },
            label = { it.label },
        )
    }
}

@Composable
private fun RecordsSection(s: ExerciseDetailState, ex: ExerciseEntity, unit: WeightUnit) {
    val type = ex.movementType
    Section(title = "Records", trailing = {
        Text(
            "${s.sessionCount} ${plural(s.sessionCount, "session")}",
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }) {
        val kinds = if (s.weighted) {
            listOf(ProgressMetric.OneRepMax, ProgressMetric.Heaviest, ProgressMetric.Volume, ProgressMetric.Reps)
        } else {
            listOf(ProgressMetric.Reps)
        }
        kinds.forEach { m ->
            val r = s.records[m] ?: return@forEach
            StatRow(
                label = recordLabel(m, type),
                value = metricNumber(m, r.value, unit),
                unit = metricUnit(m, type, unit),
                caption = shortDate(r.dateEpochDay),
            )
        }
    }
}

/** RECENT: one line per session — date, then `60×10 · 62.5×9`; set types coloured (§1). */
@Composable
private fun RecentSection(s: ExerciseDetailState, ex: ExerciseEntity, unit: WeightUnit) {
    if (s.recent.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val lifting = ex.movementType == MovementType.Compound || ex.movementType == MovementType.Isolation
    Section(title = "Recent") {
        s.recent.forEach { session ->
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                Text(
                    shortDate(session.dateEpochDay),
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.width(RecentDateWidth),
                )
                Text(
                    buildAnnotatedString {
                        session.sets.forEachIndexed { i, set ->
                            if (i > 0) withStyle(SpanStyle(color = scheme.onSurfaceVariant)) { append(" · ") }
                            val color: Color = when (set.setType) {
                                SetType.Warmup -> scheme.onSurfaceVariant
                                SetType.Working -> scheme.onSurface
                                SetType.Failure -> Effort
                            }
                            val reps = set.repsDone?.toString() ?: EM_DASH
                            val text = when {
                                !lifting -> "$reps ${ex.movementType.repUnit}"
                                set.weightKg != null && set.weightKg > 0f -> "${formatWeightNumber(set.weightKg, unit)}×$reps"
                                else -> "BW×$reps"
                            }
                            withStyle(SpanStyle(color = color)) { append(text) }
                        }
                    },
                    style = AppText.body,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Front and back diagrams: primary muscles in `primary`, secondary in `inversePrimary`. */
@Composable
private fun MusclesSection(ex: ExerciseEntity) {
    if (ex.primaryMuscles.isEmpty() && ex.secondaryMuscles.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val fills = ex.secondaryMuscles.associateWith { scheme.inversePrimary } +
        ex.primaryMuscles.associateWith { scheme.primary }
    Section(title = "Muscles") {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            listOf(BodyView.Front, BodyView.Back).forEach { view ->
                BodyDiagram(
                    view = view,
                    fills = fills,
                    modifier = Modifier.weight(1f).height(DiagramHeight),
                )
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = scheme.onSurface)) {
                    append(ex.primaryMuscles.joinToString(", ") { it.displayName })
                }
                if (ex.secondaryMuscles.isNotEmpty()) {
                    withStyle(SpanStyle(color = scheme.onSurfaceVariant)) {
                        if (ex.primaryMuscles.isNotEmpty()) append(" · ")
                        append(ex.secondaryMuscles.joinToString(", ") { it.displayName })
                    }
                }
            },
            style = AppText.bodyS,
        )
    }
}

// ---- Formatting --------------------------------------------------------------------------

private fun isLifting(type: MovementType) = type == MovementType.Compound || type == MovementType.Isolation

private fun heroLabel(metric: ProgressMetric, type: MovementType): String = when (metric) {
    ProgressMetric.OneRepMax -> "Est. 1RM"
    else -> when (type) {
        MovementType.Cardio -> "Longest bout"
        MovementType.Mobility -> "Longest hold"
        else -> "Max reps"
    }
}

private fun metricTab(metric: ProgressMetric, type: MovementType): String = when (metric) {
    ProgressMetric.OneRepMax -> "1RM"
    ProgressMetric.Heaviest -> "Heaviest"
    ProgressMetric.Volume -> "Volume"
    ProgressMetric.Reps -> if (isLifting(type)) "Reps" else "Time"
}

private fun recordLabel(metric: ProgressMetric, type: MovementType): String = when (metric) {
    ProgressMetric.OneRepMax -> "Est. 1RM"
    ProgressMetric.Heaviest -> "Heaviest"
    ProgressMetric.Volume -> "Best set volume"
    ProgressMetric.Reps -> if (isLifting(type)) "Max reps" else "Longest"
}

/** The number alone, in the user's unit (house rule 9). */
private fun metricNumber(metric: ProgressMetric, value: Float, unit: WeightUnit): String = when (metric) {
    ProgressMetric.OneRepMax, ProgressMetric.Heaviest -> formatWeightNumber(value, unit)
    ProgressMetric.Volume -> formatVolumeNumber(value.toDouble(), unit)
    ProgressMetric.Reps -> value.roundToInt().toString()
}

/** Weight metrics in kg/lb; reps in reps, cardio in min, mobility in sec (issue 24). */
private fun metricUnit(metric: ProgressMetric, type: MovementType, unit: WeightUnit): String = when (metric) {
    ProgressMetric.Reps -> type.repUnit
    else -> unit.label
}

/** Latest value minus the earliest one within the three months before it; null if none. */
private fun threeMonthDelta(points: List<ProgressPoint>): Float? {
    val last = points.lastOrNull() ?: return null
    val base = points.firstOrNull { last.dateEpochDay - it.dateEpochDay <= 90 } ?: return null
    if (base === last) return null
    return last.value - base.value
}

private val DayMonth = DateTimeFormatter.ofPattern("d MMM")
private val DayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy")

/** `23 Nov`; older years carry the year. */
private fun shortDate(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return d.format(if (d.year == LocalDate.now().year) DayMonth else DayMonthYear)
}
