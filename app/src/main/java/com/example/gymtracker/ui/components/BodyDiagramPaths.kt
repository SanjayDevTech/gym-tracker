package com.example.gymtracker.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.example.gymtracker.data.Muscle

/**
 * Stylized anatomical regions in a 100×220 viewbox. Each Muscle maps to a region made of
 * one or more axis-aligned rounded rectangles (one per anatomical side where applicable).
 * Hit-test is per-rect; drawing is via the union Path.
 */
data class MuscleRegion(val rects: List<RectF>) {
    fun contains(point: Offset): Boolean = rects.any { it.contains(point) }

    /**
     * The region's draw path, built once and reused across every frame. The geometry is static in
     * viewbox space — the canvas applies the scale/translate transform — so there's no reason to
     * reallocate a [Path] per draw. Accessed only from the draw scope (render thread), hence the
     * cheap non-synchronized lazy.
     */
    val path: Path by lazy(LazyThreadSafetyMode.NONE) {
        Path().apply { rects.forEach { r -> addRoundRect(r.toComposeRect(), 3f) } }
    }
}

/** Mutable plain-old-data rect so we can describe regions without importing Compose. */
data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(p: Offset): Boolean = p.x in left..right && p.y in top..bottom
    fun toComposeRect(): Rect = Rect(left, top, right, bottom)
}

private fun Path.addRoundRect(rect: Rect, corner: Float) {
    val r = androidx.compose.ui.geometry.RoundRect(
        rect = rect,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
    )
    addRoundRect(r)
}

private fun rect(l: Float, t: Float, r: Float, b: Float) = RectF(l, t, r, b)

/** Silhouette outline drawn behind the muscles. */
object BodySilhouette {
    val Front: Path = Path().apply {
        // Head
        addOval(Rect(40f, 4f, 60f, 24f))
        // Neck
        addRect(Rect(46f, 22f, 54f, 30f))
        // Torso (shoulders → waist)
        moveTo(28f, 30f)
        cubicTo(28f, 28f, 30f, 27f, 38f, 27f)
        lineTo(62f, 27f)
        cubicTo(70f, 27f, 72f, 28f, 72f, 30f)
        lineTo(72f, 100f)
        cubicTo(72f, 108f, 68f, 110f, 60f, 112f)
        lineTo(40f, 112f)
        cubicTo(32f, 110f, 28f, 108f, 28f, 100f)
        close()
        // Arms (left)
        moveTo(15f, 36f)
        cubicTo(15f, 32f, 18f, 30f, 23f, 30f)
        lineTo(28f, 32f)
        lineTo(28f, 70f)
        lineTo(26f, 110f)
        cubicTo(20f, 110f, 14f, 108f, 14f, 100f)
        close()
        // Arms (right)
        moveTo(85f, 36f)
        cubicTo(85f, 32f, 82f, 30f, 77f, 30f)
        lineTo(72f, 32f)
        lineTo(72f, 70f)
        lineTo(74f, 110f)
        cubicTo(80f, 110f, 86f, 108f, 86f, 100f)
        close()
        // Hips/upper legs
        moveTo(30f, 112f)
        lineTo(70f, 112f)
        lineTo(72f, 175f)
        lineTo(52f, 178f)
        lineTo(48f, 178f)
        lineTo(28f, 175f)
        close()
        // Lower legs (left)
        moveTo(32f, 178f)
        lineTo(48f, 178f)
        lineTo(48f, 215f)
        lineTo(34f, 215f)
        close()
        // Lower legs (right)
        moveTo(52f, 178f)
        lineTo(68f, 178f)
        lineTo(66f, 215f)
        lineTo(52f, 215f)
        close()
    }

    /**
     * The back view's own path (issue 33): the same outline, plus the spine and the gluteal
     * cleft, so the two figures no longer read as identical twins.
     */
    val Back: Path = Path().apply {
        addPath(Front)
        // Spine, neck to sacrum.
        moveTo(50f, 30f)
        lineTo(50f, 110f)
        // Gluteal cleft.
        moveTo(50f, 114f)
        lineTo(50f, 136f)
        // Shoulder-blade ridges.
        moveTo(36f, 48f)
        lineTo(44f, 62f)
        moveTo(64f, 48f)
        lineTo(56f, 62f)
    }
}

object BodyDiagramPaths {

    val FrontRegions: Map<Muscle, MuscleRegion> = mapOf(
        Muscle.Chest to MuscleRegion(listOf(rect(32f, 33f, 49f, 56f), rect(51f, 33f, 68f, 56f))),
        Muscle.FrontDelts to MuscleRegion(listOf(rect(24f, 28f, 36f, 42f), rect(64f, 28f, 76f, 42f))),
        Muscle.SideDelts to MuscleRegion(listOf(rect(17f, 31f, 25f, 45f), rect(75f, 31f, 83f, 45f))),
        Muscle.Biceps to MuscleRegion(listOf(rect(18f, 44f, 28f, 70f), rect(72f, 44f, 82f, 70f))),
        Muscle.Forearms to MuscleRegion(listOf(rect(14f, 72f, 26f, 108f), rect(74f, 72f, 86f, 108f))),
        Muscle.Abs to MuscleRegion(listOf(rect(42f, 56f, 58f, 100f))),
        Muscle.Obliques to MuscleRegion(listOf(rect(32f, 64f, 41f, 100f), rect(59f, 64f, 68f, 100f))),
        Muscle.Quads to MuscleRegion(listOf(rect(30f, 118f, 49f, 172f), rect(51f, 118f, 70f, 172f))),
        // Upper inner thigh (groin) — the medial adductor mass, drawn over the top-inner quads.
        Muscle.Adductors to MuscleRegion(listOf(rect(42f, 118f, 49f, 148f), rect(51f, 118f, 58f, 148f))),
        Muscle.Calves to MuscleRegion(listOf(rect(34f, 180f, 47f, 212f), rect(53f, 180f, 66f, 212f))),
    )

    val BackRegions: Map<Muscle, MuscleRegion> = mapOf(
        Muscle.Traps to MuscleRegion(listOf(rect(38f, 28f, 62f, 46f))),
        Muscle.RearDelts to MuscleRegion(listOf(rect(24f, 30f, 36f, 44f), rect(64f, 30f, 76f, 44f))),
        Muscle.SideDelts to MuscleRegion(listOf(rect(17f, 31f, 25f, 45f), rect(75f, 31f, 83f, 45f))),
        Muscle.UpperBack to MuscleRegion(listOf(rect(32f, 46f, 68f, 66f))),
        Muscle.Lats to MuscleRegion(listOf(rect(26f, 52f, 44f, 92f), rect(56f, 52f, 74f, 92f))),
        Muscle.LowerBack to MuscleRegion(listOf(rect(38f, 92f, 62f, 110f))),
        Muscle.Triceps to MuscleRegion(listOf(rect(18f, 44f, 28f, 72f), rect(72f, 44f, 82f, 72f))),
        Muscle.Forearms to MuscleRegion(listOf(rect(14f, 74f, 26f, 108f), rect(74f, 74f, 86f, 108f))),
        Muscle.Glutes to MuscleRegion(listOf(rect(31f, 112f, 49f, 138f), rect(51f, 112f, 69f, 138f))),
        Muscle.Hamstrings to MuscleRegion(listOf(rect(31f, 140f, 49f, 175f), rect(51f, 140f, 69f, 175f))),
        Muscle.Calves to MuscleRegion(listOf(rect(34f, 180f, 47f, 212f), rect(53f, 180f, 66f, 212f))),
    )

    const val VIEWBOX_WIDTH = 100f
    const val VIEWBOX_HEIGHT = 220f
}
