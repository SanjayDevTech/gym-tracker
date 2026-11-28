package com.example.gymtracker.ui.screens.history

import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Weight
import com.example.gymtracker.ui.components.StatTile
import com.example.gymtracker.ui.components.icon
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.theme.meshGradient
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.components.atoms.staggerIn
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.Metric
import com.example.gymtracker.ui.components.atoms.AppCard
import com.composables.icons.lucide.Trophy
import com.composables.icons.lucide.Lucide
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.gymtracker.ui.components.MenuItem
import com.example.gymtracker.ui.components.NoteLine
import com.example.gymtracker.ui.components.OverflowMenu
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
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Effort
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
                actions = { if (s != null) OverflowMenu(listOf(MenuItem("Delete session", { confirmDelete = true }, destructive = true))) },
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
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item(key = "stats") {
                DetailHero(s, Modifier.staggerIn(0))
            }
            item(key = "tiles") {
                Column(Modifier.staggerIn(1), verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.section)) {
                        StatTile(Rich.Sets, Lucide.Layers, "Sets", "${s.doneSets}", Modifier.weight(1f).fillMaxHeight(), unit = plural(s.doneSets, "set"))
                        StatTile(
                            Rich.Volume, Lucide.Weight, "Volume",
                            if (s.volumeKg > 0.0) formatVolumeNumber(s.volumeKg, s.unit) else EM_DASH,
                            Modifier.weight(1f).fillMaxHeight(),
                            unit = if (s.volumeKg > 0.0) s.unit.label else "",
                            caption = "Warm-ups out",
                        )
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.section)) {
                        StatTile(Rich.Body, Lucide.Dumbbell, "Exercises", "${s.exerciseCount}", Modifier.weight(1f).fillMaxHeight())
                        StatTile(Rich.Records, Lucide.Trophy, "Records", "${s.prCount}", Modifier.weight(1f).fillMaxHeight(), unit = plural(s.prCount, "PR"))
                    }
                }
            }
            item(key = "note") {
                AppCard(Modifier.staggerIn(2)) {
                    NoteLine(note = s.notes, onClick = { editingNote = true }, label = "Note")
                }
            }
            s.exercises.forEachIndexed { i, e ->
                item(key = "exercise-${e.exerciseId}") {
                    ExerciseTable(
                        exercise = e,
                        unit = s.unit,
                        onOpen = { onOpenExercise(e.exerciseId) },
                        modifier = Modifier.staggerIn(3 + i),
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

/**
 * The session on Foodprint's soft gradient hero: the date up top with a PR chip and the duration
 * as the one big figure. Sets, volume, exercises and records sit in the tiles under it.
 */
@Composable
private fun DetailHero(s: SessionDetailState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient(animate = false)
            .dotTexture(color = scheme.onSurface.copy(alpha = 0.10f))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                LocalDate.ofEpochDay(s.dateEpochDay).format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.getDefault())),
                style = AppText.eyebrow,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (s.prCount > 0) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(cardColor().copy(alpha = 0.75f))
                        .padding(horizontal = Spacing.m, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Lucide.Trophy, contentDescription = null, tint = Rich.Records.ink, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("${s.prCount} ${plural(s.prCount, "record")}", style = AppText.label, color = scheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            s.durationMillis?.let(::formatClock) ?: EM_DASH,
            style = AppText.numberXXL,
            color = scheme.onSurface,
            modifier = Modifier.semantics { contentDescription = "Duration ${s.durationMillis?.let(::formatClock) ?: "not timed"}" },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Timer, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Duration", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
        }
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
            SheetTitleRow("Note", onClose = onDismiss)
            Spacer(Modifier.height(Spacing.m))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = AppText.body.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(softFill())
                            .padding(Spacing.m),
                    ) {
                        if (text.isEmpty()) Text("Add a note…", style = AppText.body, color = scheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
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
    AppCard(modifier) {
        // Head → Exercise detail.
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Open ${exercise.name}", onClick = onOpen)
                .heightIn(min = Sizes.touchTarget),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PastelIcon(exercise.movementType.icon, Pastel.Peach, size = 36.dp)
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = AppText.titleM,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull("${exercise.doneSets} ${plural(exercise.doneSets, "set")}", exercise.target).joinToString(" · "),
                        style = AppText.bodyS,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Icon(Lucide.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(Spacing.m))
        // Each set on its own soft pill: no rules, just space.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            exercise.sets.forEach { set ->
                SetLine(set = set, unit = unit, lifting = lifting, repUnit = exercise.movementType.repUnit)
            }
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
        set.isPr -> Rich.Records.ink
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
            .clip(RoundedCornerShape(Radius.md))
            .background(softFill())
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
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
            if (set.isPr) AppBadge("PR", container = scheme.tertiary, content = scheme.onTertiary)
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
