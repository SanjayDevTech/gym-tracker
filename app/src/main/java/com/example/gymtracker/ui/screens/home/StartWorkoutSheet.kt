package com.example.gymtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Zap
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.SheetTitleRow
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * The "+" beside the tab bar: what to start. A quick log on top, then every plan. (With a workout
 * already running, "+" resumes it instead and this sheet never opens.)
 */
@Composable
fun StartWorkoutSheet(
    onQuickLog: () -> Unit,
    onStartPlan: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val repository = LocalAppContainer.current.repository
    val plansFlow = remember(repository) { repository.observePlans() }
    val plans by plansFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    AppSheet(onDismiss = onDismiss) {
        SheetTitleRow("Start a workout", onClose = onDismiss)
        Spacer(Modifier.height(Spacing.m))
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            StartRow(Lucide.Zap, Pastel.Peach, "Quick log", "An empty workout; add exercises as you go", onQuickLog)
            if (plans.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.l))
                Text("Your plans", style = AppText.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.s))
            }
            plans.forEach { plan ->
                StartRow(Lucide.Dumbbell, Pastel.Lavender, plan.name, null) { onStartPlan(plan.id) }
                Spacer(Modifier.height(Spacing.s))
            }
        }
    }
}

@Composable
private fun StartRow(icon: ImageVector, pastel: Pastel, title: String, caption: String?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(cardColor())
            .pressable(onClick = onClick)
            .padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PastelIcon(icon, pastel)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.titleM, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) Text(caption, style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
