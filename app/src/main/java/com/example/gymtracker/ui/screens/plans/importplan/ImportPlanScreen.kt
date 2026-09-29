package com.example.gymtracker.ui.screens.plans.importplan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.plan.PlanImportExercise
import com.example.gymtracker.ui.AppViewModelFactory
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.ExercisePickerSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlanScreen(
    onBack: () -> Unit,
    onImported: (Long) -> Unit,
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

    var pickerForIndex by remember { mutableStateOf<Int?>(null) }
    var createForIndex by remember { mutableStateOf<Int?>(null) }

    val pending = state.entries.count { it.resolution is Resolution.Pending }
    val skipped = state.entries.count { it.resolution is Resolution.Skipped }
    val resolved = state.entries.size - pending - skipped
    val canCommit = state.payload != null && pending == 0 && resolved > 0 && !state.committing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.payload == null) {
                PickSection(
                    error = state.parseError,
                    onTextLoaded = viewModel::parseText,
                )
            } else {
                ParsedSummary(
                    name = state.payload!!.name,
                    total = state.entries.size,
                    resolved = resolved,
                    pending = pending,
                    skipped = skipped,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(state.entries, key = { idx, _ -> idx }) { idx, entry ->
                        EntryRow(
                            entry = entry,
                            onMap = { pickerForIndex = idx },
                            onCreate = { createForIndex = idx },
                            onSkip = { viewModel.skipEntry(idx) },
                            onUndo = { viewModel.unresolveEntry(idx) },
                        )
                    }
                }
                Button(
                    onClick = { viewModel.commit() },
                    enabled = canCommit,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    Text(
                        if (state.committing) "Importing…"
                        else "Import plan ($resolved exercise${if (resolved == 1) "" else "s"})"
                    )
                }
            }
        }

        pickerForIndex?.let { idx ->
            ExercisePickerSheet(
                repository = container.repository,
                onPick = { ex ->
                    viewModel.mapEntry(idx, ex)
                    pickerForIndex = null
                },
                onDismiss = { pickerForIndex = null },
            )
        }

        createForIndex?.let { idx ->
            val src = state.entries[idx].source
            CreateCustomSheet(
                source = src,
                onCancel = { createForIndex = null },
                onConfirm = { res ->
                    viewModel.setResolution(idx, res)
                    createForIndex = null
                },
            )
        }
    }
}

@Composable
private fun PickSection(
    error: String?,
    onTextLoaded: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                readError = "Could not read the selected file."
            } else {
                onTextLoaded(text)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Pick a JSON file that follows the import format.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                launcher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Outlined.UploadFile,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(if (loading) "Reading…" else "Pick JSON file")
        }
        val message = readError ?: error
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FormatHint()
    }
}

@Composable
private fun FormatHint() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                if (expanded) "Hide format" else "Show format",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (expanded) {
                Text(
                    EXAMPLE_FULL,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ParsedSummary(
    name: String,
    total: Int,
    resolved: Int,
    pending: Int,
    skipped: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    append("$resolved/$total matched")
                    if (pending > 0) append(" · $pending need attention")
                    if (skipped > 0) append(" · $skipped skipped")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: ImportEntry,
    onMap: () -> Unit,
    onCreate: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
) {
    val borderColor = when (entry.resolution) {
        is Resolution.Mapped, is Resolution.Create -> MaterialTheme.colorScheme.primary
        Resolution.Pending -> MaterialTheme.colorScheme.error
        Resolution.Skipped -> MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.source.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${entry.source.sets} × ${entry.source.repsLow}–${entry.source.repsHigh}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ResolutionBadge(entry.resolution, borderColor)
            }
            when (entry.resolution) {
                Resolution.Pending -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onMap, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Map")
                    }
                    FilledTonalButton(onClick = onCreate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Create")
                    }
                    TextButton(onClick = onSkip) { Text("Skip") }
                }
                else -> TextButton(onClick = onUndo) { Text("Change") }
            }
        }
    }
}

@Composable
private fun ResolutionBadge(resolution: Resolution, color: androidx.compose.ui.graphics.Color) {
    val (icon, label) = when (resolution) {
        is Resolution.Mapped -> Icons.Outlined.CheckCircle to "→ ${resolution.exerciseName}"
        is Resolution.Create -> Icons.Outlined.AddCircle to "New: ${resolution.name}"
        Resolution.Skipped -> Icons.Outlined.Edit to "Skipped"
        Resolution.Pending -> Icons.Outlined.Edit to "Needs review"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = 2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCustomSheet(
    source: PlanImportExercise,
    onCancel: () -> Unit,
    onConfirm: (Resolution.Create) -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var name by remember { mutableStateOf(source.name) }
    var movementType by remember {
        mutableStateOf(
            source.movementType?.let { runCatching { MovementType.valueOf(it) }.getOrNull() }
                ?: MovementType.Compound
        )
    }
    var equipment by remember {
        mutableStateOf(
            source.equipment?.let { runCatching { Equipment.valueOf(it) }.getOrNull() }
                ?: Equipment.Other
        )
    }
    var primary by remember {
        mutableStateOf(source.primaryMuscles.mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() }.toSet())
    }
    var secondary by remember {
        mutableStateOf(source.secondaryMuscles.mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() }.toSet())
    }

    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Create custom exercise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Text("Movement type", style = MaterialTheme.typography.labelMedium)
            ChipRow(options = MovementType.values().toList(), selected = setOf(movementType),
                isSingleSelect = true, onToggle = { movementType = it }, labelFor = { it.name })

            Text("Equipment", style = MaterialTheme.typography.labelMedium)
            ChipRow(options = Equipment.values().toList(), selected = setOf(equipment),
                isSingleSelect = true, onToggle = { equipment = it }, labelFor = { it.name })

            Text("Primary muscles", style = MaterialTheme.typography.labelMedium)
            ChipRow(options = Muscle.values().toList(), selected = primary,
                isSingleSelect = false,
                onToggle = { primary = if (it in primary) primary - it else primary + it },
                labelFor = { it.name })

            Text("Secondary muscles", style = MaterialTheme.typography.labelMedium)
            ChipRow(options = Muscle.values().toList(), selected = secondary,
                isSingleSelect = false,
                onToggle = { secondary = if (it in secondary) secondary - it else secondary + it },
                labelFor = { it.name })

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = {
                        onConfirm(
                            Resolution.Create(
                                name = name.trim().ifBlank { source.name },
                                movementType = movementType,
                                primaryMuscles = primary.toList(),
                                secondaryMuscles = secondary.toList(),
                                equipment = equipment,
                            )
                        )
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(),
                ) { Text("Create") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Any> ChipRow(
    options: List<T>,
    selected: Set<T>,
    isSingleSelect: Boolean,
    onToggle: (T) -> Unit,
    labelFor: (T) -> String,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt in selected,
                onClick = { onToggle(opt) },
                label = { Text(labelFor(opt)) },
            )
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

Optional fields (movementType/primaryMuscles/
secondaryMuscles/equipment) pre-fill the
Create-custom form when name doesn't match.

Valid movementType: Compound, Isolation, Cardio, Mobility
Valid equipment: Barbell, Dumbbell, Machine, Cable,
                 Bodyweight, Kettlebell, Band, Other
Muscle names: Chest, FrontDelts, SideDelts, RearDelts,
              Biceps, Triceps, Forearms, UpperBack, Lats,
              Traps, LowerBack, Abs, Obliques, Glutes,
              Quads, Hamstrings, Calves"""
