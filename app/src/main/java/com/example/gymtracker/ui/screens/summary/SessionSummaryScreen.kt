package com.example.gymtracker.ui.screens.summary

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Trophy
import com.composables.icons.lucide.Weight
import com.example.gymtracker.ui.components.StatTile
import com.example.gymtracker.ui.components.icon
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.theme.Pastel
import com.composables.icons.lucide.X
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.components.atoms.RoundIconButton
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.components.atoms.staggerIn
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.meshGradient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.kgToDisplay
import com.example.gymtracker.data.repUnit
import com.example.gymtracker.domain.PrType
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.RecordRow
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.formatVolume
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.formatWeight
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.format.trimDecimal
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Session summary. Pushed after Finish in place of the logger. Duration hero, three stat
 * columns, records, per-exercise best set (with skipped counts), and an inline note
 * written to the session on Done / ✕ / back.
 */
@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SessionSummaryViewModel = viewModel(
        key = "summary-$sessionId",
        factory = AppViewModelFactory(
            mapOf(SessionSummaryViewModel::class.java to {
                SessionSummaryViewModel(container.repository, container.prefs, sessionId)
            })
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state) { if (note == null) state?.let { note = it.notes } }
    LaunchedEffect(closed) { if (closed) onDone() }

    val close = { viewModel.saveAndClose(note.orEmpty()) }
    BackHandler(onBack = close)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                actions = {
                    RoundIconButton(icon = Lucide.X, contentDescription = "Done", onClick = close)
                    Spacer(Modifier.width(Spacing.s))
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        val s = state ?: return@Scaffold
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                // The page's only IME consumer: keeps the note field above the keyboard.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            SummaryHero(s, Modifier.staggerIn(0))

            Column(Modifier.staggerIn(1), verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.section)) {
                    StatTile(Rich.Sets, Lucide.Layers, "Sets", "${s.doneSets}", Modifier.weight(1f).fillMaxHeight(), unit = plural(s.doneSets, "set"))
                    StatTile(Rich.Volume, Lucide.Weight, "Volume", formatVolumeNumber(s.volumeKg, s.unit), Modifier.weight(1f).fillMaxHeight(), unit = s.unit.label)
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.section)) {
                    StatTile(Rich.Body, Lucide.Dumbbell, "Exercises", "${s.exerciseCount}", Modifier.weight(1f).fillMaxHeight())
                    StatTile(
                        Rich.Records, Lucide.Trophy, "Records", "${s.records.size}", Modifier.weight(1f).fillMaxHeight(),
                        unit = plural(s.records.size, "PR"),
                    )
                }
            }

            Section(
                title = "Records",
                modifier = Modifier.staggerIn(2),
                trailing = if (s.records.isEmpty()) null else {
                    {
                        Text(
                            "${s.records.size} ${plural(s.records.size, "PR")}",
                            style = AppText.label,
                            color = Rich.Records.ink,
                        )
                    }
                },
            ) {
                if (s.records.isEmpty()) {
                    Text("No records this time", style = AppText.bodyS, color = scheme.onSurfaceVariant)
                } else {
                    s.records.forEachIndexed { i, r ->
                        if (i > 0) AppDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Rich.Records.brush),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Lucide.Trophy, contentDescription = null, tint = Rich.Records.ink, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(Spacing.m))
                            RecordRow(
                                exercise = r.exerciseName,
                                kind = r.type.summaryLabel,
                                value = r.valueText(s.unit),
                                delta = r.deltaText(s.unit),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Section(title = "Exercises", modifier = Modifier.staggerIn(3)) {
                s.exercises.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        PastelIcon(e.movementType.icon, if (e.doneSets > 0) Pastel.Peach else Pastel.Sand, size = 36.dp)
                        Spacer(Modifier.width(Spacing.m))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = AppText.titleM, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                e.lineText(s.unit) + if (e.skippedSets > 0) " · ${e.skippedSets} skipped" else "",
                                style = AppText.bodyS,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Section(title = "Note", modifier = Modifier.staggerIn(4)) {
                BasicTextField(
                    value = note.orEmpty(),
                    onValueChange = { note = it },
                    textStyle = AppText.body.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(softFill())
                        .padding(Spacing.m),
                    decorationBox = { inner ->
                        Box {
                            if (note.isNullOrEmpty()) Text("Add a note…", style = AppText.body, color = scheme.onSurfaceVariant)
                            inner()
                        }
                    },
                )
            }

            AppButton(text = "Done", onClick = close, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * The finished workout on Foodprint's soft mesh gradient: title and date, and the duration as the
 * one hero number. The other figures sit in the bento tiles under it.
 */
@Composable
private fun SummaryHero(s: SessionSummaryState, modifier: Modifier = Modifier) {
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
            Text("Workout complete", style = AppText.eyebrow, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            if (s.records.isNotEmpty()) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(cardColor().copy(alpha = 0.75f))
                        .padding(horizontal = Spacing.m, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Lucide.Trophy, contentDescription = null, tint = Rich.Records.ink, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("${s.records.size} ${plural(s.records.size, "PR")}", style = AppText.label, color = scheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(s.planName ?: "Quick log", style = AppText.titleL, color = scheme.onSurface)
        Text(
            LocalDate.ofEpochDay(s.dateEpochDay).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())),
            style = AppText.bodyS,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.l))
        Text(
            s.durationMillis?.let { formatClock(it) } ?: EM_DASH,
            style = AppText.numberXXL,
            color = scheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Timer, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Duration", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
        }
    }
}

private val PrType.summaryLabel: String
    get() = when (this) {
        PrType.OneRepMax -> "Est. 1RM"
        PrType.Weight -> "Heaviest"
        PrType.Volume -> "Best set volume"
        PrType.Reps -> "Max reps"
    }

private fun SummaryRecord.valueText(unit: WeightUnit): String = when (type) {
    PrType.Reps -> "${value.roundToInt()}${NBSP}reps"
    PrType.Volume -> formatVolume(value.toDouble(), unit)
    else -> formatWeight(value, unit)
}

private fun SummaryRecord.deltaText(unit: WeightUnit): String? {
    val d = delta ?: return null
    val sign = if (d < 0) "−" else "+"
    return when (type) {
        PrType.Reps -> "$sign${abs(d).roundToInt()}"
        PrType.Volume -> sign + formatVolumeNumber(abs(d).toDouble(), unit)
        else -> sign + trimDecimal(abs(d).kgToDisplay(unit))
    }
}

/** `3 sets · 62.5×9`, `1 set · 25 min`, or `2 sets` when nothing was completed with values. */
private fun SummaryExercise.lineText(unit: WeightUnit): String {
    val count = "$doneSets ${plural(doneSets, "set")}"
    val reps = bestReps ?: return count
    val best = when {
        movementType == MovementType.Cardio || movementType == MovementType.Mobility ->
            "$reps${NBSP}${movementType.repUnit}"
        bestWeightKg != null -> "${trimDecimal(bestWeightKg.kgToDisplay(unit))}×$reps"
        else -> "$reps${NBSP}reps"
    }
    return "$count · $best"
}
