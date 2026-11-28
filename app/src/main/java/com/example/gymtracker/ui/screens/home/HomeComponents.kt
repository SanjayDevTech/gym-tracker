package com.example.gymtracker.ui.screens.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gymtracker.ui.components.RichTile
import com.example.gymtracker.ui.components.tileInk
import com.example.gymtracker.ui.screens.plans.HEAT_WEEKS
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.LayoutTemplate
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Zap
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.displayName
import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.ui.components.AppSwitcher
import com.example.gymtracker.ui.components.BodyMap
import com.example.gymtracker.ui.components.Sparkline
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AnimatedCount
import com.example.gymtracker.ui.components.atoms.Meter
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.components.atoms.heatRamp
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.components.heatFill
import com.example.gymtracker.ui.format.EM_DASH
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.formatDuration
import com.example.gymtracker.ui.format.formatVolume
import com.example.gymtracker.ui.format.formatVolumeNumber
import com.example.gymtracker.ui.format.grouped
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.screens.plans.LiveSession
import com.example.gymtracker.ui.screens.plans.PlanCard
import com.example.gymtracker.ui.screens.plans.RecentSession
import com.example.gymtracker.ui.screens.plans.WeekMuscles
import com.example.gymtracker.ui.screens.plans.WeekSummary
import com.example.gymtracker.ui.screens.plans.components.agoLabel
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.meshGradient
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import java.time.LocalTime

private val DAY_INITIALS = listOf("M", "T", "W", "T", "F", "S", "S")

/** Greeting and the page title on the left; the Lift | Food switch on the right. */
@Composable
fun HomeHeader(name: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    } + if (name.isNotBlank()) ", ${name.trim().substringBefore(' ')}" else ""
    Row(
        modifier
            .fillMaxWidth()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.m, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = AppText.bodyS, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Today", style = AppText.headline, color = scheme.onSurface, maxLines = 1)
        }
        AppSwitcher()
    }
}

/** The workout in progress: its name, a live clock, sets done, and Resume. */
@Composable
fun LiveCard(live: LiveSession, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Rich.Sessions.brush)
            .dotTexture(color = Rich.Sessions.ink.copy(alpha = 0.12f))
            .padding(Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val paused = live.pausedAtMillis > 0L
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (paused) Rich.Duration.ink else scheme.secondary))
            Spacer(Modifier.width(Spacing.s))
            Text(if (paused) "Paused" else "In progress", style = AppText.eyebrow, color = Rich.Sessions.ink, modifier = Modifier.weight(1f))
            Text(
                formatClock((if (paused) live.pausedAtMillis else now) - live.startedAtMillis),
                style = AppText.numberM,
                color = scheme.onSurface,
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Text(live.title, style = AppText.titleL, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(Spacing.m))
        Meter(
            fraction = if (live.totalSets > 0) live.doneSets.toFloat() / live.totalSets else 0f,
            color = Rich.Sessions.ink,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "${live.doneSets} of ${live.totalSets} ${plural(live.totalSets, "set")} done",
            style = AppText.bodyS,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.l))
        AppButton("Resume workout", onClick = onResume, icon = Lucide.Play, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The week on a soft gradient card: sessions against the weekly target as one big counting
 * number, working sets per day as seven bars (today in the accent), and three figures along the
 * bottom. The colour field and content drift with the scroll, like Foodprint's calorie hero.
 */
@Composable
fun WeekHero(
    week: WeekSummary,
    setsByDay: List<Int>,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
    parallax: () -> Float = { 0f },
) {
    val scheme = MaterialTheme.colorScheme
    val pct = if (week.target > 0) (week.sessions * 100 / week.target) else 0
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient(animate = false, parallax = parallax)
            .dotTexture(color = scheme.onSurface.copy(alpha = 0.10f), parallax = parallax)
            .padding(20.dp)
            .graphicsLayer {
                val p = parallax()
                val progress = (p / size.height).coerceIn(0f, 1f)
                translationY = p * 0.4f
                scaleX = 1f - 0.08f * progress
                scaleY = 1f - 0.08f * progress
                alpha = (1f - progress * 1.4f).coerceIn(0f, 1f)
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("This week", style = AppText.eyebrow, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            // Weeks in a row on target, up top where it is seen first.
            Row(
                Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(cardColor().copy(alpha = 0.75f))
                    .padding(horizontal = Spacing.m, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.Flame, contentDescription = null, tint = Rich.Streak.ink, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "${week.streakWeeks} wk",
                    style = AppText.label,
                    color = scheme.onSurface,
                    modifier = Modifier.semantics { contentDescription = "${week.streakWeeks} weeks on target in a row" },
                )
            }
            Spacer(Modifier.width(Spacing.s))
            Text(
                "$pct% of target",
                style = AppText.label,
                color = scheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(cardColor().copy(alpha = 0.75f))
                    .padding(horizontal = Spacing.m, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedCount(value = week.sessions, style = AppText.numberXXL, color = scheme.onSurface)
            Text(
                " of ${week.target} ${plural(week.target, "session")}",
                style = AppText.titleM,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(Spacing.l))
        DayBars(setsByDay = setsByDay, trainedDays = week.trainedDays, todayIndex = week.todayIndex)
        Spacer(Modifier.height(Spacing.l))
        Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.onSurface.copy(alpha = 0.08f)))
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            HeroStat("Volume · 7d", formatVolumeNumber(week.volumeKg7d, unit), unit.label, Modifier.weight(1f))
            HeroDivider()
            HeroStat("Sets · 7d", grouped(week.sets7d), "", Modifier.weight(1f))
            HeroDivider()
            HeroStat(
                "On target",
                if (week.onTarget.of == 0) EM_DASH else "${week.onTarget.met}",
                if (week.onTarget.of == 0) "" else "of ${week.onTarget.of} wk",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.padding(horizontal = Spacing.s)) {
        Text(label, style = AppText.eyebrow, color = scheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppText.numberM, color = scheme.onSurface, maxLines = 1)
            if (unit.isNotEmpty()) {
                Text(" $unit", style = AppText.bodyS, color = scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HeroDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)))
}

/** Seven rounded bars of working sets; days after today are stubs. Bars grow on first show. */
@Composable
private fun DayBars(setsByDay: List<Int>, trainedDays: Set<Int>, todayIndex: Int) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.tertiary
    val ink = scheme.onSurface.copy(alpha = 0.72f)
    val stub = scheme.onSurface.copy(alpha = 0.12f)
    val scale = maxOf(setsByDay.maxOrNull() ?: 0, 12).toFloat()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(setsByDay) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    Column(
        Modifier.semantics {
            contentDescription = "Working sets this week: " +
                setsByDay.mapIndexed { i, v -> "${DAY_INITIALS[i]} $v" }.joinToString()
        },
    ) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val slot = size.width / 7f
            val barW = slot * 0.46f
            val r = CornerRadius(barW / 2.4f, barW / 2.4f)
            setsByDay.forEachIndexed { i, v ->
                val x = slot * i + (slot - barW) / 2f
                val trained = v > 0 || i in trainedDays
                val h = if (!trained || i > todayIndex) 4.dp.toPx()
                else (size.height * (v / scale) * grow.value).coerceAtLeast(8.dp.toPx())
                drawRoundRect(
                    color = when {
                        !trained || i > todayIndex -> stub
                        i == todayIndex -> accent
                        else -> ink
                    },
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = r,
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        Row(Modifier.fillMaxWidth()) {
            DAY_INITIALS.forEachIndexed { i, d ->
                Text(
                    d,
                    style = if (i == todayIndex) AppText.eyebrow.copy(fontWeight = FontWeight.Bold) else AppText.eyebrow,
                    color = if (i == todayIndex) scheme.onSurface else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * This week's muscles on the body map — front and back — with the three most-trained as chips and
 * a ramp legend. Tapping the card opens Progress for the full breakdown.
 */
@Composable
fun MuscleMapCard(muscles: WeekMuscles, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ramp = heatRamp()
    val fills = remember(muscles.levels, ramp) { heatFill(muscles.levels, ramp) }
    Section(
        title = "Muscles this week",
        modifier = modifier
            .semantics(mergeDescendants = false) { role = Role.Button }
            .pressable(onClick = onOpen),
        trailing = {
            Text(
                if (muscles.ranked.isEmpty()) "None yet" else "${muscles.ranked.size} trained",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
            Icon(Lucide.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        },
    ) {
        BodyMap(fills = fills, modifier = Modifier.padding(horizontal = Spacing.xl))
        Spacer(Modifier.height(Spacing.l))
        if (muscles.ranked.isEmpty()) {
            Text(
                "Log a workout and the muscles it works light up here.",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        } else {
            // The three most-trained, as rows: a dot in the muscle's heat, its name, its sets.
            val top = muscles.ranked.take(3)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(softFill())
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs),
            ) {
                top.forEach { (muscle, sets) ->
                    val level = muscles.levels[muscle] ?: 0f
                    val shown = sets.roundToInt()
                    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(heatColor(0.25f + 0.75f * level, ramp)))
                        Spacer(Modifier.width(Spacing.m))
                        Text(muscle.displayName, style = AppText.body, color = scheme.onSurface, modifier = Modifier.weight(1f))
                        Text("$shown", style = AppText.numberM, color = scheme.onSurface)
                        Text(" ${plural(shown, "set")}", style = AppText.bodyS, color = scheme.onSurfaceVariant)
                    }
                }
            }
            if (muscles.ranked.size > top.size) {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    "+ ${muscles.ranked.size - top.size} more · tap for the full breakdown",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.s))
            ramp.forEach {
                Box(Modifier.padding(end = 3.dp).size(width = 18.dp, height = 8.dp).clip(RoundedCornerShape(Radius.pill)).background(it))
            }
            Spacer(Modifier.width(Spacing.s - 3.dp))
            Text("More", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
        }
    }
}

/** Plans as rows with a round Start each; Quick log, New plan and Templates underneath. */
@Composable
fun PlansCard(
    plans: List<PlanCard>,
    onOpenPlan: (Long) -> Unit,
    onStartPlan: (Long) -> Unit,
    onQuickLog: () -> Unit,
    onNewPlan: () -> Unit,
    onTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Section(
        title = "Your plans",
        modifier = modifier,
        trailing = {
            if (plans.isNotEmpty()) {
                Text("${plans.size} ${plural(plans.size, "plan")}", style = AppText.bodyS, color = scheme.onSurfaceVariant)
            }
        },
    ) {
        if (plans.isEmpty()) {
            Text(
                "Build a plan, or pick one of the ready-made splits.",
                style = AppText.body,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.m))
        }
        plans.forEachIndexed { i, card ->
            if (i > 0) AppDivider()
            PlanLine(card = card, onOpen = { onOpenPlan(card.plan.id) }, onStart = { onStartPlan(card.plan.id) })
        }
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            AppOutlinedButton("New plan", onClick = onNewPlan, icon = Lucide.Plus, modifier = Modifier.weight(1f))
            AppOutlinedButton("Templates", onClick = onTemplates, icon = Lucide.LayoutTemplate, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.s))
        AppOutlinedButton("Quick log", onClick = onQuickLog, icon = Lucide.Zap, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PlanLine(card: PlanCard, onOpen: () -> Unit, onStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(onClick = onOpen)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PastelIcon(Lucide.Dumbbell, Pastel.Peach)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(card.plan.name, style = AppText.titleM, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val groups = card.muscleGroups.take(3).joinToString(" · ") { it.label }
            Text(
                listOfNotNull(
                    "${card.exerciseCount} ${plural(card.exerciseCount, "exercise")}",
                    groups.ifBlank { null },
                ).joinToString(" · "),
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Last · " + (card.lastTrainedEpochDay?.let { agoLabel(it) } ?: "not yet"),
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        }
        // The volume trend of its last few sessions, once there are two to compare.
        if (card.recentVolumes.size >= 2) {
            Spacer(Modifier.width(Spacing.s))
            Sparkline(card.recentVolumes)
        }
        Spacer(Modifier.width(Spacing.s))
        StartButton(enabled = card.exerciseCount > 0, label = "Start ${card.plan.name}", onClick = onStart)
    }
}

/** A 40dp ink disc with a play mark — the one-tap Start on a plan row. */
@Composable
private fun StartButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(48.dp)
            .semantics { role = Role.Button; contentDescription = label }
            .pressable(enabled = enabled, scaleTo = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (enabled) scheme.primary else softFill()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Lucide.Play,
                contentDescription = null,
                tint = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The last three sessions; tap one for its detail, or see them all. */
@Composable
fun RecentCard(
    recent: List<RecentSession>,
    unit: WeightUnit,
    onOpenSession: (Long) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val today = DateUtil.todayEpochDay()
    Section(title = "Recent", modifier = modifier) {
        if (recent.isEmpty()) {
            Text("Your finished workouts show up here.", style = AppText.body, color = scheme.onSurfaceVariant)
            return@Section
        }
        recent.forEachIndexed { i, r ->
            if (i > 0) AppDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable { onOpenSession(r.sessionId) }
                    .padding(vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayDisc(r.dateEpochDay)
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = AppText.titleM, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(
                            agoLabel(r.dateEpochDay, today).replaceFirstChar { it.uppercase() },
                            r.durationMillis?.let { formatDuration(it) },
                            "${r.doneSets} ${plural(r.doneSets, "set")}",
                        ).joinToString(" · "),
                        style = AppText.bodyS,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(formatVolume(r.volumeKg, unit), style = AppText.numberM, color = scheme.onSurface)
            }
        }
        Spacer(Modifier.height(Spacing.s))
        AppOutlinedButton("See all sessions", onClick = onSeeAll, modifier = Modifier.fillMaxWidth())
    }
}

/** A small square with the weekday and date, on the sessions hue — set tight, like a date stamp. */
@Composable
private fun DayDisc(epochDay: Long) {
    val date = java.time.LocalDate.ofEpochDay(epochDay)
    // Trimmed line boxes, so the two lines sit together instead of floating apart.
    val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    Column(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(Pastel.Mint.fill),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
            style = AppText.eyebrow.copy(lineHeightStyle = tight),
            color = Pastel.Mint.ink,
        )
        Text("${date.dayOfMonth}", style = AppText.numberM.copy(lineHeightStyle = tight), color = Pastel.Mint.ink)
    }
}

/**
 * The streak, wide and featured: weeks in a row on target, the best run, and this week as seven
 * discs — filled with a dumbbell for a day you trained, ringed for today.
 */
@Composable
fun StreakTile(week: WeekSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    RichTile(Rich.Streak, modifier.fillMaxWidth(), onClick = onClick, featured = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(tileInk().copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Lucide.Flame, contentDescription = null, tint = tileInk(), modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${week.streakWeeks}", style = AppText.numberXL, color = scheme.onSurface)
                    Text(
                        " week streak",
                        style = AppText.titleM,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                val left = week.target - week.sessions
                Text(
                    when {
                        left > 0 -> "$left more ${plural(left, "session")} this week to keep it going · best ${week.bestRunWeeks}"
                        else -> "This week's target is met · best ${week.bestRunWeeks} ${plural(week.bestRunWeeks, "week")}"
                    },
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DAY_INITIALS.forEachIndexed { i, d ->
                val trained = i in week.trainedDays
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (trained) tileInk() else tileInk().copy(alpha = 0.12f))
                            .then(if (i == week.todayIndex) Modifier.border(2.dp, tileInk(), CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (trained) Icon(Lucide.Dumbbell, contentDescription = null, tint = cardColor(), modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        d,
                        style = AppText.eyebrow,
                        color = if (i == week.todayIndex) scheme.onSurface else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * "Your rhythm": the last [HEAT_WEEKS] weeks as a heatmap, one column per week, each day shaded
 * by the sets you did on the body map's ramp; today wears a ring. Tapping opens History.
 */
@Composable
fun RhythmHeatmap(start: Long, sets: List<Int>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val today = DateUtil.todayEpochDay()
    val ramp = heatRamp()
    val trained = sets.count { it > 0 }
    val ring = scheme.onSurface
    Section(
        "Your rhythm",
        modifier.pressable(onClick = onClick),
        trailing = {
            Text("$trained ${plural(trained, "day")} in $HEAT_WEEKS weeks", style = AppText.bodyS, color = scheme.onSurfaceVariant)
        },
    ) {
        // Month names over the first week of each month, skipping ones that would collide.
        val labels = remember(start) {
            val out = mutableListOf<Pair<Int, String>>()
            var lastMonth = -1
            for (w in 0 until HEAT_WEEKS) {
                val date = java.time.LocalDate.ofEpochDay(start + w * 7L)
                if (date.monthValue != lastMonth) {
                    val name = date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                    if (out.isEmpty() || w - out.last().first >= 3) out += w to name
                    else if (out.last().first == 0) out[out.lastIndex] = w to name
                    lastMonth = date.monthValue
                }
            }
            out
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 18.dp).height(16.dp)) {
            val colW = maxWidth / HEAT_WEEKS
            labels.forEach { (w, name) ->
                Text(
                    name,
                    style = AppText.eyebrow,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = colW * w),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Row {
            Column(Modifier.width(18.dp).height(HeatHeight), verticalArrangement = Arrangement.SpaceBetween) {
                listOf("M", "", "W", "", "F", "", "S").forEach {
                    Text(it, style = AppText.eyebrow, color = scheme.onSurfaceVariant)
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(HeatHeight)
                    .semantics { contentDescription = "$trained training days in the last $HEAT_WEEKS weeks" },
            ) {
                val cw = size.width / HEAT_WEEKS
                val ch = size.height / 7f
                val gap = 3.dp.toPx()
                val cell = Size(cw - gap, ch - gap)
                val r = CornerRadius(cell.minDimension * 0.32f)
                for (i in 0 until HEAT_WEEKS * 7) {
                    val day = start + i
                    val tl = Offset((i / 7) * cw, (i % 7) * ch)
                    if (day > today) {
                        // The rest of this week: present but quieter, so the grid is always whole.
                        drawRoundRect(ramp.first(), tl, cell, r, alpha = 0.45f)
                        continue
                    }
                    val n = sets.getOrElse(i) { 0 }
                    drawRoundRect(heatColor(heatLevel(n), ramp), tl, cell, r)
                    if (day == today) drawRoundRect(ring, tl, cell, r, style = Stroke(1.5.dp.toPx()))
                }
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rest", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.xs))
            listOf(0, 6, 14, 22).forEach { n ->
                Box(Modifier.padding(horizontal = 1.5.dp).size(11.dp).clip(RoundedCornerShape(3.dp)).background(heatColor(heatLevel(n), ramp)))
            }
            Spacer(Modifier.width(Spacing.xs))
            Text("More sets", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
        }
    }
}

/** Sets in a day → a level on the heat ramp: none, light (<10), solid (<20), big day. */
private fun heatLevel(sets: Int): Float = when {
    sets <= 0 -> 0f
    sets < 10 -> 0.4f
    sets < 20 -> 0.7f
    else -> 1f
}

private val HeatHeight = 132.dp

/** The page's sign-off: one big, faint line and a small note under it. */
@Composable
fun ClosingQuote(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(top = Spacing.xxl, start = Spacing.xs)) {
        Text(
            "Consistency\nbeats intensity.",
            style = AppText.quote,
            color = scheme.onSurface.copy(alpha = 0.16f),
        )
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Made with", style = AppText.bodyS, color = scheme.onSurfaceVariant)
            Icon(
                Lucide.Heart,
                contentDescription = "love",
                tint = Rich.Records.ink,
                modifier = Modifier.padding(horizontal = Spacing.xs).size(14.dp),
            )
            Text("for every rep", style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
    }
}
