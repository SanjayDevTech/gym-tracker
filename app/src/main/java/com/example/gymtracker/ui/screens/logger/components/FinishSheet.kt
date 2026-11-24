package com.example.gymtracker.ui.screens.logger.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.Spinner
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * Finish sheet — §4.5. Merges the old "Save changes to plan?" and "Finish this session?" sheets
 * into one decision: stats, the skipped count (decision 8), and — only when the structure
 * changed — an unchecked "Update plan" checkbox with the stated diff.
 */
@Composable
fun FinishSheet(
    title: String,
    /** Read inside the sheet, so the per-second tick doesn't recompose the logger screen. */
    elapsed: () -> String,
    doneSets: Int,
    totalSets: Int,
    prCount: Int,
    planName: String?,
    planChanges: List<String>,
    saving: Boolean,
    onFinish: (updatePlan: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var updatePlan by remember { mutableStateOf(false) }
    val skipped = totalSets - doneSets

    AppSheet(onDismiss = { if (!saving) onDismiss() }) {
        Text(title, style = AppText.titleL, color = scheme.onSurface)
        Text(
            buildString {
                append("${elapsed()} · $doneSets of $totalSets sets")
                if (prCount > 0) append(" · $prCount ${plural(prCount, "PR")}")
            },
            style = AppText.bodyS,
            color = scheme.onSurfaceVariant,
        )
        if (skipped > 0) {
            Text(
                "$skipped unticked ${plural(skipped, "set")} will be saved as skipped",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        }
        if (planChanges.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.m))
            AppDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.touchTarget)
                    .padding(vertical = Spacing.s)
                    .toggleable(value = updatePlan, enabled = !saving, role = Role.Checkbox) { updatePlan = it },
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = updatePlan,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = scheme.primary, checkmarkColor = scheme.onPrimary),
                )
                Column(Modifier.padding(start = Spacing.m)) {
                    Text(
                        "Update ${planName ?: "the plan"} to match this session",
                        style = AppText.body,
                        color = scheme.onSurface,
                    )
                    Text(planChanges.joinToString(" · "), style = AppText.bodyS, color = scheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(Spacing.m))
        AppDivider()
        Spacer(Modifier.height(Spacing.m))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            AppTextButton(
                text = "Keep logging",
                onClick = onDismiss,
                enabled = !saving,
                color = scheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (saving) Spinner()
            AppButton(text = "Finish", onClick = { onFinish(updatePlan) }, enabled = !saving)
        }
    }
}
