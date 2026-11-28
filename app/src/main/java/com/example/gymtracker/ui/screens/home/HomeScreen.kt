package com.example.gymtracker.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.staggerIn
import com.example.gymtracker.ui.navigation.bottomBarClearance
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.screens.plans.PlanListViewModel
import com.example.gymtracker.ui.screens.plans.components.TextEntrySheet
import com.example.gymtracker.ui.theme.Spacing

/**
 * Home — the first tab, Foodprint's Today for training.
 *
 * Top to bottom: greeting + the Lift | Food switch, the workout in progress (only while one
 * runs), the week hero (sessions against the target, sets per day, three figures), the muscle
 * map for this week, your plans with a one-tap Start, the last few sessions, and the closing
 * quote. Starting a workout from anywhere is the "+" beside the tab bar.
 *
 * Loading: nothing but the header until the first emission, so empty states never flash.
 */
@Composable
fun HomeScreen(
    viewModel: PlanListViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenPlan: (Long) -> Unit,
    onStartPlan: (Long) -> Unit,
    onQuickLog: () -> Unit,
    onResumeSession: (planId: Long?) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // The hero's scroll offset while it is the first item; read at draw time.
    val heroIndex = if (state?.live != null) 1 else 0
    val parallax = {
        if (listState.firstVisibleItemIndex == heroIndex) listState.firstVisibleItemScrollOffset.toFloat() else 0f
    }
    val clearance = bottomBarClearance()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeHeader(name = state?.name.orEmpty()) },
        snackbarHost = { AppSnackbarHost(snackbarHostState, Modifier.padding(bottom = clearance - Spacing.l)) },
    ) { inner ->
        val s = state ?: return@Scaffold
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + Spacing.s,
                bottom = clearance,
                start = Spacing.gutter,
                end = Spacing.gutter,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            s.live?.let { live ->
                item("live") {
                    LiveCard(live = live, onResume = { haptics.light(); onResumeSession(live.planId) }, modifier = Modifier.staggerIn(0))
                }
            }
            item("week") {
                WeekHero(
                    week = s.week,
                    setsByDay = s.setsByDay,
                    unit = s.unit,
                    parallax = parallax,
                    modifier = Modifier.staggerIn(1),
                )
            }
            item("muscles") {
                MuscleMapCard(
                    muscles = s.muscles,
                    onOpen = { haptics.light(); onOpenProgress() },
                    modifier = Modifier.staggerIn(2),
                )
            }
            item("plans") {
                PlansCard(
                    plans = s.plans,
                    onOpenPlan = onOpenPlan,
                    onStartPlan = { haptics.light(); onStartPlan(it) },
                    onQuickLog = { haptics.light(); onQuickLog() },
                    onNewPlan = { showCreate = true },
                    onTemplates = onOpenTemplates,
                    modifier = Modifier.staggerIn(3),
                )
            }
            item("streak") {
                StreakTile(week = s.week, onClick = onOpenHistory, modifier = Modifier.staggerIn(2))
            }
            item("rhythm") {
                RhythmHeatmap(start = s.heatStart, sets = s.heatSets, onClick = onOpenHistory, modifier = Modifier.staggerIn(5))
            }
            item("recent") {
                RecentCard(
                    recent = s.recent,
                    unit = s.unit,
                    onOpenSession = onOpenSession,
                    onSeeAll = onOpenHistory,
                    modifier = Modifier.staggerIn(4),
                )
            }
            item("quote") { ClosingQuote() }
        }
    }

    if (showCreate) {
        TextEntrySheet(
            title = "New plan",
            caption = "Name it; you add exercises next.",
            initial = "",
            placeholder = "Push day, Upper, Full body…",
            confirmLabel = "Create plan",
            secondaryLabel = "Start from a template",
            onSecondary = {
                showCreate = false
                onOpenTemplates()
            },
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.createPlan(name) { onOpenPlan(it) }
            },
        )
    }
}
