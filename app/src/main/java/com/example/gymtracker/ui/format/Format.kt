package com.example.gymtracker.ui.format

import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.kgToDisplay
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Number formatting — design-plan §2, the `ValueText` + `Format.kt` row.
 *
 * One rounding rule for the whole app, and every weight and volume in the
 * user's preferred unit (house rule 9). Missing values render as [EM_DASH].
 */

/** Non-breaking space. Thousands separator and number–unit gap both use it. */
const val NBSP = ' '

/** Placeholder for a value that does not exist (not zero). */
const val EM_DASH = "—"

private const val LB_PER_KG = 2.20462262

/** `12400` → `12 400`. Grouped with a non-breaking space so it never wraps. */
fun grouped(value: Number): String {
    val n = value.toDouble().roundToLong()
    val digits = abs(n).toString()
    val out = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) out.append(NBSP)
        out.append(c)
    }
    return if (n < 0) "-$out" else out.toString()
}

/** `62.5` stays `62.5`, `60.0` becomes `60` — never a trailing `.0`. */
fun trimDecimal(value: Float): String =
    if (value % 1f == 0f) value.toLong().toString() else value.toString()

/** A load in the display unit, number only: `62.5`, `140`, or [EM_DASH]. */
fun formatWeightNumber(kg: Float?, unit: WeightUnit): String =
    kg?.let { trimDecimal(it.kgToDisplay(unit)) } ?: EM_DASH

/** A load with its unit: `62.5 kg`, `137.8 lb`, or [EM_DASH]. */
fun formatWeight(kg: Float?, unit: WeightUnit): String =
    kg?.let { formatWeightNumber(it, unit) + NBSP + unit.label } ?: EM_DASH

/** Volume (Σ reps × load) as a number only, in the display unit: `12 400`. */
fun formatVolumeNumber(kg: Double, unit: WeightUnit): String = when (unit) {
    WeightUnit.Kg -> grouped(kg)
    WeightUnit.Lb -> grouped(kg * LB_PER_KG)
}

/**
 * Volume with its unit — `12 400 kg` / `27 340 lb`. Replaces the old "kg·reps",
 * which was printed even when the user trains in pounds.
 */
fun formatVolume(kg: Double, unit: WeightUnit): String =
    formatVolumeNumber(kg, unit) + NBSP + unit.label

/** A running clock: `4:07`, `1:02:03`. Negative input clamps to zero. */
fun formatClock(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** A finished span: `48 min`, `1 h 05 min`, or [EM_DASH] when unknown. */
fun formatDuration(millis: Long?): String {
    if (millis == null || millis < 0) return EM_DASH
    val totalMin = millis / 60_000L
    val h = totalMin / 60L
    val m = totalMin % 60L
    return if (h > 0) "$h${NBSP}h ${"%02d".format(m)}${NBSP}min" else "$m${NBSP}min"
}

/** The noun agreeing with [count]: `plural(1, "set")` → `set`, `plural(3, "set")` → `sets`. */
fun plural(count: Long, noun: String): String = if (count == 1L) noun else "${noun}s"

/** [Int] twin of [plural]. */
fun plural(count: Int, noun: String): String = plural(count.toLong(), noun)
