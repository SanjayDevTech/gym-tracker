package com.example.gymtracker.ui.screens.plans

import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.SetType
import com.example.gymtracker.domain.ExerciseMuscleMap
import com.example.gymtracker.domain.MuscleVolume
import com.example.gymtracker.domain.SessionWindow
import com.example.gymtracker.domain.SessionWindowSet
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
import com.example.gymtracker.domain.currentRunOfWeeks
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

/** A plan plus the at-a-glance facts its row shows. */
data class PlanCard(
    val plan: PlanEntity,
    val exerciseCount: Int,
    val muscleGroups: List<MuscleGroup>,
    val lastTrainedEpochDay: Long?,
    /** Volume of the plan's last ≤ 8 sessions, oldest first (the sparkline). */
    val recentVolumes: List<Double>,
    /** Median length of the plan's timed sessions; null when none is timed. */
    val typicalDurationMillis: Long?,
)

/** The running draft as the live strip reads it. */
data class LiveSession(
    val planId: Long?,
    val title: String,
    val startedAtMillis: Long,
    val doneSets: Int,
    val totalSets: Int,
    /** Wall-clock of the last completed set; 0 while rest is cleared. */
    val lastSetCompletedAtMillis: Long,
    /** Wall-clock the workout was paused at; 0 = running. */
    val pausedAtMillis: Long = 0L,
)

/** This week against the weekly target. */
data class WeekSummary(
    val sessions: Int,
    val target: Int,
    /** Mon = 0 … Sun = 6: days of this week with at least one session. */
    val trainedDays: Set<Int>,
    /** Mon = 0 … Sun = 6. */
    val todayIndex: Int,
    val onTarget: WeeksOnTarget,
    /** Weeks in a row on target (see [WeeklyTarget.currentRunOfWeeks]). */
    val streakWeeks: Int,
    /** Longest run of weeks on target, ever. */
    val bestRunWeeks: Int,
    val volumeKg7d: Double,
    val sets7d: Int,
)

/** One of Home's recent sessions. */
data class RecentSession(
    val sessionId: Long,
    val title: String,
    val dateEpochDay: Long,
    val doneSets: Int,
    val volumeKg: Double,
    val durationMillis: Long?,
)

/** This week's working sets per muscle, for Home's body map. */
data class WeekMuscles(
    /** 0..1 per trained muscle, for the heat ramp. */
    val levels: Map<Muscle, Float>,
    /** Most-trained first, with working sets. */
    val ranked: List<Pair<Muscle, Float>>,
)

data class TrainState(
    val live: LiveSession?,
    val week: WeekSummary,
    val plans: List<PlanCard>,
    val unit: WeightUnit,
    val name: String,
    /** Working sets per day this week, Mon = 0 … Sun = 6. */
    val setsByDay: List<Int>,
    val muscles: WeekMuscles,
    val recent: List<RecentSession>,
    /** Monday that starts the heatmap; [heatSets] holds one entry per day from it. */
    val heatStart: Long,
    /** Completed sets per day over the last [HEAT_WEEKS] weeks, oldest first. */
    val heatSets: List<Int>,
)

/** Weeks on Home's heatmap — Foodprint's rhythm, 18 weeks. */
const val HEAT_WEEKS = 18

class PlanListViewModel(
    private val repository: GymRepository,
    sessionDao: SessionDao,
    prefs: PreferencesStore,
) : ViewModel() {

    private val settings = combine(prefs.weeklyTargetSessions, prefs.weightUnit, prefs.userName) { target, unit, name ->
        Triple(target, unit, name)
    }

    // shortcut: the week is fixed when Home opens, so a session left open past midnight on Sunday
    // keeps showing last week until Home is reopened.
    private val weekStart = WeeklyTarget.weekStart(DateUtil.todayEpochDay())
    private val thisWeek = combine(
        repository.observeSessionsWithSetsInRange(weekStart, weekStart + 6),
        repository.observeAllExercisesIncludingDeleted(),
    ) { sessions, exercises -> sessions to exercises }

    /** Null until the first Room emission: no "No plans yet" flash. */
    val state: StateFlow<TrainState?> = combine(
        combine(
            repository.observePlans(),
            repository.observeAllPlanned(),
            repository.observeExercises(),
            sessionDao.observeSessionVolumes(),
            repository.observeSessionDraft(),
        ) { plans, planned, exercises, sessions, draft -> Inputs(plans, planned, exercises, sessions, draft) },
        settings,
        thisWeek,
    ) { inputs, (target, unit, name), (weekSessions, allExercises) ->
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
            streakWeeks = WeeklyTarget.currentRunOfWeeks(days, target, today),
            bestRunWeeks = WeeklyTarget.bestRunOfWeeks(days, target),
            volumeKg7d = volume7d,
            sets7d = sets7d,
        )
        val muscleMap = allExercises.associate { it.id to ExerciseMuscleMap(it.primaryMuscles, it.secondaryMuscles) }
        val windows = weekSessions.map { s ->
            SessionWindow(
                dateEpochDay = s.session.dateEpochDay,
                sets = s.sets.map {
                    SessionWindowSet(it.exerciseId, it.repsDone, it.weightKg, it.isCompleted, it.setType == SetType.Warmup)
                },
            )
        }
        val muscleSets = MuscleVolume.computeForWindow(windows, weekStart..weekStart + 6, muscleMap)
            .filterValues { it > 0f }
        val setsByDay = List(7) { i ->
            weekSessions.filter { it.session.dateEpochDay == weekStart + i }
                .sumOf { s -> s.sets.count { it.isCompleted && it.setType != SetType.Warmup } }
        }
        val planNames = inputs.plans.associate { it.id to it.name }
        val recent = inputs.sessions.sortedByDescending { it.loggedAtMillis }.take(RECENT_COUNT).map {
            RecentSession(
                sessionId = it.sessionId,
                title = it.planId?.let(planNames::get) ?: "Quick log",
                dateEpochDay = it.dateEpochDay,
                doneSets = it.doneSets,
                volumeKg = it.volumeKg,
                durationMillis = it.startedAtMillis?.let { start -> (it.loggedAtMillis - start).takeIf { d -> d > 0 } },
            )
        }
        val heatStart = weekStart - (HEAT_WEEKS - 1) * 7L
        val setsByDate = inputs.sessions.groupBy { it.dateEpochDay }.mapValues { (_, v) -> v.sumOf { it.doneSets } }
        TrainState(
            heatStart = heatStart,
            heatSets = List(HEAT_WEEKS * 7) { setsByDate[heatStart + it] ?: 0 },
            live = inputs.draft?.toLive(),
            week = week,
            plans = cards,
            unit = unit,
            name = name,
            setsByDay = setsByDay,
            muscles = WeekMuscles(
                levels = MuscleVolume.normalize(muscleSets),
                ranked = muscleSets.entries.sortedByDescending { it.value }.map { it.key to it.value },
            ),
            recent = recent,
        )
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
        const val RECENT_COUNT = 3
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
                pausedAtMillis = payload?.pausedAtMillis ?: 0L,
            )
        }
    }
}
