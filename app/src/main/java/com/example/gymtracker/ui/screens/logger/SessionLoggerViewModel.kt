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
import com.example.gymtracker.domain.LoggedSetSample
import com.example.gymtracker.domain.Records
import com.example.gymtracker.domain.toSample
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
    /** "Skip exercise": its unticked sets stay unticked (saved as skipped) and the
     *  block folds. Cleared by ticking any of its sets, or "Unskip exercise". */
    val skipped: Boolean = false,
) {
    val doneCount: Int get() = sets.count { it.isCompleted }
    val allDone: Boolean get() = sets.isNotEmpty() && sets.all { it.isCompleted }
    /** Finished one way or the other: every set ticked, or skipped. Settled blocks fold. */
    val settled: Boolean get() = allDone || skipped
    /** Sets that count toward the session's "x of y": a skipped exercise counts only what it ticked. */
    val countedSets: Int get() = if (skipped) doneCount else sets.size

    /**
     * Replace one set. A set turning completed un-skips the exercise ("ticking any set brings
     * it back").
     */
    fun withSet(setIndex: Int, updated: SetDraft): ExerciseGroup {
        val current = sets.getOrNull(setIndex) ?: return this
        val newSets = sets.toMutableList().also { it[setIndex] = updated }
        val justCompleted = !current.isCompleted && updated.isCompleted
        return copy(sets = newSets, skipped = skipped && !justCompleted)
    }

    /**
     * A typed edit into a set row. Typing auto-ticks the set (the fast path) — except the
     * set that would complete the exercise (the only one left unticked): ticking that one is
     * left to the user, so the block doesn't fold while they are still adjusting it. On the first
     * input into such a set the outcome asks the row to nudge its check.
     */
    fun withInput(setIndex: Int, edit: (SetDraft) -> SetDraft): Pair<ExerciseGroup, InputOutcome> {
        val before = sets.getOrNull(setIndex) ?: return this to InputOutcome.None
        val after = edit(before)
        val hasInput = after.repsInput.isNotEmpty() || after.weightInput.isNotEmpty()
        if (before.isCompleted || !hasInput) return withSet(setIndex, after) to InputOutcome.None
        val wouldComplete = sets.withIndex().all { (i, s) -> i == setIndex || s.isCompleted }
        if (wouldComplete) {
            val firstInput = before.repsInput.isEmpty() && before.weightInput.isEmpty()
            return withSet(setIndex, after) to
                if (firstInput) InputOutcome.NudgeTick else InputOutcome.None
        }
        return withSet(setIndex, after.copy(isCompleted = true)) to InputOutcome.Ticked
    }
}

/** What a typed edit did to its set. */
enum class InputOutcome {
    None,
    /** The set auto-ticked. */
    Ticked,
    /** Not ticked because it would complete the exercise: the row pulses its check instead. */
    NudgeTick,
}

/** The top bar's primary button: Discard until a set is ticked, then Finish. */
enum class PrimaryAction { Discard, Finish }

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
    /** This workout's rest target. */
    val restTargetSeconds: Int = DEFAULT_REST_SECONDS,
    /** Wall-clock the workout was paused at; 0 = running. While paused both clocks stand still. */
    val pausedAtMillis: Long = 0L,
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
    /** Progress denominator ("3 of 12 sets"): a skipped exercise counts only its ticked sets. */
    val totalSets: Int get() = groups.sumOf { it.countedSets }
    val doneSets: Int get() = groups.sumOf { it.doneCount }
    /** Every set in the draft — what Finish saves, ticked or not. */
    val allSets: Int get() = groups.sumOf { it.sets.size }
    val skippedSets: Int get() = allSets - doneSets
    val primaryAction: PrimaryAction get() = if (doneSets > 0) PrimaryAction.Finish else PrimaryAction.Discard
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

    /** Single-level undo buffer for the last removed exercise or set. */
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
                skipped = g.skipped,
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
                pausedAtMillis = payload.pausedAtMillis,
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
            pausedAtMillis = snapshot.pausedAtMillis,
            groups = snapshot.groups.map { g ->
                SessionDraftGroup(
                    exerciseId = g.exerciseId,
                    exerciseName = g.exerciseName,
                    movementType = g.movementType.name,
                    targetRepsLow = g.targetRepsLow,
                    targetRepsHigh = g.targetRepsHigh,
                    uid = g.uid,
                    skipped = g.skipped,
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
     *  against *local* today, not UTC — are refused. */
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
            Records.baseline(repository.observeExerciseSetsWithDate(id).first().map { it.toSample() })
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
            s.withGroup(groupIndex, group.withSet(setIndex, update(current)))
        }
    }

    /** A typed edit into a set row; see [ExerciseGroup.withInput] for the auto-tick rule. */
    fun inputSet(groupIndex: Int, setIndex: Int, edit: (SetDraft) -> SetDraft): InputOutcome {
        var outcome = InputOutcome.None
        _draft.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            val (updated, result) = group.withInput(setIndex, edit)
            outcome = result
            s.withGroup(groupIndex, updated)
        }
        return outcome
    }

    /**
     * Swap in [updated]; a set that just became completed is the moment it's logged — restart
     * rest (and end a pause: ticking a set means you're training again).
     */
    private fun SessionLoggerState.withGroup(groupIndex: Int, updated: ExerciseGroup): SessionLoggerState {
        val previous = groups[groupIndex]
        val newGroups = groups.toMutableList().also { it[groupIndex] = updated }
        val now = System.currentTimeMillis()
        val completedNow = updated.doneCount > previous.doneCount
        val base = if (completedNow) resumeClocks(this, now) else this
        return base.copy(
            groups = newGroups,
            lastSetCompletedAtMillis = if (completedNow) now else base.lastSetCompletedAtMillis,
        )
    }

    /** "Skip exercise": leave its unticked sets unticked (saved as skipped) and fold it. */
    fun skipGroup(groupIndex: Int) = updateGroup(groupIndex) { it.copy(skipped = true) }

    fun unskipGroup(groupIndex: Int) = updateGroup(groupIndex) { it.copy(skipped = false) }

    /** The row's check. Ticking an empty row commits its hint values so they are shown. */
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

    /** Start a rest now (the dock's ring, the Rest sheet's "Restart now"); ends a pause. */
    fun restartRest() {
        val now = System.currentTimeMillis()
        _draft.update { resumeClocks(it, now).copy(lastSetCompletedAtMillis = now) }
    }

    // ---- Pause ------------------------------------------------------------

    /** Pause or resume the workout: both the workout clock and the rest timer. */
    fun togglePause() {
        val now = System.currentTimeMillis()
        _draft.update { if (it.pausedAtMillis > 0L) resumeClocks(it, now) else it.copy(pausedAtMillis = now) }
    }

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
        if (s.saving || s.primaryAction != PrimaryAction.Finish) return
        viewModelScope.launch {
            _draft.update { it.copy(finish = FinishPrompt(planChanges(it))) }
        }
    }

    fun dismissFinish() = _draft.update { it.copy(finish = null) }

    fun confirmFinish(updatePlan: Boolean) {
        val prompt = _draft.value.finish ?: return
        // Finishing while paused: the pause doesn't count toward the session's duration.
        _draft.update { resumeClocks(it, System.currentTimeMillis()) }
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
        val groups = plannableGroups(s.groups)
        groups.forEach { g ->
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
        if (changes.isEmpty() && baseline.map { it.planned.exerciseId } != groups.map { it.exerciseId }) {
            changes += "reordered"
        }
        return changes
    }

    /**
     * The groups a plan may hold: an exercise deleted mid-workout stays in the draft (and its sets
     * are saved) but is never synced back into the plan it was just removed from.
     */
    private suspend fun plannableGroups(groups: List<ExerciseGroup>): List<ExerciseGroup> =
        groups.filter { repository.findExercise(it.exerciseId)?.isDeleted != true }

    private fun rangeLabel(low: Int, high: Int) = if (low == high) "$low" else "$low–$high"

    private fun commitSave(updatePlan: Boolean) {
        if (_draft.value.saving) return
        _draft.update { it.copy(saving = true) }
        viewModelScope.launch {
            val s = _draft.value
            val unit = s.weightUnit
            if (updatePlan && s.planId != null) {
                val planned = plannableGroups(s.groups).mapIndexed { i, g ->
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
                        // Unticked sets are saved as skipped: is_completed = false,
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
        Records.forSession(baseline, samples).prSets.keys.forEach { idx -> keys.add("${group.uid}#$idx") }
    }
    return keys
}

/**
 * Resume [state] at [now]: slide the workout start and the rest base forward by the length of the
 * pause, so paused time counts toward neither clock. A running state is returned unchanged.
 */
internal fun resumeClocks(state: SessionLoggerState, now: Long): SessionLoggerState {
    if (state.pausedAtMillis <= 0L) return state
    val paused = (now - state.pausedAtMillis).coerceAtLeast(0L)
    return state.copy(
        startedAtMillis = if (state.startedAtMillis > 0L) state.startedAtMillis + paused else 0L,
        lastSetCompletedAtMillis = if (state.lastSetCompletedAtMillis > 0L) state.lastSetCompletedAtMillis + paused else 0L,
        pausedAtMillis = 0L,
    )
}
