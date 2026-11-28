package com.example.gymtracker.ui.screens.logger.components

import com.example.gymtracker.ui.components.atoms.AppSwitch
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Pause
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Check
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.example.gymtracker.ui.components.atoms.LocalBehind
import com.example.gymtracker.ui.components.atoms.RestRing
import com.example.gymtracker.ui.components.atoms.RestTransport
import com.example.gymtracker.ui.components.atoms.RoundIconButton
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.components.atoms.softFill
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.format.formatClock
import com.example.gymtracker.ui.screens.logger.restClockLabel
import com.example.gymtracker.ui.screens.logger.restReached
import com.example.gymtracker.ui.screens.logger.restRemainingFraction
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Rich
import com.example.gymtracker.ui.theme.Spacing
import com.example.gymtracker.ui.theme.dotTexture
import com.example.gymtracker.ui.theme.meshGradient

/** The floating dock's shadow: the app's tab bar, so the two read as one family. */
private val DockShadow = Color.Black.copy(alpha = 0.22f)

/** An inline confirmation the dock's bottom row turns into — no sheet. */
sealed interface DockConfirm {
    /** Finish: what will be saved, and the plan-sync switch when the plan's structure changed. */
    data class Finish(
        val title: String,
        val caption: String,
        val planName: String?,
        val planChanges: List<String>,
        val saving: Boolean,
    ) : DockConfirm

    data class Discard(val caption: String) : DockConfirm
}

/**
 * The logger's workout dock: one floating card at the bottom, under the thumb, holding every
 * control the workout needs — no sheets.
 *
 * - **Rest row:** the rest ring with the target and `−15` / `+15` always to hand. Idle, tap the
 *   ring to start a rest; resting, the ring drains (`1:12`, then `+0:05` past the target, when it
 *   turns green and the dock pulses once), `Skip` joins the keys, and tapping the ring opens the
 *   full-screen [RestFocus].
 * - **Workout row:** Pause / Resume, the workout clock with sets done, and Finish (or Discard
 *   while nothing is logged). Finish and Discard confirm in place: the row turns into the
 *   question with its two answers, plus the plan-sync switch when the plan changed.
 *
 * [compact] shrinks it to one slim line while the keyboard is open, so the focused set row is
 * never hidden. [elapsed] and [restElapsed] are read here, not by the caller, so the per-second
 * tick recomposes the dock only. The caller owns insets: this must be the screen's only IME
 * consumer; it keeps the card above [insets] and swallows taps in the margins round it.
 */
@Composable
fun LoggerDock(
    elapsed: () -> String,
    restElapsed: () -> Long?,
    targetSeconds: Int,
    doneSets: Int,
    totalSets: Int,
    enabled: Boolean,
    compact: Boolean,
    paused: Boolean,
    onTogglePause: () -> Unit,
    onStartRest: () -> Unit,
    onExpand: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSkip: () -> Unit,
    primaryLabel: String,
    primaryDestructive: Boolean,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    confirm: DockConfirm?,
    onConfirmFinish: (updatePlan: Boolean) -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelConfirm: () -> Unit,
    insets: WindowInsets,
    modifier: Modifier = Modifier,
    stepSeconds: Int = 15,
) {
    val rest = restElapsed()
    val scheme = MaterialTheme.colorScheme
    val reached = restReached(rest, targetSeconds)
    val resting = rest != null
    val ringColor = if (reached) scheme.secondary else Rich.Rest.ink
    val clock = restClockLabel(rest, targetSeconds)
    val target = formatClock(targetSeconds * 1000L)

    // One gentle pulse when rest reaches its target (the haptic fires in LoggerScreen).
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(reached) {
        if (reached) {
            pulse.animateTo(1.04f, tween(140))
            pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val fill = cardColor()
    val shape = RoundedCornerShape(if (compact) Radius.pill else Radius.lg + 4.dp)
    Box(
        modifier
            .fillMaxWidth()
            // Nothing paints the margins round the card, so taps there would fall through to
            // the set rows scrolling underneath; an empty pointerInput makes the dock the target.
            .pointerInput(Unit) {}
            .windowInsetsPadding(insets)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                .shadow(18.dp, shape, ambientColor = DockShadow, spotColor = DockShadow)
                .clip(shape)
                .background(fill)
                // The tab bar's hairline edge, so the dock reads over white cards too.
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .animateContentSize()
                .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 10.dp),
        ) {
            CompositionLocalProvider(LocalBehind provides fill) {
                val ringDescription = when {
                    !resting -> "Rest target $target. Start a rest now"
                    reached -> "Rest over, ${clock.removePrefix("+")} past target. Open the rest timer"
                    else -> "Rest, $clock left. Open the rest timer"
                }
                if (compact) {
                    // Keyboard open: one slim line, so the focused set row stays in view.
                    Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                        RestRing(
                            remaining = restRemainingFraction(rest, targetSeconds),
                            color = ringColor,
                            size = 32.dp,
                            stroke = 4.dp,
                            full = reached,
                            modifier = Modifier.clip(CircleShape).semantics { contentDescription = ringDescription },
                        ) { Icon(Lucide.Timer, contentDescription = null, tint = ringColor, modifier = Modifier.size(15.dp)) }
                        Spacer(Modifier.width(Spacing.m))
                        Text(elapsed(), style = AppText.numberM, color = scheme.onSurface, maxLines = 1)
                        Text(
                            when {
                                paused -> "${NBSP}·${NBSP}paused"
                                resting -> "${NBSP}·${NBSP}rest $clock"
                                else -> ""
                            },
                            style = AppText.bodyS,
                            color = if (reached) scheme.secondary else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    return@CompositionLocalProvider
                }

                // ---- Rest: always here, so the target is one tap from either side. ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RestRing(
                        remaining = restRemainingFraction(rest, targetSeconds),
                        color = ringColor,
                        size = 52.dp,
                        stroke = 5.dp,
                        full = reached,
                        modifier = Modifier
                            .clip(CircleShape)
                            .semantics { role = Role.Button; contentDescription = ringDescription }
                            .clickable(enabled = enabled) { if (resting) onExpand() else onStartRest() },
                    ) {
                        if (resting) {
                            Text(clock, style = AppText.label, color = if (reached) scheme.secondary else scheme.onSurface, maxLines = 1)
                        } else {
                            Icon(Lucide.Timer, contentDescription = null, tint = ringColor, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(Spacing.m))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                !resting -> "Rest $target"
                                reached -> "Rest over"
                                paused -> "Rest paused"
                                else -> "Resting"
                            },
                            style = AppText.titleM,
                            color = if (reached) scheme.secondary else scheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            if (resting) "Target $target" else "Tap the ring to start",
                            style = AppText.bodyS,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StepKey("−$stepSeconds", "Shorten rest by $stepSeconds seconds", enabled, onMinus)
                    if (resting) {
                        Spacer(Modifier.width(6.dp))
                        SkipKey(enabled, onSkip)
                    }
                    Spacer(Modifier.width(6.dp))
                    StepKey("+$stepSeconds", "Lengthen rest by $stepSeconds seconds", enabled, onPlus)
                }
                Spacer(Modifier.height(Spacing.s))
                Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
                Spacer(Modifier.height(Spacing.s))

                // ---- Workout: pause, the clock, and Finish / Discard — or their confirmation. ----
                AnimatedContent(
                    targetState = confirm,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    contentKey = { it?.let { c -> c::class } },
                    label = "dockConfirm",
                ) { c ->
                    when (c) {
                        null -> Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            PauseKey(paused, enabled, onTogglePause)
                            Spacer(Modifier.width(Spacing.m))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    elapsed(),
                                    style = AppText.numberM,
                                    color = if (paused) scheme.onSurfaceVariant else scheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    when {
                                        paused -> "Paused"
                                        totalSets > 0 -> "$doneSets of $totalSets sets"
                                        else -> "Workout"
                                    },
                                    style = AppText.bodyS,
                                    color = if (paused) Rich.Duration.ink else scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(Spacing.s))
                            DockAction(primaryLabel, primaryDestructive, primaryEnabled, onPrimary)
                        }
                        is DockConfirm.Finish -> FinishConfirm(c, onConfirmFinish, onCancelConfirm)
                        is DockConfirm.Discard -> ConfirmRow(
                            title = "Discard workout?",
                            caption = c.caption,
                            confirmLabel = "Discard",
                            destructive = true,
                            enabled = true,
                            onConfirm = onConfirmDiscard,
                            onCancel = onCancelConfirm,
                        )
                    }
                }
            }
        }
    }
}

/** Finish in place: the plan-sync switch (when the plan changed) over the question and answers. */
@Composable
private fun FinishConfirm(c: DockConfirm.Finish, onConfirm: (Boolean) -> Unit, onCancel: () -> Unit) {
    var updatePlan by remember { mutableStateOf(false) }
    Column {
        if (c.planChanges.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(softFill())
                    .toggleable(value = updatePlan, enabled = !c.saving, role = Role.Switch) { updatePlan = it }
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Update ${c.planName ?: "the plan"}", style = AppText.label, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Text(
                        c.planChanges.joinToString(" · "),
                        style = AppText.bodyS,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AppSwitch(checked = updatePlan, onCheckedChange = null)
            }
            Spacer(Modifier.height(Spacing.s))
        }
        ConfirmRow(
            title = c.title,
            caption = c.caption,
            confirmLabel = "Finish",
            destructive = false,
            enabled = !c.saving,
            onConfirm = { onConfirm(updatePlan) },
            onCancel = onCancel,
        )
    }
}

/** The question on the left; a soft "Keep" and the confirming pill on the right. */
@Composable
private fun ConfirmRow(
    title: String,
    caption: String,
    confirmLabel: String,
    destructive: Boolean,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = Spacing.xs)) {
            Text(title, style = AppText.titleM, color = scheme.onSurface, maxLines = 1)
            Text(caption, style = AppText.bodyS, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Spacing.s))
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(softFill())
                .semantics { role = Role.Button }
                .pressable(onClick = onCancel)
                .padding(horizontal = Spacing.m, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Keep", style = AppText.label, color = scheme.onSurface) }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(if (destructive) scheme.error else scheme.primary)
                .semantics { role = Role.Button }
                .pressable(enabled = enabled, onClick = onConfirm)
                .padding(horizontal = Spacing.l, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(confirmLabel, style = AppText.label, color = if (destructive) scheme.onError else scheme.onPrimary) }
    }
}

/** Pause / Resume as a 44dp soft round key; amber while paused. */
@Composable
private fun PauseKey(paused: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (paused) Rich.Duration.ink else softFill())
            .semantics { role = Role.Button; contentDescription = if (paused) "Resume workout" else "Pause workout" }
            .pressable(enabled = enabled, scaleTo = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (paused) Lucide.Play else Lucide.Pause,
            contentDescription = null,
            tint = if (paused) cardColor() else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Finish (ink, with a check) or Discard (nothing logged yet: soft, a bin, red text) — at the
 * bottom so one thumb reaches it.
 */
@Composable
private fun DockAction(label: String, destructive: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fill = if (destructive) softFill() else scheme.primary
    val ink = when {
        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
        destructive -> scheme.error
        else -> scheme.onPrimary
    }
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(fill)
            .semantics { role = Role.Button }
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (destructive) Lucide.Trash2 else Lucide.Check, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.s))
        Text(label, style = AppText.button, color = ink, maxLines = 1)
    }
}

/** A 40dp soft round key: `−15` / `+15`. */
@Composable
private fun StepKey(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(softFill())
            .semantics { contentDescription = description; role = Role.Button }
            .pressable(enabled = enabled, scaleTo = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = AppText.label, color = MaterialTheme.colorScheme.onSurface) }
}

/** The ink Skip pill — the action under the thumb between sets. */
@Composable
private fun SkipKey(enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(scheme.primary)
            .semantics { role = Role.Button }
            .pressable(enabled = enabled, scaleTo = 0.94f, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text("Skip", style = AppText.label, color = scheme.onPrimary) }
}

/**
 * The rest timer taking the whole screen: a big draining ring on the mesh gradient with the
 * countdown at its centre, the next set underneath, and `−15 s · Skip · +15 s` under the thumb.
 * The workout clock stays in the corner. Collapses with the chevron, system back, or (in
 * LoggerScreen) when rest is cleared.
 */
@Composable
fun RestFocus(
    elapsed: () -> String,
    restElapsed: () -> Long?,
    targetSeconds: Int,
    next: String?,
    onCollapse: () -> Unit,
    onTargetClick: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    stepSeconds: Int = 15,
) {
    val scheme = MaterialTheme.colorScheme
    val rest = restElapsed()
    val reached = restReached(rest, targetSeconds)
    val clock = restClockLabel(rest, targetSeconds)
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
            .background(scheme.background)
            .meshGradient(animate = false)
            .dotTexture(color = scheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(icon = Lucide.ChevronDown, contentDescription = "Close the rest timer", onClick = onCollapse)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = Spacing.s)) {
                    Text("Workout", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
                    Text(elapsed(), style = AppText.numberM, color = scheme.onSurface)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (reached) "Rest over — next set" else "Rest",
                style = AppText.titleL,
                color = if (reached) scheme.secondary else scheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.l))
            RestRing(
                remaining = restRemainingFraction(rest, targetSeconds),
                color = if (reached) scheme.secondary else Rich.Rest.ink,
                size = 260.dp,
                stroke = 18.dp,
                full = reached,
                modifier = Modifier.semantics {
                    contentDescription = if (reached) "Rest over, ${clock.removePrefix("+")} past target" else "$clock of rest left"
                },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(clock, style = AppText.numberXXL, color = if (reached) scheme.secondary else scheme.onSurface)
                    Text(
                        "of ${formatClock(targetSeconds * 1000L)}",
                        style = AppText.bodyS,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.pill))
                            .clickable(role = Role.Button, onClickLabel = "Rest settings", onClick = onTargetClick)
                            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            if (next != null) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(cardColor().copy(alpha = 0.75f))
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Next", style = AppText.eyebrow, color = scheme.onSurfaceVariant)
                    Text(next, style = AppText.titleM, color = scheme.onSurface, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.weight(1f))
            CompositionLocalProvider(LocalBehind provides scheme.background) {
                RestTransport(
                    onMinus = onMinus,
                    onSkip = onSkip,
                    onPlus = onPlus,
                    stepSeconds = stepSeconds,
                    skipEnabled = rest != null,
                )
            }
            Spacer(Modifier.height(Spacing.l))
        }
    }
}
