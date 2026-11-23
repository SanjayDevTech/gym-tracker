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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppFab
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.ConfirmSheet
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.Radius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    viewModel: PlanListViewModel,
    snackbarHostState: SnackbarHostState,
    onSettings: () -> Unit,
    onQuickLog: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onLogPlan: (Long) -> Unit,
    onResumeSession: (planId: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlanEntity?>(null) }

    Scaffold(
        topBar = {
            // History and Progress are bar destinations, Exercises hangs off Progress, Import
            // lives on Plans & backup (§4.1 change 1): the only action left is Settings.
            AppTopBar(
                title = "Train",
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            AppFab(text = "Quick log", onClick = onQuickLog, icon = Icons.Outlined.Bolt)
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
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
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                )
            }
            if (state.plans.isEmpty()) {
                EmptyState(
                    title = "No plans yet",
                    message = "Create one, or just hit Quick log.",
                    actionLabel = "New plan",
                    onAction = { showCreate = true },
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.xs, bottom = Sizes.fabClearance),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    items(state.plans, key = { it.plan.id }) { card ->
                        PlanRow(
                            card = card,
                            onClick = { onOpenPlan(card.plan.id) },
                            onLog = { onLogPlan(card.plan.id) },
                            onDelete = { pendingDelete = card.plan },
                        )
                    }
                    // The top bar's + went with the overflow; creating a plan is the list's foot.
                    item(key = "new-plan") {
                        AppTextButton(text = "New plan", onClick = { showCreate = true }, icon = Icons.Outlined.Add)
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
        shape = RoundedCornerShape(Radius.lg),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
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
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Surface(
                    shape = RoundedCornerShape(Radius.md),
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
                Spacer(Modifier.height(Spacing.m))
                // Tags are text, not chips (§2).
                Eyebrow(
                    card.muscleGroups.take(4).joinToString(" · ") { it.label } +
                        if (card.muscleGroups.size > 4) " · +${card.muscleGroups.size - 4}" else "",
                )
            }

            Spacer(Modifier.height(Spacing.m))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
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
                AppTextButton(
                    text = "Log",
                    onClick = onLog,
                    enabled = card.exerciseCount > 0,
                    icon = Icons.Outlined.PlayArrow,
                )
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
                .padding(start = Spacing.xxl, end = Spacing.xxl, top = Spacing.s, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
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
                shape = RoundedCornerShape(Radius.md),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.End),
            ) {
                AppTextButton(text = "Cancel", onClick = onDismiss)
                AppButton(
                    text = "Create plan",
                    onClick = { onCreate(name) },
                    enabled = name.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun DeletePlanSheet(
    plan: PlanEntity,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmSheet(
        title = "Delete “${plan.name}”?",
        message = "This permanently removes the plan and all its exercises. Logged sessions are kept (their plan link will just be empty). This can't be undone.",
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        onDismiss = onCancel,
    )
}
