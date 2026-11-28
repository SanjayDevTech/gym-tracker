package com.example.gymtracker.ui.screens.plans.templates

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.LayoutTemplate
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.components.cardRow
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.meshGradient
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtracker.data.plan.PlanTemplate
import com.example.gymtracker.data.plan.PlanTemplates
import com.example.gymtracker.data.plan.TemplateExercise
import com.example.gymtracker.data.plan.TemplatePlan
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Plan templates (§4.20) — reached from New plan's "Start from a template" and Plans & backup,
 * never prompted. One white card of rows — pastel disc, name, summary, chevron; a row opens the
 * template's detail.
 */
@Composable
fun TemplateListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = cardColor()
    Scaffold(
        topBar = { AppTopBar(title = "Plan templates", onBack = onBack) },
        containerColor = scheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxl),
        ) {
            item(key = "intro") {
                Text(
                    "Ready-made splits. Adding one copies its plans in for you to edit; " +
                        "your own plans aren't changed.",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.s, bottom = Spacing.l),
                )
            }
            itemsIndexed(PlanTemplates.all, key = { _, t -> t.id }) { i, template ->
                Column(Modifier.cardRow(i, PlanTemplates.all.size, fill)) {
                    if (i > 0) AppDivider(Modifier.padding(start = Spacing.l + 40.dp + Spacing.m))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressable { onOpen(template.id) }
                            .heightIn(min = 64.dp)
                            .padding(horizontal = Spacing.l, vertical = Spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PastelIcon(Lucide.LayoutTemplate, Pastel.entries[i % Pastel.entries.size])
                        Spacer(Modifier.width(Spacing.m))
                        Column(Modifier.weight(1f)) {
                            Text(template.name, style = AppText.titleM, color = scheme.onSurface)
                            Text(template.summary, style = AppText.bodyS, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(Spacing.s))
                        Icon(
                            Lucide.ChevronRight,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One template: its description, the A + B / A only choice when it has one, then every plan it
 * adds as a [Section] of exercises with targets. The pinned button adds the chosen plans and
 * hands their count to [onAdded].
 */
@Composable
fun TemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    onAdded: (count: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val template = remember(templateId) { PlanTemplates.find(templateId) }
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    var optionIndex by rememberSaveable { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(title = template?.name.orEmpty(), onBack = onBack) },
        bottomBar = {
            if (template != null) {
                val count = template.options[optionIndex].plans.size
                AddBar(
                    label = "Add $count ${plural(count, "plan")}",
                    enabled = !adding,
                    onAdd = {
                        haptics.light()
                        adding = true
                        scope.launch {
                            try {
                                container.planTemplates.add(template.options[optionIndex])
                                onAdded(count)
                            } finally {
                                adding = false
                            }
                        }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        if (template == null) return@Scaffold
        TemplateContent(
            template = template,
            optionIndex = optionIndex,
            onOption = { optionIndex = it },
            contentPadding = padding,
        )
    }
}

@Composable
private fun TemplateContent(
    template: PlanTemplate,
    optionIndex: Int,
    onOption: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val scheme = MaterialTheme.colorScheme
    val plans = template.options[optionIndex].plans
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        item(key = "head") {
            // The template on the soft mesh gradient: what it is, its choice of plans, and its size.
            Column(
                Modifier
                    .padding(top = Spacing.s)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.lg + 4.dp))
                    .meshGradient(animate = false)
                    .dotTexture(color = scheme.onSurface.copy(alpha = 0.08f))
                    .padding(20.dp),
            ) {
                Text(template.summary, style = AppText.eyebrow, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                Text(template.description, style = AppText.body, color = scheme.onSurface)
                if (template.options.size > 1) {
                    Spacer(Modifier.height(Spacing.l))
                    ExclusiveButtonGroup(
                        options = template.options.indices.toList(),
                        selected = optionIndex,
                        onSelect = onOption,
                        fillWidth = true,
                        trackColor = cardColor().copy(alpha = 0.62f),
                        label = { template.options[it].label },
                    )
                }
                Spacer(Modifier.height(Spacing.l))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    GlassStat("Plans", "${plans.size}", Modifier.weight(1f))
                    GlassStat("Exercises", "${plans.sumOf { it.exercises.size }}", Modifier.weight(1f))
                    GlassStat("Sets", "${plans.sumOf { p -> p.exercises.sumOf { it.sets } }}", Modifier.weight(1f))
                }
            }
        }
        items(plans, key = { "$optionIndex/${it.name}" }) { plan -> TemplatePlanSection(plan) }
    }
}

/** A plan as it will be added: head with its set count, note, then exercise · target rows. */
@Composable
private fun TemplatePlanSection(plan: TemplatePlan) {
    val scheme = MaterialTheme.colorScheme
    val sets = plan.exercises.sumOf { it.sets }
    Section(
        title = plan.name,
        trailing = { Text("$sets sets", style = AppText.bodyS, color = scheme.onSurfaceVariant) },
    ) {
        if (plan.notes.isNotEmpty()) {
            Text(plan.notes, style = AppText.bodyS, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.s))
        }
        plan.exercises.forEachIndexed { i, ex ->
            if (i > 0) AppDivider()
            ExerciseLine(ex)
        }
    }
}

@Composable
private fun ExerciseLine(exercise: TemplateExercise) {
    val scheme = MaterialTheme.colorScheme
    val reps = if (exercise.repsLow == exercise.repsHigh) "${exercise.repsLow}" else "${exercise.repsLow}–${exercise.repsHigh}"
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = AppText.body, color = scheme.onSurface)
            if (exercise.notes.isNotEmpty()) {
                Text(exercise.notes, style = AppText.bodyS, color = scheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(Spacing.m))
        Text("${exercise.sets} × $reps", style = AppText.label, color = scheme.onSurface)
    }
}

/** The pinned add action above the navigation inset, as the Plan editor's Start bar. */
@Composable
private fun AddBar(label: String, enabled: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.m),
    ) {
        AppButton(
            text = label,
            onClick = onAdd,
            enabled = enabled,
            icon = Lucide.Plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A frosted figure on the template's gradient. */
@Composable
private fun GlassStat(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(cardColor().copy(alpha = 0.62f))
            .padding(horizontal = Spacing.m, vertical = Spacing.s + 2.dp),
    ) {
        Text(label, style = AppText.eyebrow, color = scheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = AppText.numberM, color = scheme.onSurface, maxLines = 1)
    }
}
