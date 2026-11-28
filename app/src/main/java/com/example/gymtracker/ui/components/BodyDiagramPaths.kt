package com.example.gymtracker.ui.components

import android.graphics.Region
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import com.example.gymtracker.data.Muscle

/**
 * The anatomical figure, front and back, in a 200×400 viewbox: head, neck, hands with a thumb,
 * feet, and 18 muscle regions (three heads of the quads, two of the hamstrings and calves). Each shape is drawn once for the
 * viewer's left half and mirrored about x = 100, so the two sides can never drift apart. Muscles
 * are drawn over a soft silhouette with a hairline gap between them.
 *
 * The path data is SVG, so the figure can be previewed and edited outside the app.
 */
class MuscleShape(val path: Path) {
    private val region: Region by lazy(LazyThreadSafetyMode.NONE) {
        Region().apply { setPath(path.asAndroidPath(), Region(0, 0, VIEWBOX_W.toInt(), VIEWBOX_H.toInt())) }
    }

    /** Hit-test in viewbox units. */
    fun contains(x: Float, y: Float): Boolean = region.contains(x.toInt(), y.toInt())
}

const val VIEWBOX_W = 200f
const val VIEWBOX_H = 400f

/** x → 200 − x: the right half of the figure from its left half. */
private val Mirror = Matrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        VIEWBOX_W, 0f, 0f, 1f,
    ),
)

/** One left-half SVG path plus its mirror image. */
private fun mirrored(vararg halves: String): Path = Path().apply {
    halves.forEach { d ->
        val half = PathParser().parsePathString(d).toPath()
        addPath(half)
        addPath(Path().apply { addPath(half); transform(Mirror) })
    }
}

private fun shapes(spec: Map<Muscle, List<String>>): Map<Muscle, MuscleShape> =
    spec.mapValues { (_, halves) -> MuscleShape(mirrored(*halves.toTypedArray())) }

object BodyPaths {
    /** The body outline both views sit on (one outline serves front and back). */
    val Silhouette: Path by lazy(LazyThreadSafetyMode.NONE) {
        mirrored(
                    "M100,8 C88,8 80,17 80,31 C80,40 83,48 88,53 C90,56 91,59 91,62 L91,66 C84,69 74,69 64,71 C54,73 47,79 45,90 C43,100 43,108 42,116 C40,132 38,146 37,158 C35,166 33,176 31,188 C29,202 28,212 28,222 C26,232 24,242 25,252 C26,260 28,266 31,268 C34,270 37,268 38,264 C39,258 38,250 38,244 C40,246 42,250 43,253 C45,254 46,251 45,248 C43,242 41,236 39,230 C40,224 41,218 42,212 C44,198 47,186 50,176 C52,168 53,162 54,156 C56,144 58,130 60,116 C61,112 62,108 63,106 C64,122 66,140 67,156 C68,168 67,178 66,186 C65,196 64,206 64,216 C63,236 63,256 65,276 C66,286 67,294 67,300 C66,316 65,332 67,348 C68,358 69,366 69,372 C66,380 62,386 63,391 C65,396 76,397 84,395 C88,394 89,390 88,386 C87,380 86,376 86,372 C87,360 88,346 89,330 C90,316 90,304 90,296 C91,280 93,262 95,246 C96,236 98,228 100.6,226 L100.6,8 Z",
        )
    }

    val Front: Map<Muscle, MuscleShape> by lazy(LazyThreadSafetyMode.NONE) {
        shapes(
            mapOf(
                Muscle.Traps to listOf(
                    "M91,62 C86,66 76,68 66,70 C76,72 86,72 93,70 C93,67 92,64 91,62 Z",
                ),
                Muscle.SideDelts to listOf(
                    "M62,71 C53,73 47,79 45,90 C44,98 44,105 45,111 C47,101 48,92 52,84 C55,78 58,74 62,71 Z",
                ),
                Muscle.FrontDelts to listOf(
                    "M66,72 C58,73 53,77 51,84 C50,92 51,100 54,106 C57,98 61,90 67,84 C70,80 69,74 66,72 Z",
                ),
                Muscle.Chest to listOf(
                    "M98,75 C88,73 77,74 70,80 C63,86 59,95 58,104 C62,113 72,121 84,122 C93,122 98,117 98,110 Z",
                ),
                Muscle.Biceps to listOf(
                    "M55,112 C49,115 45,124 44,136 C43,146 45,153 48,158 C53,152 56,142 57,131 C58,122 58,116 55,112 Z",
                ),
                Muscle.Forearms to listOf(
                    "M48,160 C41,166 36,178 33,192 C31,203 30,212 30,220 C35,223 40,221 42,215 C45,202 49,188 52,177 C54,168 53,162 48,160 Z",
                ),
                Muscle.Abs to listOf(
                    "M89.5,125 C93,124 97,124 98.5,125 L98.5,140 C95,141 92,141 89.5,140 C88.5,135 88.5,130 89.5,125 Z",
                    "M89.5,143 C93,142 97,142 98.5,143 L98.5,158 C95,159 92,159 89.5,158 C88.5,153 88.5,148 89.5,143 Z",
                    "M89.5,161 C93,160 97,160 98.5,161 L98.5,176 C95,177 92,177 89.5,176 C88.5,171 88.5,166 89.5,161 Z",
                    "M89.5,179 C93,178 97,178 98.5,179 L98.5,212 C96,212 93,207 91.5,200 C89.5,193 88.5,186 89.5,179 Z",
                ),
                Muscle.Obliques to listOf(
                    "M86,124 C80,124 72,128 68,134 C67,148 68,162 68,174 C70,186 77,196 86,203 C86,190 85,176 85,162 C85,148 85,136 86,124 Z",
                ),
                Muscle.Quads to listOf(
                    "M66,214 C62,232 61,252 63,272 C64,283 67,291 72,296 C75,282 76,262 76,244 C76,230 73,220 66,214 Z",
                    "M78,212 C76,230 76,252 78,274 C79,285 81,292 85,295 C89,283 90,263 89,243 C88,228 84,216 78,212 Z",
                    "M90,258 C87,268 86,280 87,292 C91,295 94,291 94,283 C94,274 93,265 90,258 Z",
                ),
                Muscle.Adductors to listOf(
                    "M91,228 C89,236 89,246 90,254 C93,248 95,240 97,233 C97,229 95,228 91,228 Z",
                ),
                Muscle.Calves to listOf(
                    "M68,304 C64,316 64,332 66,346 C68,354 73,354 75,348 C77,334 77,318 74,306 Z",
                    "M86,304 C83,316 82,332 84,350 C88,348 90,338 90,324 C90,315 88,308 86,304 Z",
                ),
            ),
        )
    }

    val Back: Map<Muscle, MuscleShape> by lazy(LazyThreadSafetyMode.NONE) {
        shapes(
            mapOf(
                Muscle.Traps to listOf(
                    "M100,56 C95,62 86,66 70,69 C78,76 86,86 92,98 C96,106 98,114 100.5,122 L100.5,56 Z",
                ),
                Muscle.SideDelts to listOf(
                    "M60,71 C51,74 46,81 44,92 C44,100 44,106 46,111 C46,101 48,92 51,86 C54,79 57,74 60,71 Z",
                ),
                Muscle.RearDelts to listOf(
                    "M64,72 C56,74 51,80 49,88 C52,95 56,98 60,96 C64,89 67,81 70,74 Z",
                ),
                Muscle.UpperBack to listOf(
                    "M72,74 C67,82 65,92 66,101 C72,106 80,110 89,110 C90,100 88,90 84,82 C80,77 76,74 72,74 Z",
                ),
                Muscle.Lats to listOf(
                    "M64,105 C62,120 64,136 69,152 C75,163 83,170 92,173 C92,156 91,134 90,115 C81,114 72,110 64,105 Z",
                ),
                Muscle.LowerBack to listOf(
                    "M95,142 C91,154 88,168 87,184 C90,190 95,193 99,193 L99,142 Z",
                ),
                Muscle.Triceps to listOf(
                    "M55,100 C49,104 45,113 45,126 C45,138 47,148 51,156 C56,148 59,137 59,125 C59,113 58,104 55,100 Z",
                ),
                Muscle.Forearms to listOf(
                    "M48,160 C41,166 36,178 33,192 C31,203 30,212 30,220 C35,223 40,221 42,215 C45,202 49,188 52,177 C54,168 53,162 48,160 Z",
                ),
                Muscle.Glutes to listOf(
                    "M98.5,200 C88,196 76,198 69,207 C65,217 65,229 71,238 C79,244 91,244 98.5,238 Z",
                ),
                Muscle.Hamstrings to listOf(
                    "M68,244 C64,258 64,275 68,290 C72,296 78,296 81,290 C83,275 83,259 81,246 C77,244 72,244 68,244 Z",
                    "M84,246 C87,260 87,275 84,291 C88,294 91,290 91,281 C91,270 91,258 90,248 Z",
                ),
                Muscle.Adductors to listOf(
                    "M92,244 C91,254 91,266 92,276 C95,268 97,258 97,248 C96,245 94,244 92,244 Z",
                ),
                Muscle.Calves to listOf(
                    "M68,300 C63,311 62,326 65,341 C68,348 74,348 77,341 C79,328 78,313 76,302 C73,297 70,297 68,300 Z",
                    "M80,302 C84,312 87,326 86,341 C84,350 79,350 78,341 C78,328 78,313 80,302 Z",
                ),
            ),
        )
    }
}
