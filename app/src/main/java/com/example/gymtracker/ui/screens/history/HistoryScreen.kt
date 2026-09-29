package com.example.gymtracker.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.EmptyState
import com.example.gymtracker.ui.components.TagChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenSession: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: HistoryViewModel = viewModel(
        factory = AppViewModelFactory(
            mapOf(HistoryViewModel::class.java to {
                HistoryViewModel(container.repository)
            })
        )
    )
    val groups by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    message = "Nothing logged yet.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEach { group ->
                    item(key = "day-${group.dateEpochDay}") {
                        DayHeader(group.dateEpochDay, sessionCount = group.sessions.size)
                    }
                    items(group.sessions, key = { it.session.id }) { summary ->
                        SessionCard(
                            summary = summary,
                            onClick = { onOpenSession(summary.session.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(epochDay: Long, sessionCount: Int) {
    val date = LocalDate.ofEpochDay(epochDay)
    val label = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (sessionCount > 1) {
            Text(
                "$sessionCount sessions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(
    summary: SessionSummary,
    onClick: () -> Unit,
) {
    val isFreestyle = summary.session.planId == null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    summary.topExerciseName?.let {
                        if (summary.exerciseCount > 1) "$it +${summary.exerciseCount - 1} more" else it
                    } ?: if (isFreestyle) "Freestyle session" else "Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (isFreestyle) TagChip("Freestyle")
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MetaStat(Icons.Outlined.FitnessCenter, "${summary.exerciseCount}", "exercises")
                MetaStat(Icons.Outlined.Tag, "${summary.completedSets}", "sets")
                if (summary.tonnage > 0f) {
                    MetaStat(null, "%,.0f".format(summary.tonnage), "kg·reps")
                }
            }

            if (summary.muscleGroups.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    summary.muscleGroups.take(4).forEach { g -> TagChip(g.label) }
                    if (summary.muscleGroups.size > 4) TagChip("+${summary.muscleGroups.size - 4}")
                }
            }

            if (summary.session.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    summary.session.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetaStat(icon: ImageVector?, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
