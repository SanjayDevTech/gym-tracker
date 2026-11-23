package com.example.gymtracker.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.kgToDisplay
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.domain.ProgressMetric
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.components.LineChart
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.StatRow
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatWeight
import com.example.gymtracker.ui.format.formatWeightNumber
import com.example.gymtracker.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel,
    weightUnit: WeightUnit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercise

    Scaffold(
        topBar = {
            AppTopBar(title = exercise?.name ?: "Exercise", onBack = onBack)
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        if (exercise == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            if (state.readyToProgress) {
                Section(title = "Ready to progress") {
                    Text(
                        "You've hit the top of the rep range on the last 2 sessions with reps to spare — no failure sets. Add weight next time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.recentSets.isNotEmpty()) {
                val r = state.records
                Section(title = "Personal records") {
                    if (state.hasWeightedHistory) {
                        StatRow(
                            label = "Est. 1RM",
                            value = formatWeightNumber(r.estimatedOneRepMaxKg, weightUnit),
                            unit = r.estimatedOneRepMaxKg?.let { weightUnit.label },
                        )
                        StatRow(
                            label = "Heaviest",
                            value = formatWeightNumber(r.heaviestWeightKg, weightUnit),
                            unit = r.heaviestWeightKg?.let { weightUnit.label },
                        )
                        StatRow(
                            label = "Best set volume",
                            value = formatWeightNumber(r.bestSetVolumeKg, weightUnit),
                            unit = r.bestSetVolumeKg?.let { weightUnit.label },
                        )
                        StatRow(label = "Max reps", value = r.maxReps?.toString() ?: EM_DASH)
                    } else {
                        StatRow(label = "Max reps", value = r.maxReps?.toString() ?: EM_DASH)
                        StatRow(label = "Sessions", value = state.sessionCount.toString())
                    }
                }
            }

            Section(title = "Overview") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    LabeledLine("Movement", exercise.movementType.displayName)
                    LabeledLine("Equipment", exercise.equipment.displayName)
                    if (exercise.primaryMuscles.isNotEmpty()) {
                        MuscleTags("Primary", exercise.primaryMuscles.map { it.displayName })
                    }
                    if (exercise.secondaryMuscles.isNotEmpty()) {
                        MuscleTags("Secondary", exercise.secondaryMuscles.map { it.displayName })
                    }
                }
            }

            if (exercise.cues.isNotBlank()) {
                Section(title = "Cues") {
                    Text(exercise.cues, style = MaterialTheme.typography.bodyMedium)
                }
            }

            val metricOptions = if (state.hasWeightedHistory) {
                listOf(ProgressMetric.OneRepMax, ProgressMetric.Heaviest, ProgressMetric.Volume, ProgressMetric.Reps)
            } else {
                listOf(ProgressMetric.Reps)
            }
            var metric by remember(exercise.id, state.hasWeightedHistory) {
                mutableStateOf(metricOptions.first())
            }
            val points = state.seriesByMetric[metric].orEmpty()
            if (points.isNotEmpty()) {
                Section(title = "Progress") {
                    if (metricOptions.size > 1) {
                        UnderlineSelector(
                            options = metricOptions,
                            selected = metric,
                            onSelect = { metric = it },
                            label = { it.label },
                        )
                        Spacer(Modifier.height(Spacing.s))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        MetricReadout("Current", formatMetric(metric, points.last().value, weightUnit))
                        MetricReadout("Best", formatMetric(metric, points.maxOf { it.value }, weightUnit))
                    }
                    Spacer(Modifier.height(Spacing.s))
                    LineChart(
                        values = points.map { it.value },
                        referenceBand = if (metric == ProgressMetric.Reps) 8f..12f else null,
                    )
                    Text(
                        "${state.sessionCount} session${if (state.sessionCount == 1) "" else "s"} logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Section(title = "Recent sets") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    if (state.recentSets.isEmpty()) {
                        Text(
                            "No logged history yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val type = exercise.movementType
                        val countUnit = type.repUnit
                        val isLifting = type == MovementType.Compound || type == MovementType.Isolation
                        state.recentSets.take(10).forEach { set ->
                            val c = set.repsDone?.toString() ?: "—"
                            val w = set.weightKg?.let { formatWeight(it, weightUnit) }
                            val tail = if (isLifting) " · ${w ?: "BW"}" else ""
                            LabeledLine("Set ${set.setIndex + 1}", "$c $countUnit$tail")
                        }
                        if (state.recentSets.size > 10) {
                            Text(
                                "Showing the 10 most recent of ${state.recentSets.size} logged sets",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMetric(metric: ProgressMetric, value: Float, unit: WeightUnit): String =
    when (metric) {
        ProgressMetric.Reps -> "${value.roundToInt()} reps"
        else -> formatWeight(value, unit)
    }

@Composable
private fun MetricReadout(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LabeledLine(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Tags are text, not chips (§2): `CHEST · TRICEPS` in eyebrow. */
@Composable
private fun MuscleTags(label: String, muscles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Eyebrow(muscles.joinToString(" · "))
    }
}
