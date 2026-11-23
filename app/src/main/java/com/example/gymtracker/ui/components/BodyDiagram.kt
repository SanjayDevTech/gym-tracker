package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.gymtracker.data.Muscle
import com.example.gymtracker.ui.components.atoms.heatColor
import com.example.gymtracker.ui.rememberAppHaptics
import kotlin.math.min

enum class BodyView { Front, Back }

/**
 * §2 `BodyDiagram` (revised): regions filled from [fills] (callers use the heat ramp —
 * [heatFill]); a muscle not in the map is the empty `outlineVariant`. Region edges in `outline`,
 * the selected muscle outlined `onSurface` 1.5. The back view has its own silhouette (issue 33).
 * Hit-testing is unchanged: topmost region wins.
 */
@Composable
fun BodyDiagram(
    view: BodyView,
    fills: Map<Muscle, Color>,
    modifier: Modifier = Modifier,
    selectedMuscle: Muscle? = null,
    onMuscleTap: ((Muscle) -> Unit)? = null,
) {
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val haptics = rememberAppHaptics()

    val regions = when (view) {
        BodyView.Front -> BodyDiagramPaths.FrontRegions
        BodyView.Back -> BodyDiagramPaths.BackRegions
    }
    val silhouette = when (view) {
        BodyView.Front -> BodySilhouette.Front
        BodyView.Back -> BodySilhouette.Back
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
                regions.forEach { (muscle, region) ->
                    drawPath(region.path, fills[muscle] ?: emptyColor)
                    drawPath(region.path, outlineColor, style = Stroke(width = 0.4f))
                }
                // Silhouette over the fills so the back view's spine and blade lines stay visible.
                drawPath(path = silhouette, color = outlineColor, style = Stroke(width = 1f))
                if (selectedMuscle != null) {
                    regions[selectedMuscle]?.let { region ->
                        drawPath(region.path, selectedColor, style = Stroke(width = 1.5f))
                    }
                }
            }
        }
    }
}

/**
 * Heat-ramp fills for 0..1 volume levels. Any trained muscle starts a quarter of the way up the
 * ramp so a light week never reads as untrained; zero/absent stays the empty colour.
 */
fun heatFill(levels: Map<Muscle, Float>): Map<Muscle, Color> =
    levels.filterValues { it > 0f }.mapValues { heatColor(0.25f + 0.75f * it.value.coerceIn(0f, 1f)) }

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
