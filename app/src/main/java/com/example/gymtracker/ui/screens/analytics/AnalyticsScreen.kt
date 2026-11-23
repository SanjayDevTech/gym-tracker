package com.example.gymtracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.BodyDiagram
import com.example.gymtracker.ui.components.BodyView
import com.example.gymtracker.ui.components.ConsistencyCalendar
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.Spinner
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.format.formatVolume
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.domain.AnalyticsWindow
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.Radius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onOpenExercises: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val vm: AnalyticsViewModel = viewModel(
        factory = AppViewModelFactory(
            mapOf(AnalyticsViewModel::class.java to {
                AnalyticsViewModel(container.repository, container.prefs)
            })
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val streak by vm.currentStreak.collectAsStateWithLifecycle()
    val sessionDays by vm.allSessionDays.collectAsStateWithLifecycle()
    val neglected by vm.neglectedMuscles.collectAsStateWithLifecycle()
    var drilledMuscle by remember { mutableStateOf<Muscle?>(null) }
    val unit by container.prefs.weightUnit.collectAsStateWithLifecycle(initialValue = WeightUnit.Kg)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Progress",
                actions = { AppTextButton(text = "Exercises", onClick = onOpenExercises) },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Section(title = "Consistency") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    HeaderStat(
                        icon = Icons.Outlined.LocalFireDepartment,
                        value = "$streak",
                        label = "day streak",
                        modifier = Modifier.weight(1f),
                    )
                    HeaderStat(
                        icon = Icons.Outlined.BarChart,
                        value = sessionDays.size.toString(),
                        label = "sessions logged",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(Spacing.s))
                ConsistencyCalendar(
                    sessionDays = sessionDays,
                    weeks = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                )
                Text(
                    "Last 12 weeks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (neglected.isNotEmpty() && sessionDays.isNotEmpty()) {
                Section(title = "Neglected this week") {
                    Text(
                        neglected.take(5).joinToString { it.displayName } +
                            if (neglected.size > 5) ", +${neglected.size - 5}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Column {
                UnderlineSelector(
                    options = AnalyticsWindow.entries.toList(),
                    selected = state.window,
                    onSelect = vm::setWindow,
                    label = { it.label },
                )
                UnderlineSelector(
                    options = VolumeMetric.entries.toList(),
                    selected = state.metric,
                    onSelect = vm::setMetric,
                    label = { it.label },
                )
            }

            if (state.sessionsCount == 0) {
                EmptyState(
                    title = "Nothing in this window yet",
                    message = "Completed sets from this period will show up here.",
                )
            } else {
                Section(title = "Muscle map") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        HeaderStat(
                            icon = Icons.Outlined.BarChart,
                            value = "${state.sessionsCount}",
                            label = if (state.sessionsCount == 1) "session" else "sessions",
                            modifier = Modifier.weight(1f),
                        )
                        HeaderStat(
                            icon = Icons.Outlined.FitnessCenter,
                            value = when (state.metric) {
                                VolumeMetric.Sets -> "%,d".format(state.totalVolume.toInt())
                                VolumeMetric.Tonnage -> formatVolumeNumber(state.totalVolume.toDouble(), unit)
                            },
                            label = when (state.metric) {
                                VolumeMetric.Sets -> "working sets"
                                VolumeMetric.Tonnage -> "${unit.label} total"
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppDivider(Modifier.padding(vertical = Spacing.s))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        DiagramColumn(
                            label = "Front",
                            view = BodyView.Front,
                            volumes = state.normalized,
                            selected = drilledMuscle,
                            onMuscleTap = { drilledMuscle = it },
                            modifier = Modifier.weight(1f),
                        )
                        DiagramColumn(
                            label = "Back",
                            view = BodyView.Back,
                            volumes = state.normalized,
                            selected = drilledMuscle,
                            onMuscleTap = { drilledMuscle = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HeatLegend()
                    Text(
                        "Tap a muscle for the exercises feeding it.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }

    val muscle = drilledMuscle
    if (muscle != null) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
        // Computed off the main thread; null until the background fold returns.
        val breakdown by produceState<MuscleBreakdown?>(initialValue = null, muscle, state) {
            value = vm.breakdownFor(muscle)
        }
        ModalBottomSheet(onDismissRequest = { drilledMuscle = null }, sheetState = sheetState) {
            Column(
                modifier = Modifier.padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(muscle.displayName, style = MaterialTheme.typography.titleLarge)
                val ready = breakdown
                if (ready == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                        contentAlignment = Alignment.Center,
                    ) {
                        Spinner()
                    }
                } else {
                    BreakdownBody(breakdown = ready, unit = unit)
                }
            }
        }
    }
}

@Composable
private fun BreakdownBody(breakdown: MuscleBreakdown, unit: WeightUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                when (breakdown.metric) {
                    VolumeMetric.Sets -> {
                        val totalSets = breakdown.exercises.sumOf { it.setCount }
                        Text(
                            "$totalSets working set${if (totalSets == 1) "" else "s"} this window",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    VolumeMetric.Tonnage -> {
                        Text(
                            "Tonnage: ${formatVolume(breakdown.value.toDouble(), unit)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (breakdown.bodyweightSets > 0) {
                            Text(
                                "${breakdown.bodyweightSets} bodyweight set${if (breakdown.bodyweightSets == 1) "" else "s"} not counted — switch to Sets to include them",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                breakdown.exercises.forEach { c ->
                    val detail = when (breakdown.metric) {
                        VolumeMetric.Sets -> "${c.setCount} set${if (c.setCount == 1) "" else "s"}"
                        VolumeMetric.Tonnage -> formatVolume(c.tonnage.toDouble(), unit) +
                            if (c.bodyweightSets > 0) " · ${c.bodyweightSets} BW" else ""
                    }
                    Text(
                        "${c.exercise.name}: $detail",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (breakdown.exercises.isEmpty()) {
                    Text(
                        "No completed sets contributing to this muscle in the selected window.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
    }
}

@Composable
private fun HeaderStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagramColumn(
    label: String,
    view: BodyView,
    volumes: Map<Muscle, Float>,
    selected: Muscle?,
    onMuscleTap: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BodyDiagram(
            view = view,
            volumes = volumes,
            selectedMuscle = selected,
            onMuscleTap = onMuscleTap,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(100f / 220f),
        )
    }
}

@Composable
private fun HeatLegend() {
    val cold = MaterialTheme.colorScheme.surfaceVariant
    val hot = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(Brush.horizontalGradient(listOf(cold, hot))),
        )
        Text(
            "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
