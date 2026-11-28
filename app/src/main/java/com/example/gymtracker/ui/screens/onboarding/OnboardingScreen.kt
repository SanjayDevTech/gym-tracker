package com.example.gymtracker.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Ban
import com.composables.icons.lucide.ChartNoAxesColumn
import com.composables.icons.lucide.CloudUpload
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.WifiOff
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.plan.PlanTemplates
import com.example.gymtracker.domain.WeeklyTarget
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.BodyDiagram
import com.example.gymtracker.ui.components.BodyView
import com.example.gymtracker.ui.components.ExclusiveButtonGroup
import com.example.gymtracker.ui.components.OptionList
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTextField
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.Stepper
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.components.atoms.heatRamp
import com.example.gymtracker.ui.components.atoms.staggerIn
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.meshGradient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Two intro pages, then three setup steps — Foodprint's onboarding shape. */
private const val IntroCount = 2
private const val SetupSteps = 3
private const val PageCount = IntroCount + SetupSteps

/** Ready-made splits offered on the last step, most broadly useful first. */
private val StarterTemplates = listOf("full-body", "upper-lower", "ppl", "5x5", "dumbbells", "minimal")
    .mapNotNull { PlanTemplates.find(it) }

private val RestPresets = listOf(60, 90, 120, 180)

/**
 * First run: what the app does, how it treats your data, then your name and units, your week
 * (target and rest), and optionally a starter plan. Everything here can be changed later in You.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { PageCount })

    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(WeightUnit.Kg) }
    var target by rememberSaveable { mutableIntStateOf(WeeklyTarget.DEFAULT_TARGET) }
    var rest by rememberSaveable { mutableIntStateOf(90) }
    var templateId by rememberSaveable { mutableStateOf<String?>(StarterTemplates.firstOrNull()?.id) }
    var finishing by remember { mutableStateOf(false) }

    // Start from what is already saved, so someone updating from an older version keeps their
    // units, target and rest, and isn't handed a starter plan on top of their own.
    var seeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (seeded) return@LaunchedEffect // a rotation keeps what was typed
        val prefs = container.prefs
        name = prefs.userName.first()
        unit = prefs.weightUnit.first()
        target = prefs.weeklyTargetSessions.first()
        rest = prefs.defaultRestSeconds.first()
        if (container.repository.observePlans().first().isNotEmpty()) templateId = null
        seeded = true
    }

    fun finish() {
        if (finishing) return
        finishing = true
        haptics.confirm()
        scope.launch {
            val prefs = container.prefs
            prefs.setUserName(name)
            prefs.setWeightUnit(unit)
            prefs.setWeeklyTargetSessions(target)
            prefs.setDefaultRestSeconds(rest)
            templateId?.let(PlanTemplates::find)?.options?.firstOrNull()?.let { container.planTemplates.add(it) }
            prefs.setOnboarded()
            onDone()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ProgressHeader(
            page = pager.currentPage,
            onSkip = if (pager.currentPage < IntroCount) {
                { scope.launch { pager.animateScrollToPage(IntroCount) } }
            } else null,
        )
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PrivacyPage()
                2 -> PageScaffold(1, "About you", "So Lift Up can greet you and show your weights the way you think in them.") {
                    Section("Name") {
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("First name (optional)", style = AppText.body) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(Spacing.section))
                    Section("Weights in") {
                        ExclusiveButtonGroup(
                            options = WeightUnit.entries.toList(),
                            selected = unit,
                            onSelect = { unit = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { it.label },
                        )
                    }
                }
                3 -> PageScaffold(2, "Your week", "A week on target is what the streak counts. Pick a number you can keep.") {
                    Section(
                        "Weekly target",
                        trailing = { Text("${plural(target, "session")} a week", style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    ) {
                        Stepper(
                            value = target,
                            onValueChange = { target = it },
                            range = WeeklyTarget.MIN_TARGET..WeeklyTarget.MAX_TARGET,
                            decrementLabel = "Fewer sessions",
                            incrementLabel = "More sessions",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                    Spacer(Modifier.height(Spacing.section))
                    Section("Rest between sets", trailing = { Text(formatClock(rest * 1000L), style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) }) {
                        ExclusiveButtonGroup(
                            options = (RestPresets + rest).distinct().sorted(),
                            selected = rest,
                            onSelect = { rest = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { formatClock(it * 1000L) },
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Text(
                            "The timer starts on its own when you tick a set. Change it per workout any time.",
                            style = AppText.bodyS,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                4 -> PageScaffold(3, "A plan to start", "Add a ready-made split as your own plans, or build yours from scratch.", optional = true) {
                    OptionList(
                        options = StarterTemplates.map { it.id } + listOf<String?>(null),
                        selected = templateId,
                        onSelect = { templateId = it },
                        caption = { id -> id?.let(PlanTemplates::find)?.summary ?: "Start empty; Quick log works right away" },
                        label = { id -> id?.let(PlanTemplates::find)?.name ?: "I'll build my own" },
                    )
                }
            }
        }
        BottomNav(
            page = pager.currentPage,
            enabled = !finishing,
            onBack = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onContinue = {
                if (pager.currentPage < PageCount - 1) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    finish()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = Spacing.l),
        )
    }
}

// ───────────────────────── Intro pages ─────────────────────────

@Composable
private fun WelcomePage() {
    IntroPage(
        art = { WelcomeArt() },
        title = "Train well.\nSee it clearly.",
        subtitle = "A training log that shows what your weeks really add up to.",
        points = listOf(
            IntroPoint(Lucide.Dumbbell, Rich.Volume, "Log in a tap", "Plans, a live set table and a rest timer that starts itself."),
            IntroPoint(Lucide.ChartNoAxesColumn, Rich.Muscles, "See every muscle", "A body map lights up what you trained this week, front and back."),
            IntroPoint(Lucide.Flame, Rich.Streak, "Build the habit", "A weekly target, a streak of weeks on it, and records as you set them."),
        ),
    )
}

@Composable
private fun PrivacyPage() {
    IntroPage(
        art = { PrivacyArt() },
        title = "Private by design.",
        subtitle = "Your training lives on this phone. There is no account to make.",
        points = listOf(
            IntroPoint(Lucide.Smartphone, Rich.Rest, "Stays on your phone", "Plans, sessions and records are stored only on this device. No ads, no analytics."),
            IntroPoint(Lucide.WifiOff, Rich.Sets, "Works offline", "Nothing in Lift Up needs the internet, in the gym or anywhere else."),
            IntroPoint(Lucide.CloudUpload, Rich.Sessions, "Your backups, your call", "Export plans or a full backup as readable JSON, whenever you choose."),
        ),
    )
}

private data class IntroPoint(val icon: ImageVector, val rich: Rich, val title: String, val body: String)

@Composable
private fun IntroPage(art: @Composable () -> Unit, title: String, subtitle: String, points: List<IntroPoint>) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
    ) {
        Box(Modifier.staggerIn(0)) { art() }
        Spacer(Modifier.height(Spacing.xxl))
        Text(title, style = AppText.headline, color = scheme.onSurface, modifier = Modifier.staggerIn(1))
        Spacer(Modifier.height(Spacing.s))
        Text(subtitle, style = AppText.body, color = scheme.onSurfaceVariant, modifier = Modifier.staggerIn(1))
        Spacer(Modifier.height(Spacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            points.forEachIndexed { i, p ->
                Row(Modifier.staggerIn(2 + i), verticalAlignment = Alignment.Top) {
                    RichDisc(p.icon, p.rich, 44.dp)
                    Spacer(Modifier.width(Spacing.m))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = AppText.titleM, color = scheme.onSurface)
                        Text(p.body, style = AppText.bodyS, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.l))
    }
}

/** An icon on a rich-gradient circle. */
@Composable
private fun RichDisc(icon: ImageVector, rich: Rich, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(rich.brush).dotTexture(color = rich.ink.copy(alpha = 0.10f), spacing = 5.dp),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = rich.ink, modifier = Modifier.size(size * 0.46f)) }
}

/** A gentle bob, phase-shifted per element so the tiles don't move in step. */
@Composable
private fun floatOffset(phase: Float): Float {
    val t by rememberInfiniteTransition(label = "float").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart), label = "floatT",
    )
    return sin((t + phase) * 2 * PI).toFloat()
}

/** A tiny, live-looking Home: a lit-up body map, the week's sessions and the streak. */
@Composable
private fun WelcomeArt() {
    val ramp = heatRamp()
    val fills = remember(ramp) {
        mapOf(
            Muscle.Chest to 1f, Muscle.FrontDelts to 0.8f, Muscle.SideDelts to 0.5f, Muscle.Triceps to 0.6f,
            Muscle.Quads to 0.9f, Muscle.Abs to 0.3f, Muscle.Biceps to 0.4f, Muscle.Adductors to 0.4f,
        ).mapValues { heatColor(0.25f + 0.75f * it.value, ramp) }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient()
            .dotTexture(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val a = floatOffset(0f)
            MiniTile(Rich.Muscles, Modifier.weight(1f).fillMaxHeight().graphicsLayer { translationY = a * 6.dp.toPx() }) {
                MiniHead(Lucide.Dumbbell, "This week", Rich.Muscles)
                Spacer(Modifier.height(Spacing.s))
                BodyDiagram(BodyView.Front, fills, Modifier.weight(1f).align(Alignment.CenterHorizontally))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                val b = floatOffset(0.33f)
                MiniTile(Rich.Sessions, Modifier.weight(1f).fillMaxWidth().graphicsLayer { translationY = b * 5.dp.toPx() }) {
                    MiniHead(Lucide.Timer, "Sessions", Rich.Sessions)
                    Spacer(Modifier.weight(1f))
                    Text("3 of 4", style = AppText.numberM, color = MaterialTheme.colorScheme.onSurface)
                }
                val c = floatOffset(0.66f)
                MiniTile(Rich.Streak, Modifier.weight(1f).fillMaxWidth().graphicsLayer { translationY = c * 5.dp.toPx() }) {
                    MiniHead(Lucide.Flame, "Streak", Rich.Streak)
                    Spacer(Modifier.weight(1f))
                    Text("6 weeks", style = AppText.numberM, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MiniTile(rich: Rich, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(Radius.md + 2.dp)).background(cardColor())
            .dotTexture(color = rich.ink.copy(alpha = 0.22f)).padding(Spacing.m),
        content = content,
    )
}

@Composable
private fun MiniHead(icon: ImageVector, label: String, rich: Rich) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = rich.ink, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(Spacing.xs + 2.dp))
        Text(label, style = AppText.label, color = rich.ink)
    }
}

/** A shield on a gradient, with the three things the app never does floating round it. */
@Composable
private fun PrivacyArt() {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(Radius.lg + 4.dp))
            .meshGradient()
            .dotTexture(color = scheme.onSurface.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        val a = floatOffset(0f)
        Box(Modifier.graphicsLayer { translationY = a * 6.dp.toPx() }) {
            RichDisc(Lucide.ShieldCheck, Rich.Body, 96.dp)
        }
        GlassChip("No account", Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 36.dp), 0.2f)
        GlassChip("No ads", Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 64.dp), 0.5f)
        GlassChip("No tracking", Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp), 0.8f)
    }
}

@Composable
private fun GlassChip(text: String, modifier: Modifier, phase: Float) {
    val f = floatOffset(phase)
    Row(
        modifier
            .graphicsLayer { translationY = f * 4.dp.toPx() }
            .clip(RoundedCornerShape(Radius.pill))
            .background(cardColor().copy(alpha = 0.8f))
            .padding(horizontal = Spacing.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Ban, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(Spacing.xs + 2.dp))
        Text(text, style = AppText.label, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ───────────────────────── Page chrome ─────────────────────────

/** Step eyebrow, title, one-line subtitle, and a scrolling body. */
@Composable
private fun PageScaffold(
    step: Int,
    title: String,
    subtitle: String,
    optional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
    ) {
        Text(
            if (optional) "Step $step of $SetupSteps · Optional" else "Step $step of $SetupSteps",
            style = AppText.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(title, style = AppText.headline, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.xs))
        Text(subtitle, style = AppText.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xl))
        content()
        Spacer(Modifier.height(Spacing.xxl))
    }
}

/** Back on the left, the one forward action on the right. */
@Composable
private fun BottomNav(
    page: Int,
    enabled: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (page > 0) AppTextButton(text = "Back", onClick = onBack) else Spacer(Modifier.width(1.dp))
        Spacer(Modifier.weight(1f))
        AppButton(
            text = when {
                page == PageCount - 1 -> "Start training"
                page == 0 -> "Get started"
                page < IntroCount -> "Sounds good"
                else -> "Continue"
            },
            onClick = onContinue,
            enabled = enabled,
        )
    }
}

/** Five short bars across the top — the done ones ink — and Skip on the intro pages. */
@Composable
private fun ProgressHeader(page: Int, onSkip: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.s)
            .heightIn(min = Sizes.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(PageCount) { i ->
                val fill by animateColorAsState(
                    if (i <= page) scheme.primary else scheme.surfaceContainerHighest,
                    label = "step$i",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(fill),
                )
            }
        }
        Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
            if (onSkip != null) AppTextButton("Skip", onSkip)
        }
    }
}
