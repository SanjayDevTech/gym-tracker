package com.example.gymtracker.ui.screens.logger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.DEFAULT_REST_SECONDS
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
import com.example.gymtracker.data.kgToDisplay
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.domain.ExerciseProgress
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.PersonalRecords
import com.example.gymtracker.ui.format.trimDecimal
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
) {
    val doneCount: Int get() = sets.count { it.isCompleted }
    val allDone: Boolean get() = sets.isNotEmpty() && sets.all { it.isCompleted }
}

/** The Finish sheet is open. [planChanges] lists how this session differs from its plan
 *  ("Bench press 3→4 sets", "added Dips"); empty when there is no plan or nothing changed,
 *  in which case the sheet shows no plan-sync checkbox. */
data class FinishPrompt(val planChanges: List<String>)

data class SessionLoggerState(
    val planId: Long? = null,
    /** The draft's plan name — the logger's title always comes from here, never from the route. */
    val planName: String? = null,
    val dateEpochDay: Long = DateUtil.todayEpochDay(),
    val startedAtMillis: Long = 0L,
    /** Wall-clock of the last set marked completed; 0 = rest cleared (Skip, or no set yet).
     *  Persisted in the draft so the rest clock keeps running while the logger is closed. */
    val lastSetCompletedAtMillis: Long = 0L,
    /** This workout's rest target (§6.3). */
    val restTargetSeconds: Int = DEFAULT_REST_SECONDS,
    val groups: List<ExerciseGroup> = emptyList(),
    val saving: Boolean = false,
    /** Set once the session is committed; the UI navigates to its summary. */
    val savedSessionId: Long? = null,
    val discarded: Boolean = false,
    val initialized: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val finish: FinishPrompt? = null,
    /** Pre-session all-time records per exercise — the bar a set must clear to earn a PR. */
    val baselineRecords: Map<Long, PersonalRecords> = emptyMap(),
    /** One-shot: a group just added from the picker that the list should scroll to and focus. */
    val revealGroupUid: String? = null,
) {
    val totalSets: Int get() = groups.sumOf { it.sets.size }
    val doneSets: Int get() = groups.sumOf { it.doneCount }
    val skippedSets: Int get() = totalSets - doneSets
}

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

    /** Single-level undo buffer for the last removed exercise or set (§6.6). */
    private sealed interface Removed {
        data class Group(val index: Int, val group: ExerciseGroup) : Removed
        data class Set(val groupUid: String, val index: Int, val set: SetDraft) : Removed
    }
    private var lastRemoved: Removed? = null

    init {
        viewModelScope.launch {
            val unit = prefs.weightUnit.first()
            val existing = repository.findSessionDraft()
            if (existing != null) {
                hydrateFromDraft(existing, unit)
            } else {
                val rest = prefs.defaultRestSeconds.first()
                _draft.update { it.copy(weightUnit = unit, planId = planId, restTargetSeconds = rest) }
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
                .collect { snapshot -> if (snapshot.shouldPersist) persistDraft(snapshot) }
        }
    }

    private val SessionLoggerState.shouldPersist: Boolean
        get() = initialized && savedSessionId == null && !saving && !discarded

    override fun onCleared() {
        super.onCleared()
        // Flush the latest draft on an app-scoped coroutine so a fast back-press
        // (which cancels viewModelScope) doesn't drop the last typed edit.
        val snapshot = _draft.value
        if (snapshot.shouldPersist) {
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
                restTargetSeconds = payload.restTargetSeconds,
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
            restTargetSeconds = snapshot.restTargetSeconds,
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

    // ---- Date -------------------------------------------------------------

    /** Apply a date from the Material date picker (UTC-midnight millis). Future days — judged
     *  against *local* today, not UTC — are refused (issue 15). */
    fun setDateFromPicker(utcMillis: Long) {
        val epochDay = DateUtil.pickerMillisToEpochDay(utcMillis)
        if (epochDay > DateUtil.todayEpochDay()) return
        _draft.update { it.copy(dateEpochDay = epochDay) }
    }

    // ---- Exercises --------------------------------------------------------

    /** Append the picker's selection, in pick order, with type defaults and history hints. */
    fun addExercises(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val added = ids.mapNotNull { id -> repository.findExercise(id)?.let { newGroup(it) } }
            if (added.isEmpty()) return@launch
            _draft.update { it.copy(groups = it.groups + added, revealGroupUid = added.first().uid) }
            loadBaselines(added.map { it.exerciseId })
        }
    }

    fun consumeReveal() = _draft.update { it.copy(revealGroupUid = null) }

    private suspend fun newGroup(exercise: ExerciseEntity): ExerciseGroup {
        val hints = repository.lastLoggedSetsFor(exercise.id)
        val defaults = defaultsFor(exercise.movementType)
        val sets = (0 until defaults.sets).map { idx ->
            val hint = hints[idx]
            SetDraft(hintReps = hint?.repsDone ?: defaults.hint, hintWeightKg = hint?.weightKg)
        }
        return ExerciseGroup(
            uid = UUID.randomUUID().toString(),
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            movementType = exercise.movementType,
            targetSets = defaults.sets,
            targetRepsLow = defaults.low,
            targetRepsHigh = defaults.high,
            sets = sets,
        )
    }

    /** Snapshot each exercise's all-time records once, to compare live sets against for PRs. */
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

    /** Remove an exercise; [undoRemove] puts it back where it was. Returns what was removed. */
    fun removeGroup(index: Int): ExerciseGroup? {
        val group = _draft.value.groups.getOrNull(index) ?: return null
        lastRemoved = Removed.Group(index, group)
        _draft.update { s -> s.copy(groups = s.groups.filterNot { it.uid == group.uid }) }
        return group
    }

    /** Commit a reorder made in reorder mode (one write per drag, on release). */
    fun reorderGroups(orderedUids: List<String>) {
        _draft.update { s ->
            val byUid = s.groups.associateBy { it.uid }
            val reordered = orderedUids.mapNotNull { byUid[it] }
            // Anything not in the list (added meanwhile) keeps its place at the end.
            s.copy(groups = reordered + s.groups.filter { it.uid !in orderedUids })
        }
    }

    // ---- Sets -------------------------------------------------------------

    fun addSet(groupIndex: Int) {
        updateGroup(groupIndex) { group ->
            val last = group.sets.lastOrNull()
            group.copy(sets = group.sets + SetDraft(hintReps = last?.hintReps, hintWeightKg = last?.hintWeightKg))
        }
    }

    /** Remove one set (the set menu's "Delete set"); the last remaining set can't be removed. */
    fun removeSet(groupIndex: Int, setIndex: Int) {
        val group = _draft.value.groups.getOrNull(groupIndex) ?: return
        if (group.sets.size <= 1 || setIndex !in group.sets.indices) return
        lastRemoved = Removed.Set(group.uid, setIndex, group.sets[setIndex])
        updateGroup(groupIndex) { g -> g.copy(sets = g.sets.filterIndexed { i, _ -> i != setIndex }) }
    }

    fun undoRemove() {
        val removed = lastRemoved ?: return
        lastRemoved = null
        _draft.update { s ->
            when (removed) {
                is Removed.Group -> {
                    if (s.groups.any { it.uid == removed.group.uid }) return@update s
                    val list = s.groups.toMutableList()
                    list.add(removed.index.coerceIn(0, list.size), removed.group)
                    s.copy(groups = list)
                }
                is Removed.Set -> s.copy(
                    groups = s.groups.map { g ->
                        if (g.uid != removed.groupUid) g
                        else g.copy(
                            sets = g.sets.toMutableList().apply {
                                add(removed.index.coerceIn(0, size), removed.set)
                            }
                        )
                    }
                )
            }
        }
    }

    /** Edit an exercise's target range mid-session. Kept valid: low ≥ 1 and high ≥ low. */
    fun setRepTarget(groupIndex: Int, low: Int, high: Int) {
        val lo = low.coerceAtLeast(1)
        val hi = high.coerceAtLeast(lo)
        updateGroup(groupIndex) { it.copy(targetRepsLow = lo, targetRepsHigh = hi) }
    }

    fun updateSet(groupIndex: Int, setIndex: Int, update: (SetDraft) -> SetDraft) {
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            val current = group.sets.getOrNull(setIndex) ?: return@update s
            val updated = update(current)
            val newSets = group.sets.toMutableList().also { it[setIndex] = updated }
            val newGroups = s.groups.toMutableList().also { it[groupIndex] = group.copy(sets = newSets) }
            // A set transitioning into "completed" is the moment it's logged — restart rest.
            val justCompleted = !current.isCompleted && updated.isCompleted
            s.copy(
                groups = newGroups,
                lastSetCompletedAtMillis =
                    if (justCompleted) System.currentTimeMillis() else s.lastSetCompletedAtMillis,
            )
        }
    }

    /** The row's check. Ticking an empty row commits its hint values so they are shown (§6.2). */
    fun setCompleted(groupIndex: Int, setIndex: Int, completed: Boolean) {
        val unit = _draft.value.weightUnit
        updateSet(groupIndex, setIndex) { set ->
            if (!completed) set.copy(isCompleted = false)
            else set.copy(
                isCompleted = true,
                repsInput = set.repsInput.ifEmpty { set.hintReps?.toString().orEmpty() },
                weightInput = set.weightInput.ifEmpty {
                    set.hintWeightKg?.let { trimDecimal(it.kgToDisplay(unit)) }.orEmpty()
                },
            )
        }
    }

    private fun updateGroup(groupIndex: Int, transform: (ExerciseGroup) -> ExerciseGroup) {
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            s.copy(groups = s.groups.toMutableList().also { it[groupIndex] = transform(group) })
        }
    }

    // ---- Rest -------------------------------------------------------------

    fun adjustRestTarget(deltaSeconds: Int) =
        setRestTarget(_draft.value.restTargetSeconds + deltaSeconds)

    fun setRestTarget(seconds: Int) {
        _draft.update { it.copy(restTargetSeconds = seconds.coerceIn(REST_MIN_SECONDS, REST_MAX_SECONDS)) }
    }

    /** Clear rest until the next completed set (dock shows "—"). */
    fun skipRest() = _draft.update { it.copy(lastSetCompletedAtMillis = 0L) }

    /** Rest sheet's "Restart now". */
    fun restartRest() = _draft.update { it.copy(lastSetCompletedAtMillis = System.currentTimeMillis()) }

    fun saveRestTargetAsDefault() {
        val seconds = _draft.value.restTargetSeconds
        viewModelScope.launch { prefs.setDefaultRestSeconds(seconds) }
    }

    // ---- Discard / finish -------------------------------------------------

    fun discard() {
        viewModelScope.launch {
            repository.clearSessionDraft()
            // Flip discarded last (this triggers nav back, which destroys the VM).
            _draft.update { it.copy(discarded = true) }
        }
    }

    /** Open the one Finish sheet: stats, skipped count and — for a plan session whose structure
     *  changed — the plan-sync checkbox with its diff. */
    fun openFinish() {
        val s = _draft.value
        if (s.saving || s.groups.isEmpty()) return
        viewModelScope.launch {
            _draft.update { it.copy(finish = FinishPrompt(planChanges(it))) }
        }
    }

    fun dismissFinish() = _draft.update { it.copy(finish = null) }

    fun confirmFinish(updatePlan: Boolean) {
        val prompt = _draft.value.finish ?: return
        commitSave(updatePlan = updatePlan && prompt.planChanges.isNotEmpty())
    }

    /** How the live session differs from its plan: per-exercise set counts and targets,
     *  additions, removals, and (if nothing else changed) order. Empty when unchanged. */
    private suspend fun planChanges(s: SessionLoggerState): List<String> {
        val planId = s.planId ?: return emptyList()
        val plan = repository.observePlanWithExercises(planId).first() ?: return emptyList()
        val baseline = plan.planned.sortedBy { it.planned.sortIndex }
        val remaining = baseline.toMutableList()
        val changes = mutableListOf<String>()
        s.groups.forEach { g ->
            val match = remaining.firstOrNull { it.planned.exerciseId == g.exerciseId }
            if (match == null) {
                changes += "added ${g.exerciseName}"
            } else {
                remaining.remove(match)
                val p = match.planned
                if (p.targetSets != g.sets.size) {
                    changes += "${g.exerciseName} ${p.targetSets}→${g.sets.size} sets"
                }
                if (p.targetRepsLow != g.targetRepsLow || p.targetRepsHigh != g.targetRepsHigh) {
                    changes += "${g.exerciseName} target ${rangeLabel(g.targetRepsLow, g.targetRepsHigh)}"
                }
            }
        }
        remaining.forEach { changes += "removed ${it.exercise.name}" }
        if (changes.isEmpty() && baseline.map { it.planned.exerciseId } != s.groups.map { it.exerciseId }) {
            changes += "reordered"
        }
        return changes
    }

    private fun rangeLabel(low: Int, high: Int) = if (low == high) "$low" else "$low–$high"

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
                startedAtMillis = s.startedAtMillis.takeIf { it > 0L },
            )
            val sessionId = repository.saveSession(session) { id ->
                s.groups.flatMap { group ->
                    group.sets.mapIndexed { idx, draft ->
                        // Unticked sets are saved as skipped (decision 8): is_completed = false,
                        // carrying the values the row showed so History can render them.
                        LoggedSetEntity(
                            sessionId = id,
                            exerciseId = group.exerciseId,
                            setIndex = idx,
                            repsDone = draft.repsInput.toIntOrNull() ?: draft.hintReps,
                            // Weight is optional — empty with no history means bodyweight.
                            weightKg = draft.weightInput.toFloatOrNull()?.displayToKg(unit) ?: draft.hintWeightKg,
                            isCompleted = draft.isCompleted,
                            setType = draft.setType,
                        )
                    }
                }
            }
            repository.clearSessionDraft()
            // Order matters: publishing the id navigates away (and destroys the VM).
            _draft.update { it.copy(saving = false, finish = null, savedSessionId = sessionId) }
        }
    }

    companion object {
        const val REST_STEP_SECONDS = 15
        const val REST_MIN_SECONDS = 15
        const val REST_MAX_SECONDS = 600
    }
}

/**
 * Resolve each draft set (typed value, else its history hint) the same way Save does, then ask
 * the domain which sets beat their exercise's pre-session records. Returns "groupUid#setIndex".
 */
fun sessionPrKeys(
    groups: List<ExerciseGroup>,
    baselines: Map<Long, PersonalRecords>,
    unit: WeightUnit,
): Set<String> {
    val keys = HashSet<String>()
    groups.forEach { group ->
        val baseline = baselines[group.exerciseId] ?: return@forEach
        val samples = group.sets.map { s ->
            LoggedSetSample(
                dateEpochDay = 0L,
                repsDone = s.repsInput.toIntOrNull() ?: s.hintReps,
                weightKg = s.weightInput.toFloatOrNull()?.displayToKg(unit) ?: s.hintWeightKg,
                isCompleted = s.isCompleted,
                setType = s.setType,
            )
        }
        ExerciseProgress.sessionPrs(baseline, samples).keys.forEach { idx -> keys.add("${group.uid}#$idx") }
    }
    return keys
}
