package com.example.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.ExerciseSetWithDate
import com.example.gymtracker.data.db.SessionWithSets
import com.example.gymtracker.data.formatTarget
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.Records
import com.example.gymtracker.domain.toSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One row of a Session-detail set table. */
data class DetailSet(
    /** `W`, `F`, or the working-set number (warm-ups don't take a number). */
    val badge: String,
    val setType: SetType,
    val weightKg: Float?,
    val reps: Int?,
    /** False = left unticked at Finish, saved as skipped. */
    val completed: Boolean,
    /** Broke a record against history *before* this session. */
    val isPr: Boolean,
)

data class DetailExercise(
    val exerciseId: Long,
    val name: String,
    val movementType: MovementType,
    /** The plan's target for this exercise, e.g. `3 × 8–12 reps`; null for ad hoc. */
    val target: String?,
    val doneSets: Int,
    val sets: List<DetailSet>,
)

data class SessionDetailState(
    val planName: String?,
    val dateEpochDay: Long,
    val durationMillis: Long?,
    val doneSets: Int,
    val volumeKg: Double,
    /** Exercises with at least one PR set (same unit as the Session summary). */
    val prCount: Int,
    val exerciseCount: Int,
    val notes: String,
    val exercises: List<DetailExercise>,
    val unit: WeightUnit,
)

/**
 * Session detail. Read-only apart from the note and Delete. PR flags are
 * recomputed from the exercise's history *before* this session, so a later session can't steal
 * the badge. State starts `null`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModel(
    private val repository: GymRepository,
    prefs: PreferencesStore,
    private val sessionId: Long,
) : ViewModel() {

    val state: StateFlow<SessionDetailState?> = combine(
        // filterNotNull: after Delete the row vanishes; keep the last frame while the page pops.
        repository.observeSessionWithSets(sessionId).filterNotNull(),
        prefs.weightUnit,
    ) { withSets, unit -> withSets to unit }
        .mapLatest { (withSets, unit) -> load(withSets, unit) }
        // load() replays each exercise's whole history for PR flags: keep that CPU off Main.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private suspend fun load(withSets: SessionWithSets, unit: WeightUnit): SessionDetailState {
        val session = withSets.session
        val plan = session.planId?.let { repository.observePlanWithExercises(it).first() }
        val targets = plan?.planned.orEmpty().associate { pw ->
            pw.planned.exerciseId to formatTarget(
                pw.planned.targetSets, pw.planned.targetRepsLow, pw.planned.targetRepsHigh,
                pw.exercise.movementType,
            )
        }
        // Exercise order = insertion order of each exercise's first set (as logged).
        val perExercise = withSets.sets.sortedBy { it.id }.groupBy { it.exerciseId }
            .map { (exerciseId, sets) ->
                val exercise = repository.findExercise(exerciseId)
                val ordered = sets.sortedBy { it.setIndex }
                val before = repository.observeExerciseSetsWithDate(exerciseId).first().beforeSession(sessionId)
                val records = Records.forSession(before, ordered.map { it.toSample() })
                var working = 0
                records to DetailExercise(
                    exerciseId = exerciseId,
                    name = exercise?.name ?: "Exercise",
                    movementType = exercise?.movementType ?: MovementType.Compound,
                    target = targets[exerciseId],
                    doneSets = ordered.count { it.isCompleted },
                    sets = ordered.mapIndexed { i, set ->
                        DetailSet(
                            badge = when (set.setType) {
                                SetType.Warmup -> "W"
                                SetType.Failure -> { working++; "F" }
                                SetType.Working -> "${++working}"
                            },
                            setType = set.setType,
                            weightKg = set.weightKg,
                            reps = set.repsDone,
                            completed = set.isCompleted,
                            isPr = i in records.prSets,
                        )
                    },
                )
            }
        val exercises = perExercise.map { it.second }
        val done = withSets.sets.filter { it.isCompleted }
        return SessionDetailState(
            planName = plan?.plan?.name,
            dateEpochDay = session.dateEpochDay,
            durationMillis = session.durationMillis(),
            doneSets = done.size,
            volumeKg = done.filter { it.setType != SetType.Warmup }
                .sumOf { ((it.weightKg ?: 0f) * (it.repsDone ?: 0)).toDouble() },
            // Exercises with at least one record, matching the Session summary count.
            prCount = Records.prCount(perExercise.map { it.first }),
            exerciseCount = exercises.count { it.doneSets > 0 },
            notes = session.notes,
            exercises = exercises,
            unit = unit,
        )
    }

    fun saveNote(note: String) {
        val trimmed = note.trim()
        if (trimmed == state.value?.notes?.trim()) return
        viewModelScope.launch {
            withContext(NonCancellable) { repository.updateSessionNotes(sessionId, trimmed) }
        }
    }

    fun delete() {
        if (_deleted.value) return
        viewModelScope.launch {
            withContext(NonCancellable) { repository.deleteSession(sessionId) }
            _deleted.value = true
        }
    }
}

/**
 * The exercise's history strictly before [sessionId]. The query orders by date then log time, so
 * everything up to this session's first set is earlier; a session not found yields the whole list.
 */
internal fun List<ExerciseSetWithDate>.beforeSession(sessionId: Long): List<LoggedSetSample> =
    takeWhile { it.sessionId != sessionId }.map { it.toSample() }
