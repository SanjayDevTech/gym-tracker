package com.example.gymtracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.domain.AnalyticsWindow
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.domain.ExerciseMuscleMap
import com.example.gymtracker.domain.MuscleVolume
import com.example.gymtracker.domain.SessionWindow
import com.example.gymtracker.domain.SessionWindowSet
import com.example.gymtracker.domain.StreakCalc
import com.example.gymtracker.domain.rangeEpochDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MuscleBreakdown(
    val muscle: Muscle,
    val metric: VolumeMetric,
    val value: Float,
    val bodyweightSets: Int,
    val exercises: List<ExerciseContribution>,
)

data class ExerciseContribution(
    val exercise: ExerciseEntity,
    val setCount: Int,
    val tonnage: Float,
    val bodyweightSets: Int,
)

data class AnalyticsState(
    val window: AnalyticsWindow = AnalyticsWindow.Week,
    val metric: VolumeMetric = VolumeMetric.Sets,
    val normalized: Map<Muscle, Float> = emptyMap(),
    val totalVolume: Float = 0f,
    val bodyweightSetCounts: Map<Muscle, Int> = emptyMap(),
    val sessionsCount: Int = 0,
    val rawVolume: Map<Muscle, Float> = emptyMap(),
    val exercisesById: Map<Long, ExerciseEntity> = emptyMap(),
    val setsByExercisePerMuscle: Map<Long, List<LoggedSetEntity>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: GymRepository,
    private val prefs: PreferencesStore,
) : ViewModel() {

    private val window = MutableStateFlow(AnalyticsWindow.Week)

    val allSessionDays: StateFlow<Set<Long>> = repository.observeSessionDays()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val currentStreak: StateFlow<Int> = allSessionDays.map { days ->
        StreakCalc.currentStreak(days, DateUtil.todayEpochDay())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val neglectedMuscles: StateFlow<List<Muscle>> = run {
        val range = AnalyticsWindow.Week.rangeEpochDays()
        combine(
            repository.observeSessionsWithSetsInRange(range.start, range.endInclusive),
            repository.observeExercises(),
            prefs.volumeMetric,
        ) { sessions, exercises, metric ->
            val muscleMap = exercises.associate {
                it.id to ExerciseMuscleMap(it.primaryMuscles, it.secondaryMuscles)
            }
            val windowed = sessions.map { sws ->
                SessionWindow(
                    dateEpochDay = sws.session.dateEpochDay,
                    sets = sws.sets.map {
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
            val volume = MuscleVolume.computeForWindow(windowed, range, muscleMap, metric)
            Muscle.values().filter { (volume[it] ?: 0f) <= 0f }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    val state: StateFlow<AnalyticsState> = window
        .flatMapLatest { w ->
            val range = w.rangeEpochDays()
            combine(
                repository.observeSessionsWithSetsInRange(range.start, range.endInclusive),
                repository.observeExercises(),
                prefs.volumeMetric,
            ) { sessions, exercises, metric ->
                val muscleMap = exercises.associate { ex ->
                    ex.id to ExerciseMuscleMap(ex.primaryMuscles, ex.secondaryMuscles)
                }
                val windowed = sessions.map { sws ->
                    SessionWindow(
                        dateEpochDay = sws.session.dateEpochDay,
                        sets = sws.sets.map {
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
                val volume = MuscleVolume.computeForWindow(windowed, range, muscleMap, metric)
                val normalized = MuscleVolume.normalize(volume)
                val bwCounts = MuscleVolume.bodyweightSetCountsForWindow(windowed, range, muscleMap)
                val setsByExercise = sessions
                    .flatMap { it.sets }
                    .groupBy { it.exerciseId }
                // Headline total counts each logged set once (no muscle double-counting).
                val workingSets = windowed.flatMap { it.sets }
                    .filter { it.isCompleted && !it.isWarmup }
                val totalVolume = when (metric) {
                    VolumeMetric.Sets -> workingSets.size.toFloat()
                    VolumeMetric.Tonnage -> workingSets.sumOf { set ->
                        val reps = set.repsDone
                        val weight = set.weightKg
                        if (reps != null && weight != null) (reps * weight).toDouble() else 0.0
                    }.toFloat()
                }
                AnalyticsState(
                    window = w,
                    metric = metric,
                    normalized = normalized,
                    totalVolume = totalVolume,
                    bodyweightSetCounts = bwCounts,
                    sessionsCount = sessions.size,
                    rawVolume = volume,
                    exercisesById = exercises.associateBy { it.id },
                    setsByExercisePerMuscle = setsByExercise,
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsState(),
        )

    fun setWindow(value: AnalyticsWindow) = window.update { value }

    fun setMetric(value: VolumeMetric) = viewModelScope.launch { prefs.setVolumeMetric(value) }

    suspend fun breakdownFor(muscle: Muscle): MuscleBreakdown = withContext(Dispatchers.Default) {
        val s = state.value
        val contributions = s.exercisesById.values.mapNotNull { ex ->
            val sets = s.setsByExercisePerMuscle[ex.id].orEmpty()
                .filter { it.isCompleted && it.setType != SetType.Warmup }
            if (sets.isEmpty()) return@mapNotNull null
            val primary = muscle in ex.primaryMuscles
            val secondary = muscle in ex.secondaryMuscles
            if (!primary && !secondary) return@mapNotNull null
            val tonnage = sets.sumOf { s2 ->
                val reps = s2.repsDone
                val weight = s2.weightKg
                if (reps != null && weight != null) (reps * weight).toDouble() else 0.0
            }.toFloat()
            val bwCount = sets.count { it.weightKg == null }
            ExerciseContribution(ex, setCount = sets.size, tonnage = tonnage, bodyweightSets = bwCount)
        }.sortedWith(
            when (s.metric) {
                VolumeMetric.Sets -> compareByDescending { it.setCount }
                VolumeMetric.Tonnage -> compareByDescending { it.tonnage }
            },
        )
        MuscleBreakdown(
            muscle = muscle,
            metric = s.metric,
            value = s.rawVolume[muscle] ?: 0f,
            bodyweightSets = s.bodyweightSetCounts[muscle] ?: 0,
            exercises = contributions,
        )
    }
}
