package com.example.gymtracker.ui.screens.plans.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.ui.components.Sparkline
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.screens.plans.PlanCard
import com.example.gymtracker.ui.screens.plans.PlanStats
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/**
 * Plan row — design-plan §3 `PlanRow`. No card, tile, chips or trash (delete lives in the Plan
 * editor's ⋮, §4.1 change 2). Name + a primary `Start` text button; `Last · 3 days ago` with the
 * plan's volume sparkline (S2-a); `6 exercises · Chest, Shoulders · ~55 min`. Row tap opens the
 * Plan editor.
 */
@Composable
fun PlanRow(
    card: PlanCard,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = Spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                card.plan.name,
                style = AppText.titleM,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            AppTextButton(text = "Start", onClick = onStart, enabled = card.exerciseCount > 0)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Last · " + (card.lastTrainedEpochDay?.let(::agoLabel) ?: "not yet"),
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.m))
            Sparkline(card.recentVolumes)
        }
        Text(
            PlanStats.metaLine(card.exerciseCount, card.typicalDurationMillis, muscles = card.muscleGroups),
            style = AppText.bodyS,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** `today`, `yesterday`, `3 days ago`, `2 weeks ago`, `4 months ago`. */
internal fun agoLabel(epochDay: Long, today: Long = DateUtil.todayEpochDay()): String {
    val days = (today - epochDay).coerceAtLeast(0)
    fun ago(n: Long, unit: String) = "$n ${plural(n, unit)} ago"
    return when {
        days == 0L -> "today"
        days == 1L -> "yesterday"
        days < 14 -> ago(days, "day")
        days < 61 -> ago(days / 7, "week")
        days < 365 -> ago(days / 30, "month")
        else -> ago(days / 365, "year")
    }
}
