package com.example.gymtracker.ui.screens.exerciseform

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.gymtracker.ui.components.OptionGrid
import com.example.gymtracker.ui.components.atoms.AppTextField
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.theme.Radius
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
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing


/**
 * Custom exercise form. A full page from Exercises' FAB, the
 * picker's empty search, Exercise detail ⋮ Edit and Import's Create.
 *
 * Top bar with a `Save` text button, disabled until the name is valid; then Name, Type,
 * Equipment, Primary and Secondary muscles as cards — a soft field, a grid of options, and pill
 * chips that turn ink when chosen. The name must be
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
        containerColor = MaterialTheme.colorScheme.background,
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
                NameField(
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
                OptionGrid(
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
            { Text("${selected.size} chosen", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
 * Wrapped pill chips: soft when off, ink when on. [multi] toggles; otherwise one choice.
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        options.forEach { option ->
            val on = isSelected(option)
            val select = if (multi) {
                Modifier.toggleable(value = on, role = Role.Checkbox, onValueChange = { onTap(option) })
            } else {
                Modifier.selectable(selected = on, role = Role.RadioButton, onClick = { if (!on) onTap(option) })
            }
            val fill by animateColorAsState(if (on) scheme.primary else softFill(), label = "optFill")
            val ink by animateColorAsState(if (on) scheme.onPrimary else scheme.onSurface, label = "optInk")
            Box(
                Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(fill)
                    .then(select)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = Spacing.l, vertical = Spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = AppText.label, color = ink)
            }
        }
    }
}

/** The name field: a soft filled box, an ink ring on focus, the error ring and line when set. */
@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, error: String?) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = AppText.titleM,
        placeholder = { Text("Landmine press", style = AppText.titleM) },
        isError = error != null,
        supportingText = error?.let { { Text(it, style = AppText.bodyS) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
