package com.example.gymtracker.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppBadge
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.ConfirmSheet
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.ValueText
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.formatWeightNumber
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Effort
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Session detail — §4.9, a leaf page from History. Stat row (DURATION · SETS · VOLUME · PRS), a
 * caption, the note (tap to edit), then one set table per exercise: warm-ups muted, skipped sets
 * (decision 8) muted with "skipped", PR badges recomputed against earlier history. ⋮ → Delete
 * behind a [ConfirmSheet] (house rule 10).
 */
@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit, onOpenExercise: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: SessionDetailViewModel = viewModel(
        key = "session-detail-$sessionId",
        factory = AppViewModelFactory(
            mapOf(SessionDetailViewModel::class.java to {
                SessionDetailViewModel(container.repository, container.prefs, sessionId)
            })
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var editingNote by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            val s = state
            AppTopBar(
                title = s?.let { it.planName ?: "Quick log" }.orEmpty(),
                subtitle = s?.let {
                    LocalDate.ofEpochDay(it.dateEpochDay)
                        .format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault()))
                },
                onBack = onBack,
                actions = { if (s != null) OverflowMenu(onDelete = { confirmDelete = true }) },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        // Loading: top bar only (house rule 8).
        val s = state ?: return@Scaffold
        val scheme = MaterialTheme.colorScheme
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.s,
                bottom = Spacing.xxl,
            ),
        ) {
            item(key = "stats") {
                Column {
                    DetailStats(s)
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        "${s.exerciseCount} ${if (s.exerciseCount == 1) "exercise" else "exercises"} · " +
                            "Volume = weight × reps, warm-ups out.",
                        style = AppText.bodyS,
                        color = scheme.onSurfaceVariant,
                    )
                    NoteLine(notes = s.notes, onClick = { editingNote = true })
                }
            }
            s.exercises.forEach { e ->
                item(key = "exercise-${e.exerciseId}") {
                    ExerciseTable(
                        exercise = e,
                        unit = s.unit,
                        onOpen = { onOpenExercise(e.exerciseId) },
                        modifier = Modifier.padding(top = Spacing.section),
                    )
                }
            }
        }
    }

    if (editingNote) {
        NoteSheet(
            initial = state?.notes.orEmpty(),
            onSave = {
                vm.saveNote(it)
                editingNote = false
            },
            onDismiss = { editingNote = false },
        )
    }

    if (confirmDelete) {
        val sets = state?.doneSets ?: 0
        ConfirmSheet(
            title = "Delete this session?",
            message = "Its $sets ${if (sets == 1) "set leaves" else "sets leave"} History and Progress. This can't be undone.",
            confirmLabel = "Delete session",
            onConfirm = {
                confirmDelete = false
                vm.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun OverflowMenu(onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(Radius.md),
            containerColor = scheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text("Delete session", style = AppText.body, color = scheme.error) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * The S4-c stat row: four equal columns, eyebrow over [AppText.numberM]. A private twin of
 * `StatColumns` because PRS needs its own colour (primary when > 0), which the atom can't take.
 */
@Composable
private fun DetailStats(s: SessionDetailState) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.Bottom,
    ) {
        StatCell("Duration", s.durationMillis?.let(::formatClock) ?: EM_DASH, null, scheme.onSurface)
        StatCell("Sets", "${s.doneSets}", null, scheme.onSurface)
        StatCell(
            "Volume",
            if (s.volumeKg > 0.0) formatVolumeNumber(s.volumeKg, s.unit) else EM_DASH,
            if (s.volumeKg > 0.0) s.unit.label else null,
            scheme.onSurface,
        )
        StatCell("PRs", "${s.prCount}", null, if (s.prCount > 0) scheme.primary else scheme.onSurface)
    }
}

@Composable
private fun RowScope.StatCell(
    label: String,
    value: String,
    unit: String?,
    color: Color,
) {
    Column(Modifier.weight(1f)) {
        Eyebrow(label)
        ValueText(value = value, unit = unit, style = AppText.numberM, color = color)
    }
}

@Composable
private fun NoteLine(notes: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit note", onClick = onClick)
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow("Note")
        Spacer(Modifier.width(Spacing.m))
        Text(
            notes.ifBlank { "Add a note…" },
            style = AppText.body,
            color = if (notes.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NoteSheet(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { focus.requestFocus() }
    AppSheet(onDismiss = onDismiss) {
        // The sheet's only IME consumer: keeps the field and Save above the keyboard.
        Column(Modifier.imePadding()) {
            Text("Note", style = AppText.titleL, color = scheme.onSurface)
            Spacer(Modifier.height(Spacing.l))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = AppText.body.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("Add a note…", style = AppText.body, color = scheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(Spacing.s))
            AppDivider()
            AppButton(
                text = "Save",
                onClick = { onSave(text) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
            )
        }
    }
}

/** Width of the set-number column — a size (fits `W`/`10` at numberM), not a gap. */
private val BadgeColumn = 32.dp

/** Width the PR badge slot reserves so weight and reps columns stay aligned across rows. */
private val PrColumn = 40.dp

@Composable
private fun ExerciseTable(
    exercise: DetailExercise,
    unit: WeightUnit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val lifting = exercise.movementType == MovementType.Compound || exercise.movementType == MovementType.Isolation
    Column(modifier.fillMaxWidth()) {
        // Head → Exercise detail.
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Open ${exercise.name}", onClick = onOpen)
                .heightIn(min = Sizes.touchTarget),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.name,
                    style = AppText.titleM,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.m))
                Text(
                    "${exercise.doneSets} ${if (exercise.doneSets == 1) "set" else "sets"}",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (exercise.target != null) {
                Eyebrow(exercise.target, color = scheme.tertiary)
            }
        }
        Spacer(Modifier.height(Spacing.s))
        AppDivider()
        exercise.sets.forEach { set ->
            SetLine(set = set, unit = unit, lifting = lifting, repUnit = exercise.movementType.repUnit)
        }
    }
}

/** `W   40 kg   12 reps` — S2-c table row, numberM, right-aligned value columns. */
@Composable
private fun SetLine(set: DetailSet, unit: WeightUnit, lifting: Boolean, repUnit: String) {
    val scheme = MaterialTheme.colorScheme
    val muted = !set.completed || set.setType == SetType.Warmup
    val valueColor = when {
        muted -> scheme.onSurfaceVariant
        set.isPr -> scheme.primary
        else -> scheme.onSurface
    }
    val badgeColor = when {
        !set.completed -> scheme.onSurfaceVariant
        set.setType == SetType.Failure -> Effort
        set.setType == SetType.Warmup -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(set.badge, style = AppText.numberM, color = badgeColor, modifier = Modifier.widthIn(min = BadgeColumn))
        Spacer(Modifier.width(Spacing.s))
        if (lifting) {
            val weight = when {
                !set.completed -> EM_DASH
                (set.weightKg ?: 0f) <= 0f -> "BW"
                else -> formatWeightNumber(set.weightKg, unit)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                ValueText(
                    value = weight,
                    unit = if (set.completed && (set.weightKg ?: 0f) > 0f) unit.label else null,
                    style = AppText.numberM,
                    color = valueColor,
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Box(Modifier.widthIn(min = PrColumn), contentAlignment = Alignment.Center) {
            if (set.isPr) AppBadge("PR")
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (!set.completed) {
                Text("skipped", style = AppText.bodyS, color = scheme.onSurfaceVariant, textAlign = TextAlign.End)
            } else {
                ValueText(
                    value = set.reps?.toString() ?: EM_DASH,
                    unit = repUnit,
                    style = AppText.numberM,
                    color = valueColor,
                )
            }
        }
    }
}
