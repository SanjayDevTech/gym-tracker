package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.components.atoms.heatRamp
import com.example.gymtracker.data.displayName
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing
import kotlin.math.min

enum class BodyView { Front, Back }

/**
 * One view of the anatomical figure: a soft silhouette, each muscle filled from [fills] (callers
 * use [heatFill]) with a hairline gap of the card colour between muscles, so the figure reads as
 * a set of shapes rather than a blob. A muscle missing from [fills] is the ramp's "untrained"
 * stop. The [selectedMuscle] gets an ink outline; taps hit the topmost shape.
 */
@Composable
fun BodyDiagram(
    view: BodyView,
    fills: Map<Muscle, Color>,
    modifier: Modifier = Modifier,
    selectedMuscle: Muscle? = null,
    onMuscleTap: ((Muscle) -> Unit)? = null,
) {
    val ramp = heatRamp()
    val gap = cardColor()
    val untrained = ramp.first()
    val body = lerp(gap, untrained, 0.5f)
    val selected = MaterialTheme.colorScheme.onSurface
    val haptics = rememberAppHaptics()
    val shapes = when (view) {
        BodyView.Front -> BodyPaths.Front
        BodyView.Back -> BodyPaths.Back
    }
    val trained = shapes.keys.filter { fills.containsKey(it) }

    Canvas(
        modifier = modifier
            .aspectRatio(VIEWBOX_W / VIEWBOX_H)
            .semantics {
                contentDescription = "${view.name} view. " + if (trained.isEmpty()) "Nothing trained"
                else "Trained: " + trained.joinToString { it.displayName }
            }
            .pointerInput(view, onMuscleTap) {
                if (onMuscleTap == null) return@pointerInput
                detectTapGestures { tap ->
                    val vp = viewport(size.width.toFloat(), size.height.toFloat())
                    val x = (tap.x - vp.dx) / vp.scale
                    val y = (tap.y - vp.dy) / vp.scale
                    shapes.entries.lastOrNull { (_, s) -> s.contains(x, y) }?.let {
                        haptics.light()
                        onMuscleTap(it.key)
                    }
                }
            },
    ) {
        val vp = viewport(size.width, size.height)
        translate(vp.dx, vp.dy) {
            scale(vp.scale, vp.scale, pivot = Offset.Zero) {
                drawPath(BodyPaths.Silhouette, body)
                val stroke = Stroke(width = 1.6f, join = StrokeJoin.Round)
                shapes.forEach { (muscle, shape) ->
                    drawPath(shape.path, fills[muscle] ?: untrained)
                    drawPath(shape.path, gap, style = stroke)
                }
                if (selectedMuscle != null) {
                    shapes[selectedMuscle]?.let {
                        drawPath(it.path, selected, style = Stroke(width = 2f, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

/** Front and back side by side, each captioned. The muscle map on Home and Progress. */
@Composable
fun BodyMap(
    fills: Map<Muscle, Color>,
    modifier: Modifier = Modifier,
    selectedMuscle: Muscle? = null,
    onMuscleTap: ((Muscle) -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        BodyView.entries.forEach { view ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                BodyDiagram(
                    view = view,
                    fills = fills,
                    selectedMuscle = selectedMuscle,
                    onMuscleTap = onMuscleTap,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(view.name, style = AppText.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Ramp fills for 0..1 volume levels. Any trained muscle starts a quarter of the way up so a light
 * week never reads as untrained; zero/absent stays the untrained stop.
 */
fun heatFill(levels: Map<Muscle, Float>, ramp: List<Color>): Map<Muscle, Color> =
    levels.filterValues { it > 0f }.mapValues { heatColor(0.25f + 0.75f * it.value.coerceIn(0f, 1f), ramp) }

private data class Viewport(val scale: Float, val dx: Float, val dy: Float)

private fun viewport(w: Float, h: Float): Viewport {
    val s = min(w / VIEWBOX_W, h / VIEWBOX_H)
    return Viewport(s, (w - VIEWBOX_W * s) / 2f, (h - VIEWBOX_H * s) / 2f)
}
