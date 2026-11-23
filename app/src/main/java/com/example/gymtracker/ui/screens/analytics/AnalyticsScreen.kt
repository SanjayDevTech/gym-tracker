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
import com.example.gymtracker.ui.components.EmptyState
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.components.Section
import com.example.gymtracker.domain.AnalyticsWindow
import com.example.gymtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Section(modifier = Modifier.fillMaxWidth(), title = "Consistency") {
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
                Section(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.width(Spacing.s))
                        Column {
                            Text("Neglected this week", style = MaterialTheme.typography.titleMedium)
                            Text(
                                neglected.take(5).joinToString { it.name } +
                                    if (neglected.size > 5) ", +${neglected.size - 5}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ExclusiveButtonGroup(
                options = AnalyticsWindow.values().toList(),
                selected = state.window,
                onSelect = { it?.let(vm::setWindow) },
                labelFor = { it.label },
                modifier = Modifier.fillMaxWidth(),
            )

            ExclusiveButtonGroup(
                options = VolumeMetric.values().toList(),
                selected = state.metric,
                onSelect = { it?.let(vm::setMetric) },
                labelFor = { it.label },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.sessionsCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.BarChart,
                        message = "Nothing in this window yet.",
                    )
                }
            } else {
                Section(modifier = Modifier.fillMaxWidth(), title = "Muscle map") {
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
                                VolumeMetric.Tonnage -> "%,.0f".format(state.totalVolume)
                            },
                            label = when (state.metric) {
                                VolumeMetric.Sets -> "working sets"
                                VolumeMetric.Tonnage -> "kg·reps total"
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.s),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
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
                Text(muscle.name, style = MaterialTheme.typography.titleLarge)
                val ready = breakdown
                if (ready == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else {
                BreakdownBody(breakdown = ready)
                }
            }
        }
    }
}

@Composable
private fun BreakdownBody(breakdown: MuscleBreakdown) {
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
                            "Tonnage: ${"%.0f".format(breakdown.value)} kg·reps",
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
                        VolumeMetric.Tonnage -> "${"%.0f".format(c.tonnage)} kg·reps" +
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
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(cold, hot))),
        )
        Text(
            "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
