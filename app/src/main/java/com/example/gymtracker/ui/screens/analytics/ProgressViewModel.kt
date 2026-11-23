package com.example.gymtracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.SessionWithSets
import com.example.gymtracker.domain.AnalyticsWindow
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.domain.ExerciseMuscleMap
import com.example.gymtracker.domain.MuscleVolume
import com.example.gymtracker.domain.SessionWindow
import com.example.gymtracker.domain.SessionWindowSet
import com.example.gymtracker.domain.VolumeTrend
import com.example.gymtracker.domain.WeekVolume
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.domain.WeeksOnTarget
import com.example.gymtracker.domain.rangeEpochDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** The pinned range selector + pager position. [offset] = whole periods back from the current. */
data class PeriodSelection(val window: AnalyticsWindow = AnalyticsWindow.Week, val offset: Int = 0)

/** TRAINING DAYS hero and calendar (decision 7). */
data class Consistency(
    /** Distinct days trained inside the period (issue 27: days, not sessions). */
    val trainingDays: Int,
    val onTarget: WeeksOnTarget,
    val bestRun: Int,
    val target: Int,
    val trainedDays: Set<Long>,
    val metWeeks: Set<Long>,
)

/** VOLUME: the period's total against the 4-week reference, and the weekly bars. */
data class VolumeFigures(
    val metric: VolumeMetric,
    val total: Float,
    /** vs the four weeks before the period, scaled to its length; null for Today / no base. */
    val change: Float?,
    val bars: List<WeekVolume>,
    /** Monday of the running week, drawn outline-only; null when it isn't on the chart. */
    val runningWeek: Long?,
)

/** MUSCLES: working sets per muscle in the period (sets metric — the band's unit). */
data class MuscleFigures(
    val sets: Map<Muscle, Float>,
    /** 0..1 per muscle for the diagram's heat ramp. */
    val levels: Map<Muscle, Float>,
    val ranked: List<Pair<Muscle, Float>>,
    /** Muscles with no working sets in the period (issue 31: follows the period). */
    val neglected: List<Muscle>,
)

data class ProgressState(
    val selection: PeriodSelection,
    val period: ClosedRange<Long>,
    val today: Long,
    /** Any completed working set in the period; false → "Nothing in this period". */
    val hasWork: Boolean,
    val consistency: Consistency,
    val volume: VolumeFigures,
    val muscles: MuscleFigures,
    internal val periodSessions: List<SessionWithSets>,
    internal val exercisesById: Map<Long, ExerciseEntity>,
)

/** One exercise's share of a muscle in the breakdown sheet. */
data class ExerciseContribution(
    val exercise: ExerciseEntity,
    val sets: Int,
    val tonnageKg: Double,
    val bodyweightSets: Int,
)

data class MuscleBreakdown(
    val muscle: Muscle,
    val sets: Float,
    val tonnageKg: Float,
    val exercises: List<ExerciseContribution>,
)

/**
 * Progress (§4.12), was `AnalyticsViewModel`. Range + pager drive every figure; the volume
 * metric is local to this page (issue 30) and starts from the Settings preference.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val repository: GymRepository,
    private val prefs: PreferencesStore,
) : ViewModel() {

    private val _selection = MutableStateFlow(PeriodSelection())
    /** Drives the pinned selectors, which draw before the first emission (§4.12 Loading). */
    val selection: StateFlow<PeriodSelection> = _selection.asStateFlow()

    /** Local metric choice; null = follow the preference. */
    private val metricChoice = MutableStateFlow<VolumeMetric?>(null)
    private val metric = combine(metricChoice, prefs.volumeMetric) { local, pref -> local ?: pref }

    private val periodData = _selection.flatMapLatest { sel ->
        val today = DateUtil.todayEpochDay()
        val period = sel.window.rangeEpochDays(LocalDate.ofEpochDay(today), sel.offset)
        val firstBar = firstBarDay(period)
        // Four weeks before the first bar (and the period) feed the reference figures.
        val queryStart = minOf(period.start, firstBar) - VolumeTrend.REFERENCE_WEEKS * 7L - 7L
        repository.observeSessionsWithSetsInRange(queryStart, period.endInclusive)
            .map { Triple(sel, today, period) to it }
    }

    /** Null until the first Room emission (house rule 8). */
    val state: StateFlow<ProgressState?> = combine(
        periodData,
        // Deleted (custom) exercises included: their logged sets still count toward muscles.
        repository.observeAllExercisesIncludingDeleted(),
        repository.observeSessions(),
        prefs.weeklyTargetSessions,
        metric,
    ) { (key, sessions), exercises, allSessions, target, metric ->
        val (sel, today, period) = key
        build(sel, today, period, sessions, exercises, allSessions.map { it.dateEpochDay }, target, metric)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWindow(window: AnalyticsWindow) = _selection.update { PeriodSelection(window, 0) }
    fun previousPeriod() = _selection.update { it.copy(offset = it.offset + 1) }
    fun nextPeriod() = _selection.update { it.copy(offset = (it.offset - 1).coerceAtLeast(0)) }
    fun setMetric(value: VolumeMetric) { metricChoice.value = value }

    private fun build(
        sel: PeriodSelection,
        today: Long,
        period: ClosedRange<Long>,
        sessions: List<SessionWithSets>,
        exercises: List<ExerciseEntity>,
        sessionDates: List<Long>,
        target: Int,
        metric: VolumeMetric,
    ): ProgressState {
        val muscleMap = exercises.associate { it.id to ExerciseMuscleMap(it.primaryMuscles, it.secondaryMuscles) }
        val windows = sessions.map { it.toWindow() }
        val inPeriod = sessions.filter { it.session.dateEpochDay in period }

        val hasWork = inPeriod.any { s -> s.sets.any { it.isCompleted && it.setType != SetType.Warmup } }

        val consistency = Consistency(
            trainingDays = sessionDates.filter { it in period }.distinct().size,
            onTarget = WeeklyTarget.weeksOnTarget(sessionDates, target, today = minOf(period.endInclusive, today)),
            bestRun = WeeklyTarget.bestRunOfWeeks(sessionDates, target),
            target = target,
            trainedDays = sessionDates.toSet(),
            metWeeks = WeeklyTarget.weeksMeetingTarget(sessionDates, target),
        )

        val total = VolumeTrend.total(windows, period, metric)
        val runningWeek = WeeklyTarget.weekStart(today)
        val volume = VolumeFigures(
            metric = metric,
            total = total,
            change = if (sel.window == AnalyticsWindow.Today) null
            else VolumeTrend.change(total, VolumeTrend.referenceFor(windows, period, metric)),
            bars = VolumeTrend.weeklyBars(windows, firstBarDay(period), period.endInclusive, metric),
            runningWeek = runningWeek.takeIf { period.endInclusive >= it },
        )

        val sets = if (sel.window == AnalyticsWindow.Week) {
            MuscleVolume.weeklyWorkingSets(windows, period, muscleMap)
        } else {
            MuscleVolume.computeForWindow(windows, period, muscleMap, VolumeMetric.Sets)
        }.filterValues { it > 0f }
        val muscles = MuscleFigures(
            sets = sets,
            levels = MuscleVolume.normalize(sets),
            ranked = sets.entries.sortedByDescending { it.value }.map { it.key to it.value },
            neglected = Muscle.entries.filter { it !in sets },
        )

        return ProgressState(
            selection = sel,
            period = period,
            today = today,
            hasWork = hasWork,
            consistency = consistency,
            volume = volume,
            muscles = muscles,
            periodSessions = inPeriod,
            exercisesById = exercises.associateBy { it.id },
        )
    }

    /** Contributions to [muscle] in the current period, heaviest set count first. */
    fun breakdownFor(muscle: Muscle): MuscleBreakdown? {
        val s = state.value ?: return null
        val sets = s.periodSessions.flatMap { it.sets }.filter { it.isCompleted && it.setType != SetType.Warmup }
        val contributions = sets.groupBy { it.exerciseId }.mapNotNull { (id, exSets) ->
            val ex = s.exercisesById[id] ?: return@mapNotNull null
            if (muscle !in ex.primaryMuscles && muscle !in ex.secondaryMuscles) return@mapNotNull null
            ExerciseContribution(
                exercise = ex,
                sets = exSets.size,
                tonnageKg = exSets.sumOf { st ->
                    val r = st.repsDone
                    val w = st.weightKg
                    if (r != null && w != null) (r * w).toDouble() else 0.0
                },
                bodyweightSets = exSets.count { it.weightKg == null },
            )
        }.sortedByDescending { it.sets }
        val muscleMap = s.exercisesById.mapValues { ExerciseMuscleMap(it.value.primaryMuscles, it.value.secondaryMuscles) }
        val windows = s.periodSessions.map { it.toWindow() }
        return MuscleBreakdown(
            muscle = muscle,
            sets = s.muscles.sets[muscle] ?: 0f,
            tonnageKg = MuscleVolume.computeForWindow(windows, s.period, muscleMap, VolumeMetric.Tonnage)[muscle] ?: 0f,
            exercises = contributions,
        )
    }

    private companion object {
        /** The bar chart always shows at least this many weeks, so short ranges have context. */
        const val MIN_BARS = 8

        fun firstBarDay(period: ClosedRange<Long>): Long =
            minOf(period.start, period.endInclusive - (MIN_BARS - 1) * 7L)

        fun SessionWithSets.toWindow() = SessionWindow(
            dateEpochDay = session.dateEpochDay,
            sets = sets.map {
                SessionWindowSet(
                    exerciseId = it.exerciseId,
                    repsDone = it.repsDone,
                    weightKg = it.weightKg,
                    isCompleted = it.isCompleted,
                    isWarmup = it.setType == SetType.Warmup,
                )
            },
        )
    }
}
