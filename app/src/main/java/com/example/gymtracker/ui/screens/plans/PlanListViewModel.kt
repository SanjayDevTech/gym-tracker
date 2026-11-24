package com.example.gymtracker.ui.screens.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MuscleGroup
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.PlanEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.SessionDao
import com.example.gymtracker.data.db.SessionDraftEntity
import com.example.gymtracker.data.db.SessionDraftPayload
import com.example.gymtracker.data.db.SessionVolume
import com.example.gymtracker.data.toMuscleGroups
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.domain.WeeksOnTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

/** A plan plus the at-a-glance facts its Train row shows (§3 `PlanRow`). */
data class PlanCard(
    val plan: PlanEntity,
    val exerciseCount: Int,
    val muscleGroups: List<MuscleGroup>,
    val lastTrainedEpochDay: Long?,
    /** Volume of the plan's last ≤ 8 sessions, oldest first (S2-a sparkline). */
    val recentVolumes: List<Double>,
    /** Median length of the plan's timed sessions (decision 4); null when none is timed. */
    val typicalDurationMillis: Long?,
)

/** The running draft as Train's live strip reads it (§3 `LiveStrip`). */
data class LiveSession(
    val planId: Long?,
    val title: String,
    val startedAtMillis: Long,
    val doneSets: Int,
    val totalSets: Int,
    /** Wall-clock of the last completed set; 0 while rest is cleared. */
    val lastSetCompletedAtMillis: Long,
)

/** This week against the weekly target (decision 7, §3 `WeekHero`). */
data class WeekSummary(
    val sessions: Int,
    val target: Int,
    /** Mon = 0 … Sun = 6: days of this week with at least one session. */
    val trainedDays: Set<Int>,
    /** Mon = 0 … Sun = 6. */
    val todayIndex: Int,
    val onTarget: WeeksOnTarget,
    val volumeKg7d: Double,
    val sets7d: Int,
)

data class TrainState(
    val live: LiveSession?,
    val week: WeekSummary,
    val plans: List<PlanCard>,
    val unit: WeightUnit,
)

class PlanListViewModel(
    private val repository: GymRepository,
    sessionDao: SessionDao,
    prefs: PreferencesStore,
) : ViewModel() {

    private val settings = combine(prefs.weeklyTargetSessions, prefs.weightUnit) { target, unit -> target to unit }

    /** Null until the first Room emission (house rule 8): no "No plans yet" flash. */
    val state: StateFlow<TrainState?> = combine(
        combine(
            repository.observePlans(),
            repository.observeAllPlanned(),
            repository.observeExercises(),
            sessionDao.observeSessionVolumes(),
            repository.observeSessionDraft(),
        ) { plans, planned, exercises, sessions, draft -> Inputs(plans, planned, exercises, sessions, draft) },
        settings,
    ) { inputs, (target, unit) ->
        val today = DateUtil.todayEpochDay()
        val exById = inputs.exercises.associateBy { it.id }
        val plannedByPlan = inputs.planned.groupBy { it.planId }
        val sessionsByPlan = inputs.sessions.filter { it.planId != null }.groupBy { it.planId!! }
        val volumesByPlan = PlanStats.recentVolumesByPlan(inputs.sessions)

        val cards = inputs.plans.map { plan ->
            val rows = plannedByPlan[plan.id].orEmpty()
            val planSessions = sessionsByPlan[plan.id].orEmpty()
            PlanCard(
                plan = plan,
                exerciseCount = rows.size,
                muscleGroups = rows.flatMap { exById[it.exerciseId]?.primaryMuscles.orEmpty() }.toMuscleGroups(),
                lastTrainedEpochDay = planSessions.maxOfOrNull { it.dateEpochDay },
                recentVolumes = volumesByPlan[plan.id].orEmpty(),
                typicalDurationMillis = PlanStats.medianDurationMillis(planSessions),
            )
        }

        val days = inputs.sessions.map { it.dateEpochDay }
        val weekStart = WeeklyTarget.weekStart(today)
        val (volume7d, sets7d) = PlanStats.lastSevenDays(inputs.sessions, today)
        val week = WeekSummary(
            sessions = WeeklyTarget.sessionsInWeek(days, today),
            target = target,
            trainedDays = days.filter { it in weekStart until weekStart + 7 }.map { (it - weekStart).toInt() }.toSet(),
            todayIndex = (today - weekStart).toInt(),
            onTarget = WeeklyTarget.weeksOnTarget(days, target, today),
            volumeKg7d = volume7d,
            sets7d = sets7d,
        )
        TrainState(live = inputs.draft?.toLive(), week = week, plans = cards, unit = unit)
    }
        // Folds every session and decodes the draft JSON on each emission (Train is the start
        // destination, so this ran on Main during startup): keep it off Main.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun createPlan(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertPlan(
                PlanEntity(
                    clientUuid = UUID.randomUUID().toString(),
                    name = name.trim().ifBlank { "Untitled plan" },
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
            onCreated(id)
        }
    }

    private data class Inputs(
        val plans: List<PlanEntity>,
        val planned: List<PlannedExerciseEntity>,
        val exercises: List<ExerciseEntity>,
        val sessions: List<SessionVolume>,
        val draft: SessionDraftEntity?,
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        fun SessionDraftEntity.toLive(): LiveSession {
            val payload = runCatching {
                json.decodeFromString(SessionDraftPayload.serializer(), payloadJson)
            }.getOrNull()
            val sets = payload?.groups?.flatMap { it.sets }.orEmpty()
            return LiveSession(
                planId = planId,
                title = payload?.planName ?: "Quick log",
                startedAtMillis = startedAtMillis,
                doneSets = sets.count { it.isCompleted },
                totalSets = sets.size,
                lastSetCompletedAtMillis = payload?.lastSetCompletedAtMillis ?: 0L,
            )
        }
    }
}
