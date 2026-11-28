package com.example.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.SessionEntity
import com.example.gymtracker.data.db.SessionWithSets
import com.example.gymtracker.domain.WeeklyTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** One History row: plan name (or "Quick log"), counts, duration, volume. */
data class HistoryRow(
    val sessionId: Long,
    val dateEpochDay: Long,
    /** Plan name, or null for an ad-hoc session (rendered "Quick log"). */
    val planName: String?,
    /** Exercises with at least one completed set (the one definition used everywhere). */
    val exerciseCount: Int,
    val doneSets: Int,
    /** Working + failure volume in kg; warm-ups out. */
    val volumeKg: Double,
    /** Null for sessions logged before v5 (no start time). */
    val durationMillis: Long?,
)

/** One ISO week (Monday start) of sessions, newest first. */
data class HistoryWeek(
    val weekStartEpochDay: Long,
    val sessions: List<HistoryRow>,
)

data class HistoryState(
    val weeks: List<HistoryWeek>,
    val unit: WeightUnit,
)

/**
 * History. Sessions grouped by ISO week. Starts at `null` so the
 * screen draws only its top bar until Room's first emission; an empty list is a real empty.
 */
class HistoryViewModel(
    repository: GymRepository,
    prefs: PreferencesStore,
) : ViewModel() {
    val state: StateFlow<HistoryState?> = combine(
        repository.observeAllSessionsWithSets(),
        repository.observePlans(),
        prefs.weightUnit,
    ) { sessions, plans, unit ->
        val planNames = plans.associate { it.id to it.name }
        val weeks = sessions
            .map { it.toRow(planNames) }
            .groupBy { WeeklyTarget.weekStart(it.dateEpochDay) }
            .map { (week, rows) -> HistoryWeek(week, rows) }
            .sortedByDescending { it.weekStartEpochDay }
        HistoryState(weeks, unit)
    }
        // Every session with every set is folded here on each emission: keep it off Main.
        .flowOn(Dispatchers.Default)
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
}

internal fun SessionWithSets.toRow(planNames: Map<Long, String>): HistoryRow {
    val done = sets.filter { it.isCompleted }
    return HistoryRow(
        sessionId = session.id,
        dateEpochDay = session.dateEpochDay,
        planName = session.planId?.let(planNames::get),
        exerciseCount = done.map { it.exerciseId }.distinct().size,
        doneSets = done.size,
        volumeKg = done.filter { it.setType != SetType.Warmup }
            .sumOf { ((it.weightKg ?: 0f) * (it.repsDone ?: 0)).toDouble() },
        durationMillis = session.durationMillis(),
    )
}

/** `loggedAt − startedAt`; null when the session predates v5 or the clock went backwards. */
internal fun SessionEntity.durationMillis(): Long? =
    startedAtMillis?.let { loggedAtMillis - it }?.takeIf { it >= 0 }
