package com.example.gymtracker.ui.screens.logger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.ExerciseEntity
import com.example.gymtracker.data.db.LoggedSetEntity
import com.example.gymtracker.data.db.PlannedExerciseEntity
import com.example.gymtracker.data.db.SessionDraftEntity
import com.example.gymtracker.data.db.SessionDraftGroup
import com.example.gymtracker.data.db.SessionDraftPayload
import com.example.gymtracker.data.db.SessionDraftSet
import com.example.gymtracker.data.db.SessionEntity
import com.example.gymtracker.data.displayToKg
import com.example.gymtracker.domain.ExerciseProgress
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.PersonalRecords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class SetDraft(
    val repsInput: String = "",
    val weightInput: String = "",
    val hintReps: Int? = null,
    val hintWeightKg: Float? = null,
    val isCompleted: Boolean = false,
    val setType: SetType = SetType.Working,
)

data class ExerciseGroup(
    val uid: String,
    val exerciseId: Long,
    val exerciseName: String,
    val movementType: MovementType,
    val targetSets: Int,
    val targetRepsLow: Int,
    val targetRepsHigh: Int,
    val sets: List<SetDraft>,
)

data class SessionLoggerState(
    val planId: Long? = null,
    val planName: String? = null,
    val dateEpochDay: Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
    val startedAtMillis: Long = 0L,
    /** Wall-clock of the last set marked completed. Drives the rest timer (time since last set).
     *  Persisted in the draft so the rest clock keeps running while the logger is closed. */
    val lastSetCompletedAtMillis: Long = 0L,
    val groups: List<ExerciseGroup> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val discarded: Boolean = false,
    val initialized: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    /** True when Save was tapped on a plan-backed session whose structure now differs from the
     *  parent plan — the UI asks whether to push the changes back to the plan. */
    val askPlanSync: Boolean = false,
    /** True when Finish was tapped and there's no plan-sync prompt to show — a plain confirmation
     *  so finishing always takes a deliberate second tap (guards against accidental Finish). */
    val askConfirmFinish: Boolean = false,
    /** Pre-session all-time records per exercise — the bar a set must clear to earn a PR crown. */
    val baselineRecords: Map<Long, PersonalRecords> = emptyMap(),
)

@OptIn(FlowPreview::class)
class SessionLoggerViewModel(
    private val repository: GymRepository,
    private val prefs: PreferencesStore,
    private val applicationScope: CoroutineScope,
    private val planId: Long?,
) : ViewModel() {

    private val _draft = MutableStateFlow(SessionLoggerState())
    val state: StateFlow<SessionLoggerState> = _draft.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            val unit = prefs.weightUnit.first()
            val existing = repository.findSessionDraft()
            if (existing != null) {
                hydrateFromDraft(existing, unit)
            } else {
                _draft.update { it.copy(weightUnit = unit, planId = planId) }
                if (planId != null) loadFromPlan(planId)
                _draft.update { it.copy(initialized = true, startedAtMillis = System.currentTimeMillis()) }
                // Persist the initial draft immediately so resume works even with zero edits
                // or a fast back-press before debounce fires.
                persistDraft(_draft.value)
            }
            loadBaselines(_draft.value.groups.map { it.exerciseId })
            // Persist subsequent edits, debouncing bursts.
            _draft
                .drop(1)
                .debounce(400L)
                .collect { snapshot ->
                    if (snapshot.initialized && !snapshot.saved && !snapshot.discarded) {
                        persistDraft(snapshot)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Flush the latest draft on an app-scoped coroutine so a fast back-press
        // (which cancels viewModelScope) doesn't drop the last typed edit.
        val snapshot = _draft.value
        if (snapshot.initialized && !snapshot.saved && !snapshot.discarded) {
            applicationScope.launch {
                withContext(NonCancellable) { persistDraft(snapshot) }
            }
        }
    }

    private fun hydrateFromDraft(draft: SessionDraftEntity, unit: WeightUnit) {
        val payload = runCatching {
            json.decodeFromString(SessionDraftPayload.serializer(), draft.payloadJson)
        }.getOrNull() ?: return
        val groups = payload.groups.map { g ->
            ExerciseGroup(
                uid = g.uid.ifBlank { UUID.randomUUID().toString() },
                exerciseId = g.exerciseId,
                exerciseName = g.exerciseName,
                movementType = runCatching { MovementType.valueOf(g.movementType) }.getOrDefault(MovementType.Compound),
                targetSets = g.sets.size,
                targetRepsLow = g.targetRepsLow,
                targetRepsHigh = g.targetRepsHigh,
                sets = g.sets.map { s ->
                    SetDraft(
                        repsInput = s.repsInput,
                        weightInput = s.weightInput,
                        hintReps = s.hintReps,
                        hintWeightKg = s.hintWeightKg,
                        isCompleted = s.isCompleted,
                        setType = runCatching { SetType.valueOf(s.setType) }.getOrDefault(SetType.Working),
                    )
                },
            )
        }
        _draft.update {
            it.copy(
                planId = draft.planId,
                planName = payload.planName,
                dateEpochDay = payload.dateEpochDay,
                startedAtMillis = draft.startedAtMillis,
                lastSetCompletedAtMillis = payload.lastSetCompletedAtMillis,
                groups = groups,
                weightUnit = unit,
                initialized = true,
            )
        }
    }

    private suspend fun persistDraft(snapshot: SessionLoggerState) {
        val payload = SessionDraftPayload(
            planName = snapshot.planName,
            dateEpochDay = snapshot.dateEpochDay,
            lastSetCompletedAtMillis = snapshot.lastSetCompletedAtMillis,
            groups = snapshot.groups.map { g ->
                SessionDraftGroup(
                    exerciseId = g.exerciseId,
                    exerciseName = g.exerciseName,
                    movementType = g.movementType.name,
                    targetRepsLow = g.targetRepsLow,
                    targetRepsHigh = g.targetRepsHigh,
                    uid = g.uid,
                    sets = g.sets.map { s ->
                        SessionDraftSet(
                            repsInput = s.repsInput,
                            weightInput = s.weightInput,
                            hintReps = s.hintReps,
                            hintWeightKg = s.hintWeightKg,
                            isCompleted = s.isCompleted,
                            setType = s.setType.name,
                        )
                    },
                )
            },
        )
        repository.upsertSessionDraft(
            SessionDraftEntity(
                id = 1L,
                planId = snapshot.planId,
                startedAtMillis = snapshot.startedAtMillis,
                updatedAtMillis = System.currentTimeMillis(),
                payloadJson = json.encodeToString(SessionDraftPayload.serializer(), payload),
            )
        )
    }

    private suspend fun loadFromPlan(planId: Long) {
        val plan = repository.observePlanWithExercises(planId).first() ?: return
        val groups = plan.planned
            .sortedBy { it.planned.sortIndex }
            .map { p ->
                val hints = repository.lastLoggedSetsFor(p.planned.exerciseId)
                val midRepHint = (p.planned.targetRepsLow + p.planned.targetRepsHigh) / 2
                val sets = (0 until p.planned.targetSets).map { idx ->
                    val hint = hints[idx]
                    SetDraft(
                        hintReps = hint?.repsDone ?: midRepHint,
                        hintWeightKg = hint?.weightKg,
                    )
                }
                ExerciseGroup(
                    uid = UUID.randomUUID().toString(),
                    exerciseId = p.exercise.id,
                    exerciseName = p.exercise.name,
                    movementType = p.exercise.movementType,
                    targetSets = p.planned.targetSets,
                    targetRepsLow = p.planned.targetRepsLow,
                    targetRepsHigh = p.planned.targetRepsHigh,
                    sets = sets,
                )
            }
        _draft.update { it.copy(planName = plan.plan.name, groups = groups) }
    }

    fun setDate(epochDay: Long) {
        _draft.update { it.copy(dateEpochDay = epochDay) }
    }

    fun addExerciseFreestyle(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val hints = repository.lastLoggedSetsFor(exercise.id)
            val defaults = defaultsFor(exercise.movementType)
            val sets = (0 until defaults.sets).map { idx ->
                val hint = hints[idx]
                SetDraft(
                    hintReps = hint?.repsDone ?: defaults.hint,
                    hintWeightKg = hint?.weightKg,
                )
            }
            _draft.update {
                it.copy(
                    groups = it.groups + ExerciseGroup(
                        uid = UUID.randomUUID().toString(),
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        movementType = exercise.movementType,
                        targetSets = defaults.sets,
                        targetRepsLow = defaults.low,
                        targetRepsHigh = defaults.high,
                        sets = sets,
                    )
                )
            }
            loadBaselines(listOf(exercise.id))
        }
    }

    /** Snapshot each exercise's all-time records once, to compare live sets against for PR crowns. */
    private suspend fun loadBaselines(exerciseIds: List<Long>) {
        val toLoad = exerciseIds.distinct().filter { it !in _draft.value.baselineRecords }
        if (toLoad.isEmpty()) return
        val loaded = toLoad.associateWith { id ->
            val history = repository.observeExerciseSetsWithDate(id).first()
            ExerciseProgress.records(
                history.map {
                    LoggedSetSample(it.dateEpochDay, it.repsDone, it.weightKg, it.isCompleted, it.setType)
                }
            )
        }
        _draft.update { it.copy(baselineRecords = it.baselineRecords + loaded) }
    }

    private data class TypeDefaults(val sets: Int, val low: Int, val high: Int, val hint: Int)
    private fun defaultsFor(type: MovementType): TypeDefaults = when (type) {
        MovementType.Cardio -> TypeDefaults(sets = 1, low = 20, high = 30, hint = 25)
        MovementType.Mobility -> TypeDefaults(sets = 2, low = 30, high = 60, hint = 45)
        else -> TypeDefaults(sets = 3, low = 8, high = 12, hint = 10)
    }

    fun removeGroup(index: Int) {
        _draft.update {
            it.copy(groups = it.groups.toMutableList().apply { if (index in indices) removeAt(index) })
        }
    }

    /** Move an exercise group up (delta = -1) or down (delta = +1). */
    fun moveGroup(index: Int, delta: Int) {
        _draft.update { s ->
            val target = index + delta
            if (index !in s.groups.indices || target !in s.groups.indices) return@update s
            val list = s.groups.toMutableList()
            val item = list.removeAt(index)
            list.add(target, item)
            s.copy(groups = list)
        }
    }

    /** Start or restart the rest timer manually — both to kick it off before the first set is
     *  logged and to reset it for sets finished without ticking the checkbox. */
    fun resetRestTimer() {
        _draft.update { it.copy(lastSetCompletedAtMillis = System.currentTimeMillis()) }
    }

    fun addSet(groupIndex: Int) {
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            val newSet = SetDraft(hintReps = group.sets.lastOrNull()?.hintReps, hintWeightKg = group.sets.lastOrNull()?.hintWeightKg)
            val newGroups = s.groups.toMutableList().also {
                it[groupIndex] = group.copy(sets = group.sets + newSet)
            }
            s.copy(groups = newGroups)
        }
    }

    /** Edit an exercise's target rep range mid-session. Kept valid: low ≥ 1 and high ≥ low. */
    fun setRepTarget(groupIndex: Int, low: Int, high: Int) {
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            val lo = low.coerceAtLeast(1)
            val hi = high.coerceAtLeast(lo)
            val newGroups = s.groups.toMutableList().also {
                it[groupIndex] = group.copy(targetRepsLow = lo, targetRepsHigh = hi)
            }
            s.copy(groups = newGroups)
        }
    }

    fun removeSet(groupIndex: Int, setIndex: Int) {
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            if (setIndex !in group.sets.indices) return@update s
            val newGroup = group.copy(sets = group.sets.toMutableList().apply { removeAt(setIndex) })
            val newGroups = s.groups.toMutableList().also { it[groupIndex] = newGroup }
            s.copy(groups = newGroups)
        }
    }

    fun updateSet(groupIndex: Int, setIndex: Int, update: (SetDraft) -> SetDraft) {
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            val current = group.sets.getOrNull(setIndex) ?: return@update s
            val updated = update(current)
            val newSets = group.sets.toMutableList().also { it[setIndex] = updated }
            val newGroups = s.groups.toMutableList().also { it[groupIndex] = group.copy(sets = newSets) }
            // A set transitioning into "completed" is the moment it's logged — reset the rest timer.
            val justCompleted = !current.isCompleted && updated.isCompleted
            s.copy(
                groups = newGroups,
                lastSetCompletedAtMillis =
                    if (justCompleted) System.currentTimeMillis() else s.lastSetCompletedAtMillis,
            )
        }
    }

    fun discard() {
        viewModelScope.launch {
            repository.clearSessionDraft()
            // Flip discarded last (this triggers nav back, which destroys the VM).
            _draft.update { it.copy(discarded = true) }
        }
    }

    /** Entry point for the Save/Done button. If this is a plan-backed session whose structure
     *  changed mid-workout, raise [SessionLoggerState.askPlanSync] so the UI can ask whether to
     *  push those edits back to the plan; otherwise just commit. */
    fun attemptSave() {
        val s = _draft.value
        if (s.saving || s.groups.isEmpty()) return
        viewModelScope.launch {
            if (s.planId != null && planStructureChanged(s)) {
                _draft.update { it.copy(askPlanSync = true) }
            } else {
                // No plan-sync prompt to gate the tap — show a plain confirmation instead so
                // Finish is never a single accidental click.
                _draft.update { it.copy(askConfirmFinish = true) }
            }
        }
    }

    fun dismissPlanSync() = _draft.update { it.copy(askPlanSync = false) }

    fun dismissConfirmFinish() = _draft.update { it.copy(askConfirmFinish = false) }

    /** Resolve the plain finish confirmation: commit the session as-is. */
    fun confirmFinish() {
        _draft.update { it.copy(askConfirmFinish = false) }
        commitSave(updatePlan = false)
    }

    /** Resolve the plan-sync prompt: save the session, optionally rewriting the parent plan. */
    fun confirmSave(updatePlan: Boolean) {
        _draft.update { it.copy(askPlanSync = false) }
        commitSave(updatePlan)
    }

    /** Compare the live session structure against the parent plan: exercise order, per-exercise set
     *  count, and target rep range (all editable mid-session). Any difference offers a plan sync. */
    private suspend fun planStructureChanged(s: SessionLoggerState): Boolean {
        val planId = s.planId ?: return false
        val plan = repository.observePlanWithExercises(planId).first() ?: return false
        val baseline = plan.planned.sortedBy { it.planned.sortIndex }
            .map { Quad(it.planned.exerciseId, it.planned.targetSets, it.planned.targetRepsLow, it.planned.targetRepsHigh) }
        val current = s.groups.map { Quad(it.exerciseId, it.sets.size, it.targetRepsLow, it.targetRepsHigh) }
        return baseline != current
    }

    private data class Quad(val exerciseId: Long, val sets: Int, val repsLow: Int, val repsHigh: Int)

    private fun commitSave(updatePlan: Boolean) {
        if (_draft.value.saving) return
        _draft.update { it.copy(saving = true) }
        viewModelScope.launch {
            val s = _draft.value
            val unit = s.weightUnit
            if (updatePlan && s.planId != null) {
                val planned = s.groups.mapIndexed { i, g ->
                    PlannedExerciseEntity(
                        planId = s.planId,
                        exerciseId = g.exerciseId,
                        sortIndex = i,
                        targetSets = g.sets.size,
                        targetRepsLow = g.targetRepsLow,
                        targetRepsHigh = g.targetRepsHigh,
                    )
                }
                repository.replacePlanned(s.planId, planned)
            }
            val session = SessionEntity(
                clientUuid = UUID.randomUUID().toString(),
                planId = s.planId,
                dateEpochDay = s.dateEpochDay,
                loggedAtMillis = System.currentTimeMillis(),
            )
            repository.saveSession(session) { sessionId ->
                s.groups.flatMap { group ->
                    group.sets.mapIndexed { idx, draft ->
                        val resolvedReps = draft.repsInput.toIntOrNull() ?: draft.hintReps
                        // Weight is optional — an empty field with no history means a bodyweight
                        // movement (e.g. hanging leg raises) and stays null. The checkbox alone
                        // determines whether the set counts.
                        val resolvedWeightKg = draft.weightInput.toFloatOrNull()?.displayToKg(unit) ?: draft.hintWeightKg
                        val completed = draft.isCompleted
                        LoggedSetEntity(
                            sessionId = sessionId,
                            exerciseId = group.exerciseId,
                            setIndex = idx,
                            repsDone = resolvedReps,
                            weightKg = resolvedWeightKg,
                            isCompleted = completed,
                            setType = draft.setType,
                        )
                    }
                }
            }
            repository.clearSessionDraft()
            // Order matters: flip saved last (this destroys the VM via nav).
            _draft.update { it.copy(saving = false, saved = true) }
        }
    }
}
