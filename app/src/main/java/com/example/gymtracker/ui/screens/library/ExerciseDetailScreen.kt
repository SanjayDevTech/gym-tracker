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
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.components.LineChart
import com.example.gymtracker.ui.components.Section
import com.example.gymtracker.ui.components.StatTile
import com.example.gymtracker.ui.components.TagChip
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
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
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
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            if (state.readyToProgress) {
                Section(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.primary,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.padding(start = Spacing.s)) {
                            Text("Ready to progress", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "You've hit the top of the rep range on the last 2 sessions with reps to spare — no failure sets. Add weight next time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (state.recentSets.isNotEmpty()) {
                val r = state.records
                Text(
                    "Personal records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.hasWeightedHistory) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            StatTile(
                                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                                title = "Est. 1RM",
                                value = r.estimatedOneRepMaxKg.weightOrDash(weightUnit),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                icon = Icons.Outlined.FitnessCenter,
                                title = "Heaviest",
                                value = r.heaviestWeightKg.weightOrDash(weightUnit),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            StatTile(
                                icon = Icons.Outlined.BarChart,
                                title = "Best set volume",
                                value = r.bestSetVolumeKg.weightOrDash(weightUnit),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                icon = Icons.Outlined.Repeat,
                                title = "Max reps",
                                value = r.maxReps?.toString() ?: "—",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        StatTile(
                            icon = Icons.Outlined.Repeat,
                            title = "Max reps",
                            value = r.maxReps?.toString() ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "Sessions",
                            value = state.sessionCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Section(title = "Overview", modifier = Modifier.fillMaxWidth()) {
                LabeledLine("Movement", exercise.movementType.name)
                LabeledLine("Equipment", exercise.equipment.name)
                if (exercise.primaryMuscles.isNotEmpty()) {
                    MuscleTags("Primary", exercise.primaryMuscles.map { it.name })
                }
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    MuscleTags("Secondary", exercise.secondaryMuscles.map { it.name })
                }
            }

            if (exercise.cues.isNotBlank()) {
                Section(title = "Cues", modifier = Modifier.fillMaxWidth()) {
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
                Section(title = "Progress", modifier = Modifier.fillMaxWidth()) {
                    if (metricOptions.size > 1) {
                        ExclusiveButtonGroup(
                            options = metricOptions,
                            selected = metric,
                            onSelect = { if (it != null) metric = it },
                            labelFor = { it.label },
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

            Section(title = "Recent sets", modifier = Modifier.fillMaxWidth()) {
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
                        val w = set.weightKg?.kgToDisplay(weightUnit)?.let { "$it ${weightUnit.label}" }
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

private fun Float?.weightOrDash(unit: WeightUnit): String =
    this?.let { "${it.kgToDisplay(unit)} ${unit.label}" } ?: "—"

private fun formatMetric(metric: ProgressMetric, value: Float, unit: WeightUnit): String =
    when (metric) {
        ProgressMetric.Reps -> "${value.roundToInt()} reps"
        else -> "${value.kgToDisplay(unit)} ${unit.label}"
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleTags(label: String, muscles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            muscles.forEach { TagChip(it) }
        }
    }
}
