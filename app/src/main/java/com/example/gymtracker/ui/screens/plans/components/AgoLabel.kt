package com.example.gymtracker.ui.screens.plans.components

import com.example.gymtracker.domain.DateUtil
import com.example.gymtracker.ui.format.plural

/** `today`, `yesterday`, `3 days ago`, `2 weeks ago`, `4 months ago`. */
internal fun agoLabel(epochDay: Long, today: Long = DateUtil.todayEpochDay()): String {
    val days = (today - epochDay).coerceAtLeast(0)
    fun ago(n: Long, unit: String) = "$n ${plural(n, unit)} ago"
    return when {
        days == 0L -> "today"
        days == 1L -> "yesterday"
        days < 14 -> ago(days, "day")
        days < 61 -> ago(days / 7, "week")
        days < 365 -> ago(days / 30, "month")
        else -> ago(days / 365, "year")
    }
}
