package com.example.gymtracker.ui.screens.plans.importplan

import com.composables.icons.lucide.CircleMinus
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleAlert
import com.example.gymtracker.ui.theme.meshGradient
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AppCard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.FileJson
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.plan.PlanImportExercise
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Eyebrow
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Import plan — design-plan §4.16. Two phases on one page.
 *
 * **Pick:** a helper line, `Choose JSON file`, an error line, and the format as a [Section] with
 * the monospace example. **Review:** a `numberXL` "6 / 8" matched hero with the review/skipped
 * counts, the source exercises as [ImportEntryRow]s on hairlines, and a pinned
 * `Import plan (6 exercises)`. Committing always creates a new plan.
 */
@Composable
fun ImportPlanScreen(
    onBack: () -> Unit,
    onImported: (Long) -> Unit,
    onPickExercise: () -> Unit,
    pickedExerciseIds: List<Long>?,
    onPickedHandled: () -> Unit,
    onCreateExercise: (PlanImportExercise) -> Unit,
    createdExerciseId: Long?,
    onCreatedHandled: () -> Unit,
    initialText: String? = null,
) {
    val container = LocalAppContainer.current
    val viewModel: ImportPlanViewModel = viewModel(
        factory = AppViewModelFactory(
            mapOf(ImportPlanViewModel::class.java to { ImportPlanViewModel(container.repository) })
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Deep-linked JSON (opened/shared from outside): parse immediately into the review UI.
    LaunchedEffect(initialText) {
        if (initialText != null) viewModel.parseText(initialText)
    }

    LaunchedEffect(state.committedPlanId) {
        state.committedPlanId?.let(onImported)
    }

    // Saveable: the picker is a page, so this must survive leaving composition.
    var pickerForIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(pickedExerciseIds) {
        val id = pickedExerciseIds?.firstOrNull() ?: return@LaunchedEffect
        val idx = pickerForIndex
        // Resolve before clearing: clearing the result restarts (cancels) this effect.
        val exercise = container.repository.findExercise(id)
        if (idx != null && exercise != null) viewModel.mapEntry(idx, exercise)
        pickerForIndex = null
        onPickedHandled()
    }
    // Create opens the custom-exercise page (§4.19) prefilled; its new id maps the entry.
    var createForIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(createdExerciseId) {
        val id = createdExerciseId ?: return@LaunchedEffect
        val idx = createForIndex
        val exercise = container.repository.findExercise(id)
        if (idx != null && exercise != null) viewModel.mapEntry(idx, exercise)
        createForIndex = null
        onCreatedHandled()
    }

    val pending = state.entries.count { it.resolution is Resolution.Pending }
    val skipped = state.entries.count { it.resolution is Resolution.Skipped }
    val resolved = state.entries.size - pending - skipped
    val canCommit = state.payload != null && pending == 0 && resolved > 0 && !state.committing

    Scaffold(
        topBar = { AppTopBar(title = "Import plan", onBack = onBack) },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        val payload = state.payload
        if (payload == null) {
            PickPhase(
                error = state.parseError,
                onTextLoaded = viewModel::parseText,
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.gutter),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = Spacing.s, bottom = Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.section),
                ) {
                    item("hero") {
                        ReviewHero(
                            name = payload.name,
                            total = state.entries.size,
                            resolved = resolved,
                            pending = pending,
                            skipped = skipped,
                        )
                    }
                    itemsIndexed(state.entries, key = { idx, _ -> idx }) { idx, entry ->
                        ImportEntryRow(
                            entry = entry,
                            onMap = { pickerForIndex = idx; onPickExercise() },
                            onCreate = { createForIndex = idx; onCreateExercise(entry.source) },
                            onSkip = { viewModel.skipEntry(idx) },
                            onChange = { viewModel.unresolveEntry(idx) },
                        )
                    }
                }
                AppButton(
                    text = if (state.committing) "Importing…"
                    else "Import plan ($resolved ${plural(resolved, "exercise")})",
                    onClick = { viewModel.commit() },
                    enabled = canCommit,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                )
            }
        }
    }

}

@Composable
private fun PickPhase(
    error: String?,
    onTextLoaded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var loading by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        loading = true
        readError = null
        scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                }
            }.getOrNull()
            loading = false
            if (text.isNullOrBlank()) {
                readError = "Couldn't read the selected file."
            } else {
                onTextLoaded(text)
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
    ) {
        AppCard {
            Row(verticalAlignment = Alignment.Top) {
                PastelIcon(Lucide.FileJson, Pastel.Sky)
                Spacer(Modifier.size(Spacing.m))
                Text(
                    "Choose a plan JSON file. You'll review how each exercise matches your library " +
                        "before anything is saved, and it always becomes a new plan.",
                    style = AppText.body,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.l))
            AppButton(
                text = if (loading) "Reading…" else "Choose JSON file",
                onClick = { launcher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            val message = readError ?: error
            if (message != null) {
                Spacer(Modifier.height(Spacing.s))
                Text(message, style = AppText.bodyS, color = scheme.error)
            }
        }
        Spacer(Modifier.height(Spacing.section))
        Section("Format") {
            Text(
                EXAMPLE_FULL,
                style = AppText.bodyS.copy(fontFamily = FontFamily.Monospace),
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** Plan name, then `6 / 8` matched as the big figure and the review / skipped counts as a chip. */
@Composable
private fun ReviewHero(
    name: String,
    total: Int,
    resolved: Int,
    pending: Int,
    skipped: Int,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient(animate = false)
            .dotTexture(color = scheme.onSurface.copy(alpha = 0.10f))
            .padding(20.dp),
    ) {
        Text("Plan", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
        Text(name, style = AppText.titleL, color = scheme.onSurface)
        Spacer(Modifier.height(Spacing.l))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$resolved / $total", style = AppText.numberXL, color = scheme.onSurface)
            Text(" matched", style = AppText.titleM, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(Spacing.s))
        Text(
            "$pending need${if (pending == 1) "s" else ""} review · $skipped skipped",
            style = AppText.label,
            color = if (pending > 0) scheme.error else scheme.onSurface,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(cardColor().copy(alpha = 0.75f))
                .padding(horizontal = Spacing.m, vertical = 6.dp),
        )
    }
}

/**
 * §3 `ImportEntryRow`: source name + `3 × 8–12`; the resolution as one text line —
 * `→ Bench press` in the positive colour (also after Create, which saves the exercise),
 * `– Skipped` onSurfaceVariant, `! Needs review` error — on its own card, actions as pills.
 */
@Composable
private fun ImportEntryRow(
    entry: ImportEntry,
    onMap: () -> Unit,
    onCreate: () -> Unit,
    onSkip: () -> Unit,
    onChange: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (line, color) = when (val r = entry.resolution) {
        is Resolution.Mapped -> "→ ${r.exerciseName}" to scheme.secondary
        Resolution.Skipped -> "– Skipped" to scheme.onSurfaceVariant
        Resolution.Pending -> "! Needs review" to scheme.error
    }
    val (statusIcon, statusPastel) = when (entry.resolution) {
        is Resolution.Mapped -> Lucide.CircleCheck to Pastel.Mint
        Resolution.Skipped -> Lucide.CircleMinus to Pastel.Sand
        Resolution.Pending -> Lucide.CircleAlert to Pastel.Rose
    }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PastelIcon(statusIcon, statusPastel, size = 36.dp)
            Spacer(Modifier.size(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(entry.source.name, style = AppText.titleM, color = scheme.onSurface)
                Text(
                    "${entry.source.sets} × ${entry.source.repsLow}–${entry.source.repsHigh}",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
                Text(line, style = AppText.label, color = color)
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            when (entry.resolution) {
                Resolution.Pending -> {
                    AppButton(text = "Map", onClick = onMap, modifier = Modifier.weight(1f))
                    AppOutlinedButton(text = "Create", onClick = onCreate, modifier = Modifier.weight(1f))
                    AppOutlinedButton(text = "Skip", onClick = onSkip, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
                else -> AppOutlinedButton(text = "Change", onClick = onChange, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private const val EXAMPLE_FULL = """{
  "version": 1,
  "name": "Push Day A",
  "notes": "optional",
  "exercises": [
    {
      "name": "Barbell Bench Press",
      "sets": 4,
      "repsLow": 6,
      "repsHigh": 8,
      "notes": "optional",
      "movementType": "Compound",
      "primaryMuscles": ["Chest"],
      "secondaryMuscles": ["FrontDelts", "Triceps"],
      "equipment": "Barbell"
    }
  ]
}

Optional fields (movementType, primaryMuscles,
secondaryMuscles, equipment) pre-fill the
Create form when a name doesn't match.

movementType: Compound, Isolation, Cardio, Mobility
equipment: Barbell, Dumbbell, Machine, Cable,
           Bodyweight, Kettlebell, Band, Other
Muscles: Chest, FrontDelts, SideDelts, RearDelts,
         Biceps, Triceps, Forearms, UpperBack, Lats,
         Traps, LowerBack, Abs, Obliques, Glutes,
         Quads, Hamstrings, Calves, Adductors"""
