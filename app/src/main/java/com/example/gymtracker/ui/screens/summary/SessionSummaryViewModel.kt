package com.example.gymtracker.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.domain.ExerciseProgress
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.OneRepMax
import com.example.gymtracker.domain.PersonalRecords
import com.example.gymtracker.domain.PrType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Best record an exercise set this session; [delta] vs the previous best (null = first log). */
data class SummaryRecord(
    val exerciseName: String,
    val type: PrType,
    val value: Float,
    val delta: Float?,
)

data class SummaryExercise(
    val name: String,
    val movementType: MovementType,
    val doneSets: Int,
    val skippedSets: Int,
    /** Best completed working set (by est. 1RM, else reps): weight (kg, nullable) × reps. */
    val bestWeightKg: Float?,
    val bestReps: Int?,
)

data class SessionSummaryState(
    val planName: String?,
    val dateEpochDay: Long,
    val durationMillis: Long?,
    val doneSets: Int,
    val volumeKg: Double,
    val exerciseCount: Int,
    val records: List<SummaryRecord>,
    val exercises: List<SummaryExercise>,
    val notes: String,
    val unit: WeightUnit,
)

/**
 * Session summary — §4.5. Loaded once from the saved session: duration from
 * `loggedAt − startedAt` (decision 4), records recomputed against history *excluding* this
 * session (the same baseline the logger snapshotted at start), skipped sets per exercise.
 */
class SessionSummaryViewModel(
    private val repository: GymRepository,
    private val prefs: PreferencesStore,
    private val sessionId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow<SessionSummaryState?>(null)
    val state: StateFlow<SessionSummaryState?> = _state.asStateFlow()

    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    init {
        viewModelScope.launch { _state.value = load() }
    }

    private suspend fun load(): SessionSummaryState? {
        val withSets = repository.observeSessionWithSets(sessionId).first() ?: return null
        val session = withSets.session
        val unit = prefs.weightUnit.first()
        // Exercise order = insertion order of their first set.
        val byExercise = withSets.sets.sortedBy { it.id }.groupBy { it.exerciseId }
        val records = mutableListOf<SummaryRecord>()
        val exercises = byExercise.map { (exerciseId, sets) ->
            val exercise = repository.findExercise(exerciseId)
            val name = exercise?.name ?: "Exercise"
            val ordered = sets.sortedBy { it.setIndex }
            val history = repository.observeExerciseSetsWithDate(exerciseId).first()
                .filter { it.sessionId != sessionId }
                .map { LoggedSetSample(it.dateEpochDay, it.repsDone, it.weightKg, it.isCompleted, it.setType) }
            bestRecord(name, ExerciseProgress.records(history), ordered.map { it.sample() })?.let(records::add)
            val scoring = ordered.filter { it.isCompleted && it.setType != SetType.Warmup && (it.repsDone ?: 0) > 0 }
            val best = scoring.maxByOrNull { s ->
                s.weightKg?.takeIf { it > 0f }?.let { OneRepMax.epley(it, s.repsDone!!) } ?: s.repsDone!!.toFloat() / 1000f
            }
            SummaryExercise(
                name = name,
                movementType = exercise?.movementType ?: MovementType.Compound,
                doneSets = ordered.count { it.isCompleted },
                skippedSets = ordered.count { !it.isCompleted },
                bestWeightKg = best?.weightKg?.takeIf { it > 0f },
                bestReps = best?.repsDone,
            )
        }
        val done = withSets.sets.filter { it.isCompleted }
        return SessionSummaryState(
            planName = session.planId?.let { repository.findPlan(it)?.name },
            dateEpochDay = session.dateEpochDay,
            durationMillis = session.startedAtMillis?.let { session.loggedAtMillis - it }?.takeIf { it >= 0 },
            doneSets = done.size,
            volumeKg = done.filter { it.setType != SetType.Warmup }
                .sumOf { ((it.weightKg ?: 0f) * (it.repsDone ?: 0)).toDouble() },
            exerciseCount = exercises.count { it.doneSets > 0 },
            records = records,
            exercises = exercises,
            notes = session.notes,
            unit = unit,
        )
    }

    private fun LoggedSetEntity.sample() = LoggedSetSample(0L, repsDone, weightKg, isCompleted, setType)

    /** The highest-priority record this exercise broke ([PrType] order), with its delta. */
    private fun bestRecord(name: String, baseline: PersonalRecords, sets: List<LoggedSetSample>): SummaryRecord? {
        val won = ExerciseProgress.sessionPrs(baseline, sets)
        if (won.isEmpty()) return null
        val type = won.values.flatten().minByOrNull { it.ordinal } ?: return null
        val session = ExerciseProgress.records(sets)
        val (value, previous) = when (type) {
            PrType.OneRepMax -> session.estimatedOneRepMaxKg to baseline.estimatedOneRepMaxKg
            PrType.Weight -> session.heaviestWeightKg to baseline.heaviestWeightKg
            PrType.Volume -> session.bestSetVolumeKg to baseline.bestSetVolumeKg
            PrType.Reps -> session.maxReps?.toFloat() to baseline.maxReps?.toFloat()
        }
        return SummaryRecord(name, type, value ?: return null, previous?.let { value - it })
    }

    /** Done / ✕ / back: write the note (if changed) to the session, then close. */
    fun saveAndClose(note: String) {
        if (_closed.value) return
        viewModelScope.launch {
            val trimmed = note.trim()
            if (trimmed != _state.value?.notes?.trim()) {
                withContext(NonCancellable) { repository.updateSessionNotes(sessionId, trimmed) }
            }
            _closed.update { true }
        }
    }
}
