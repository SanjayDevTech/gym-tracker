package com.example.gymtracker.ui.screens.plans

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.SessionDraftEntity
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.ui.components.EmptyState
import com.example.gymtracker.ui.components.TagChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    viewModel: PlanListViewModel,
    onLibrary: () -> Unit,
    onQuickLog: () -> Unit,
    onHistory: () -> Unit,
    onAnalytics: () -> Unit,
    onSettings: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onLogPlan: (Long) -> Unit,
    onResumeSession: (planId: Long?) -> Unit,
    onImportPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlanEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans") },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "New plan")
                    }
                    IconButton(onClick = onAnalytics) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Analytics")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("History") },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                                onClick = { menuOpen = false; onHistory() },
                            )
                            DropdownMenuItem(
                                text = { Text("Exercise library") },
                                leadingIcon = { Icon(Icons.Outlined.LocalLibrary, contentDescription = null) },
                                onClick = { menuOpen = false; onLibrary() },
                            )
                            DropdownMenuItem(
                                text = { Text("Import plan") },
                                leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                                onClick = { menuOpen = false; onImportPlan() },
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                onClick = { menuOpen = false; onSettings() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onQuickLog,
                icon = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
                text = { Text("Quick log") },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.inProgressDraft?.let { draft ->
                ResumeSessionBanner(
                    draft = draft,
                    onResume = { onResumeSession(draft.planId) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.plans.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    EmptyState(
                        icon = Icons.Outlined.FitnessCenter,
                        message = "No plans yet.\nCreate one, or just hit Quick log.",
                    )
                    Button(onClick = { showCreate = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("New plan")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.plans, key = { it.plan.id }) { card ->
                        PlanRow(
                            card = card,
                            onClick = { onOpenPlan(card.plan.id) },
                            onLog = { onLogPlan(card.plan.id) },
                            onDelete = { pendingDelete = card.plan },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlanSheet(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                showCreate = false
                viewModel.createPlan(name) { onOpenPlan(it) }
            },
        )
    }

    pendingDelete?.let { plan ->
        DeletePlanSheet(
            plan = plan,
            onCancel = { pendingDelete = null },
            onConfirm = {
                viewModel.deletePlan(plan.id)
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun ResumeSessionBanner(
    draft: SessionDraftEntity,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elapsedMin = ((System.currentTimeMillis() - draft.startedAtMillis) / 60_000L).coerceAtLeast(0)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onResume),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Timer, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Session in progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (elapsedMin <= 0L) "Just started · tap to resume"
                    else "Started ${elapsedMin}m ago · tap to resume",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanRow(
    card: PlanCard,
    onClick: () -> Unit,
    onLog: () -> Unit,
    onDelete: () -> Unit,
) {
    val plan = card.plan
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildString {
                            append(
                                if (card.exerciseCount == 0) "No exercises"
                                else "${card.exerciseCount} exercise${if (card.exerciseCount == 1) "" else "s"}"
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (card.muscleGroups.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    card.muscleGroups.take(4).forEach { g -> TagChip(g.label) }
                    if (card.muscleGroups.size > 4) {
                        TagChip("+${card.muscleGroups.size - 4}")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        card.lastTrainedEpochDay?.let { "Last trained ${DateUtil.relativeDayLabel(it)}" }
                            ?: "Never logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onLog,
                    enabled = card.exerciseCount > 0,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Log")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New plan", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Give your plan a name. You can add exercises next.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Plan name") },
                placeholder = { Text("e.g. Push day, Upper, Full body") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { onCreate(name) },
                    enabled = name.isNotBlank(),
                ) { Text("Create plan") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeletePlanSheet(
    plan: PlanEntity,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Delete “${plan.name}”?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "This permanently removes the plan and all its exercises. Logged sessions are kept (their plan link will just be empty). This can't be undone.",
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
