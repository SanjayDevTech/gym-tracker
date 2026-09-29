package com.example.gymtracker.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.SessionWithSets
import com.example.gymtracker.data.kgToDisplay
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.TagChip
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit, onOpenExercise: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val session by produceState<SessionWithSets?>(initialValue = null, sessionId) {
        container.repository.observeSessionWithSets(sessionId).collect { value = it }
    }
    val unit by container.prefs.weightUnit.collectAsStateWithLifecycle(initialValue = WeightUnit.Kg)
    var showDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val date = session?.session?.dateEpochDay?.let {
                        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
                    } ?: "Session"
                    Text(date)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (session != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        val s = session ?: return@Scaffold
        val groupedByExercise = s.sets.groupBy { it.exerciseId }.toList()
        val completed = s.sets.filter { it.isCompleted }
        val tonnage = completed
            .filter { it.setType != SetType.Warmup }
            .sumOf { set ->
                val r = set.repsDone
                val w = set.weightKg
                if (r != null && w != null) (r * w).toDouble() else 0.0
            }.toFloat()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "summary") {
                SummaryHeader(
                    exerciseCount = groupedByExercise.size,
                    completedSets = completed.size,
                    tonnage = tonnage,
                    isFreestyle = s.session.planId == null,
                    notes = s.session.notes,
                )
            }
            items(groupedByExercise, key = { it.first }) { (exerciseId, sets) ->
                ExerciseGroupCard(
                    exerciseId = exerciseId,
                    sets = sets,
                    unit = unit,
                    onOpenExercise = onOpenExercise,
                )
            }
        }
    }

    if (showDelete) {
        DeleteSessionSheet(
            onConfirm = {
                showDelete = false
                scope.launch {
                    container.repository.deleteSession(sessionId)
                    onBack()
                }
            },
            onCancel = { showDelete = false },
        )
    }
}

@Composable
private fun SummaryHeader(
    exerciseCount: Int,
    completedSets: Int,
    tonnage: Float,
    isFreestyle: Boolean,
    notes: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryStat("$exerciseCount", "exercises", Modifier.weight(1f))
                SummaryStat("$completedSets", "sets done", Modifier.weight(1f))
                SummaryStat(
                    if (tonnage > 0f) "%,.0f".format(tonnage) else "—",
                    "kg·reps",
                    Modifier.weight(1f),
                )
            }
            if (tonnage > 0f) {
                Text(
                    "Volume = weight × reps, summed across working sets (warm-ups excluded).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (isFreestyle) {
                Text(
                    "Freestyle session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (notes.isNotBlank()) {
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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

@Composable
private fun ExerciseGroupCard(
    exerciseId: Long,
    sets: List<com.example.gymtracker.data.db.LoggedSetEntity>,
    unit: WeightUnit,
    onOpenExercise: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val exercise by produceState<com.example.gymtracker.data.db.ExerciseEntity?>(initialValue = null, exerciseId) {
        container.repository.observeExercise(exerciseId).collect { value = it }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    exercise?.name ?: "Exercise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onOpenExercise(exerciseId) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = "View progress",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val type = exercise?.movementType ?: MovementType.Compound
            val countUnit = type.repUnit
            val isLifting = type == MovementType.Compound || type == MovementType.Isolation
            sets.sortedBy { it.setIndex }.forEach { set ->
                val count = set.repsDone?.toString() ?: "—"
                val w = set.weightKg?.kgToDisplay(unit)?.let { "$it ${unit.label}" }
                val suffix = if (isLifting) " · ${w ?: "BW"}" else ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Set ${set.setIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Non-working sets (warm-ups, drop sets) get a tinted pill so they're
                        // distinguishable from working sets at a glance in a dense list.
                        if (set.setType != SetType.Working) {
                            TagChip(
                                set.setType.label,
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    Text(
                        "$count $countUnit$suffix",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteSessionSheet(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Delete this session?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "All sets logged in this session will be removed. This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            }
        }
    }
}
