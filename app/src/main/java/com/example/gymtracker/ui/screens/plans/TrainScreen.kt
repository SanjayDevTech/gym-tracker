package com.example.gymtracker.ui.screens.plans

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppFab
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.EmptyState
import com.example.gymtracker.ui.components.atoms.SectionHead
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.screens.plans.components.LiveStrip
import com.example.gymtracker.ui.screens.plans.components.PlanRow
import com.example.gymtracker.ui.screens.plans.components.TextEntrySheet
import com.example.gymtracker.ui.screens.plans.components.WeekHero
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Train — design-plan §4.1, bottom destination 1. Live strip (only with a draft), the week
 * against its target, then the plans as text on hairlines with a one-tap Start. Quick log is the
 * FAB, hidden while a session is live (Resume is the action then).
 *
 * Bar screen inset contract (§5): the NavHost already pads for the bar and consumes the
 * navigation-bar inset, so this Scaffold adds neither; the list's bottom padding is the FAB
 * clearance only while the FAB is shown.
 */
@Composable
fun TrainScreen(
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
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    val current = state
    val showFab = current != null && current.live == null

    Scaffold(
        topBar = {
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
            if (showFab) {
                AppFab(
                    text = "Quick log",
                    onClick = { haptics.light(); onQuickLog() },
                    icon = Icons.Outlined.Bolt,
                )
            }
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        // Loading: top bar (and the app's bar) only until the first emission (house rule 8).
        if (current == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.s,
                bottom = if (showFab) Sizes.fabClearance else Spacing.xxl,
            ),
        ) {
            current.live?.let { live ->
                item(key = "live") {
                    LiveStrip(
                        live = live,
                        onResume = { onResumeSession(live.planId) },
                    )
                    Spacer(Modifier.height(Spacing.section))
                }
            }
            item(key = "week") {
                WeekHero(week = current.week, unit = current.unit)
                Spacer(Modifier.height(Spacing.section))
            }
            item(key = "plans-head") {
                SectionHead(title = "Plans") {
                    if (current.plans.isNotEmpty()) {
                        val n = current.plans.size
                        Text(
                            "$n plan${if (n == 1) "" else "s"}",
                            style = AppText.bodyS,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.s))
                AppDivider()
            }
            if (current.plans.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No plans yet",
                        message = "Build one, or start a quick log.",
                        actionLabel = "New plan",
                        onAction = { showCreate = true },
                    )
                }
            } else {
                items(current.plans, key = { it.plan.id }) { card ->
                    PlanRow(
                        card = card,
                        onOpen = { onOpenPlan(card.plan.id) },
                        onStart = { haptics.light(); onLogPlan(card.plan.id) },
                    )
                    AppDivider()
                }
                // Creating a plan is the list's foot (§4.1).
                item(key = "new-plan") {
                    AppTextButton(text = "New plan", onClick = { showCreate = true }, icon = Icons.Outlined.Add)
                }
            }
        }
    }

    if (showCreate) {
        TextEntrySheet(
            title = "New plan",
            caption = "Name it; you add exercises next.",
            initial = "",
            placeholder = "Push day, Upper, Full body…",
            confirmLabel = "Create plan",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.createPlan(name) { onOpenPlan(it) }
            },
        )
    }
}
