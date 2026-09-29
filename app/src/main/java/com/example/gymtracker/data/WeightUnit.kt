package com.example.gymtracker.data

import kotlin.math.roundToInt

enum class WeightUnit(val label: String) {
    Kg("kg"),
    Lb("lb"),
}

private const val KG_PER_LB = 0.45359237f
private const val LB_PER_KG = 2.20462262f

/** Convert canonical kg to display unit, rounded to one decimal. */
fun Float.kgToDisplay(unit: WeightUnit): Float = when (unit) {
    WeightUnit.Kg -> roundOneDecimal(this)
    WeightUnit.Lb -> roundOneDecimal(this * LB_PER_KG)
}

/** Convert displayed value back to canonical kg, rounded to one decimal. */
fun Float.displayToKg(unit: WeightUnit): Float = when (unit) {
    WeightUnit.Kg -> roundOneDecimal(this)
    WeightUnit.Lb -> roundOneDecimal(this * KG_PER_LB)
}

private fun roundOneDecimal(v: Float): Float = (v * 10f).roundToInt() / 10f
