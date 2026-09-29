package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.ui.rememberAppHaptics
import kotlin.math.min
import kotlin.math.pow

enum class BodyView { Front, Back }

@Composable
fun BodyDiagram(
    view: BodyView,
    volumes: Map<Muscle, Float>,
    modifier: Modifier = Modifier,
    selectedMuscle: Muscle? = null,
    onMuscleTap: ((Muscle) -> Unit)? = null,
) {
    // Cold (untrained) → warm (high volume) ramp. The empty state is a faint, clearly-cold fill
    // so the figure always reads as a body; trained muscles climb toward the warm accent.
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val coldColor = MaterialTheme.colorScheme.surfaceVariant
    val hotColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val silhouetteColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val haptics = rememberAppHaptics()

    val regions = when (view) {
        BodyView.Front -> BodyDiagramPaths.FrontRegions
        BodyView.Back -> BodyDiagramPaths.BackRegions
    }

    Canvas(
        modifier = modifier.pointerInput(view, onMuscleTap) {
            if (onMuscleTap == null) return@pointerInput
            detectTapGestures { tap ->
                val vp = computeViewport(size.width.toFloat(), size.height.toFloat())
                val local = Offset(
                    x = (tap.x - vp.offsetX) / vp.scale,
                    y = (tap.y - vp.offsetY) / vp.scale,
                )
                // Hit-test topmost first (reverse draw order) so overlaid regions like Adductors,
                // drawn over the inner Quads, win the tap instead of the region beneath them.
                val hit = regions.entries.lastOrNull { (_, r) -> r.contains(local) }
                if (hit != null) {
                    haptics.light()
                    onMuscleTap(hit.key)
                }
            }
        },
    ) {
        val vp = computeViewport(size.width, size.height)
        translate(vp.offsetX, vp.offsetY) {
            scale(scaleX = vp.scale, scaleY = vp.scale, pivot = Offset.Zero) {
                drawPath(
                    path = BodySilhouette.Front,
                    color = silhouetteColor,
                    style = Stroke(width = 1f),
                )
                regions.forEach { (muscle, region) ->
                    val raw = (volumes[muscle] ?: 0f).coerceIn(0f, 1f)
                    val path = region.path
                    val fill = if (raw <= 0f) {
                        emptyColor
                    } else {
                        // Ease so mid-volume muscles separate visibly instead of bunching dark.
                        lerp(coldColor, hotColor, raw.pow(0.65f))
                    }
                    drawPath(path, fill)
                    drawPath(path, outlineColor, style = Stroke(width = 0.4f))
                }
                // Draw the selected muscle's outline last so it sits on top of its neighbours.
                if (selectedMuscle != null) {
                    regions[selectedMuscle]?.let { region ->
                        drawPath(region.path, selectedColor, style = Stroke(width = 1.6f))
                    }
                }
            }
        }
    }
}

private data class Viewport(val scale: Float, val offsetX: Float, val offsetY: Float)

private fun computeViewport(canvasW: Float, canvasH: Float): Viewport {
    val s = min(
        canvasW / BodyDiagramPaths.VIEWBOX_WIDTH,
        canvasH / BodyDiagramPaths.VIEWBOX_HEIGHT,
    )
    val contentW = BodyDiagramPaths.VIEWBOX_WIDTH * s
    val contentH = BodyDiagramPaths.VIEWBOX_HEIGHT * s
    return Viewport(
        scale = s,
        offsetX = (canvasW - contentW) / 2f,
        offsetY = (canvasH - contentH) / 2f,
    )
}
