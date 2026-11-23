package com.example.gymtracker.ui.screens.exerciseform

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.MovementType
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.displayName
import com.example.gymtracker.domain.ExerciseNameRule
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/** The selection rule under a text option (house rule 3). A size. */
private val RuleWidth = 24.dp
private val RuleHeight = 2.dp

/**
 * Custom exercise form — design-plan §4.19 (phase 9). A full page from Exercises' FAB, the
 * picker's empty search, Exercise detail ⋮ Edit and Import's Create.
 *
 * Top bar with a primary `Save` text button, disabled until the name is valid; then NAME,
 * TYPE, EQUIPMENT, PRIMARY and SECONDARY MUSCLES as standing sections. The name must be
 * non-empty and not another live exercise's (ignoring case), with the reason inline under the
 * field. Edit mode draws the top bar only until the exercise loads.
 */
@Composable
fun ExerciseFormScreen(
    viewModel: ExerciseFormViewModel,
    onBack: () -> Unit,
    onSaved: (SavedExercise) -> Unit,
) {
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    // A blank error only after the user has emptied the field, not on a fresh form.
    var nameTouched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(saved) { saved?.let(onSaved) }
    LaunchedEffect(blocked) { if (blocked) onBack() }

    val f = fields
    val all = existing
    val validation = if (f != null && all != null) viewModel.validate(f, all) else null

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.editing) "Edit exercise" else "New exercise",
                onBack = onBack,
                actions = {
                    AppTextButton(
                        text = "Save",
                        onClick = viewModel::save,
                        enabled = validation is ExerciseNameRule.Result.Ok && saved == null,
                    )
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        if (f == null) return@Scaffold // Edit loading: top bar only.
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(top = Spacing.s, bottom = Spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Section("Name") {
                UnderlineTextField(
                    value = f.name,
                    onValueChange = { v ->
                        nameTouched = true
                        viewModel.edit { it.copy(name = v) }
                    },
                    error = when (validation) {
                        is ExerciseNameRule.Result.Duplicate -> "You already have ${validation.existingName}"
                        ExerciseNameRule.Result.Blank -> if (nameTouched) "Give it a name" else null
                        else -> null
                    },
                )
            }
            Section("Type") {
                UnderlineSelector(
                    options = MovementType.entries.toList(),
                    selected = f.movementType,
                    onSelect = { t -> viewModel.edit { it.copy(movementType = t) } },
                    label = { it.displayName },
                )
            }
            Section("Equipment") {
                TextOptions(
                    options = Equipment.entries.toList(),
                    isSelected = { it == f.equipment },
                    onTap = { e -> viewModel.edit { it.copy(equipment = e) } },
                    label = { it.displayName },
                    multi = false,
                )
            }
            MuscleSection("Primary muscles", f.primary) { m -> viewModel.edit { it.copy(primary = it.primary.toggle(m)) } }
            MuscleSection("Secondary muscles", f.secondary) { m -> viewModel.edit { it.copy(secondary = it.secondary.toggle(m)) } }
        }
    }
}

/** A muscle section: the selected count as the head's figure; 18 names as text options. */
@Composable
private fun MuscleSection(title: String, selected: Set<Muscle>, onToggle: (Muscle) -> Unit) {
    Section(
        title = title,
        trailing = if (selected.isEmpty()) null else {
            { Text("${selected.size}", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) {
        TextOptions(
            options = Muscle.entries.toList(),
            isSelected = { it in selected },
            onTap = onToggle,
            label = { it.displayName },
            multi = true,
        )
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

/**
 * Wrapped text options: selected = onSurface label over a 24×2dp rule, unselected
 * onSurfaceVariant (house rule 3 — no chips). [multi] toggles; otherwise one choice.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> TextOptions(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onTap: (T) -> Unit,
    label: (T) -> String,
    multi: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
        options.forEach { option ->
            val on = isSelected(option)
            val select = if (multi) {
                Modifier.toggleable(value = on, role = Role.Checkbox, onValueChange = { onTap(option) })
            } else {
                Modifier.selectable(selected = on, role = Role.RadioButton, onClick = { if (!on) onTap(option) })
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .heightIn(min = Sizes.touchTarget)
                    .then(select)
                    .padding(vertical = Spacing.s),
            ) {
                Text(label(option), style = AppText.label, color = if (on) scheme.onSurface else scheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    Modifier
                        .size(width = RuleWidth, height = RuleHeight)
                        .background(if (on) scheme.onSurface else Color.Transparent),
                )
            }
        }
    }
}

/**
 * One-line `titleM` text input in the [com.example.gymtracker.ui.components.atoms.NumberField]
 * idiom: no box, a rule beneath (2dp primary when focused, error when [error] is set), and the
 * error as a `bodyS` line under it.
 */
@Composable
private fun UnderlineTextField(value: String, onValueChange: (String) -> Unit, error: String?) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ruleColor = when {
        error != null -> scheme.error
        focused -> scheme.primary
        else -> scheme.outlineVariant
    }
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppText.titleM.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Landmine press", style = AppText.titleM, color = scheme.onSurfaceVariant)
                    inner()
                }
            },
        )
        Spacer(Modifier.height(Spacing.xs))
        Box(Modifier.fillMaxWidth().height(RuleHeight), contentAlignment = Alignment.TopStart) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (focused || error != null) RuleHeight else Elevation.hairline)
                    .background(ruleColor),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(error, style = AppText.bodyS, color = scheme.error)
        }
    }
}
