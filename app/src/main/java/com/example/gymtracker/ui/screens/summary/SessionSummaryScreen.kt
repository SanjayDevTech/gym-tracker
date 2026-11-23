package com.example.gymtracker.ui.screens.summary

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.StatColumns
import com.example.gymtracker.ui.components.atoms.StatItem
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
 * Session summary — §4.5. Pushed after Finish in place of the logger. Duration hero, three stat
 * columns, records, per-exercise best set (with skipped counts, decision 8), and an inline note
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
                    IconButton(onClick = close) { Icon(Icons.Outlined.Close, contentDescription = "Done") }
                },
            )
        },
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
        ) {
            Eyebrow("Workout complete", color = scheme.primary)
            Text(s.planName ?: "Quick log", style = AppText.titleL, color = scheme.onSurface)
            Text(
                LocalDate.ofEpochDay(s.dateEpochDay).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())),
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                s.durationMillis?.let { formatClock(it) } ?: EM_DASH,
                style = AppText.numberXXL,
                color = scheme.onSurface,
            )
            Eyebrow("Duration")
            Spacer(Modifier.height(Spacing.xxl))
            StatColumns(
                items = listOf(
                    StatItem("Sets", "${s.doneSets}"),
                    StatItem("Volume", formatVolumeNumber(s.volumeKg, s.unit), s.unit.label),
                    StatItem("Exercises", "${s.exerciseCount}"),
                ),
                valueStyle = AppText.numberL,
            )

            Spacer(Modifier.height(Spacing.section))
            Section(
                title = "Records",
                trailing = if (s.records.isEmpty()) null else {
                    {
                        Text(
                            "${s.records.size} ${plural(s.records.size, "PR")}",
                            style = AppText.bodyS,
                            color = scheme.primary,
                        )
                    }
                },
            ) {
                if (s.records.isEmpty()) {
                    Text("No records this time", style = AppText.bodyS, color = scheme.onSurfaceVariant)
                } else {
                    s.records.forEachIndexed { i, r ->
                        if (i > 0) AppDivider()
                        RecordRow(
                            exercise = r.exerciseName,
                            kind = r.type.summaryLabel,
                            value = r.valueText(s.unit),
                            delta = r.deltaText(s.unit),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.section))
            Section(title = "Exercises") {
                s.exercises.forEachIndexed { i, e ->
                    if (i > 0) AppDivider()
                    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.m)) {
                        Row {
                            Text(
                                e.name,
                                style = AppText.titleM,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(e.lineText(s.unit), style = AppText.bodyS, color = scheme.onSurfaceVariant)
                        }
                        if (e.skippedSets > 0) {
                            Text("${e.skippedSets} skipped", style = AppText.bodyS, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.section))
            Section(title = "Note") {
                BasicTextField(
                    value = note.orEmpty(),
                    onValueChange = { note = it },
                    textStyle = AppText.body.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (note.isNullOrEmpty()) Text("Add a note…", style = AppText.body, color = scheme.onSurfaceVariant)
                            inner()
                        }
                    },
                )
            }

            Spacer(Modifier.height(Spacing.section))
            AppButton(text = "Done", onClick = close, modifier = Modifier.fillMaxWidth())
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
