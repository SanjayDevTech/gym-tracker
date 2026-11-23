package com.example.gymtracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

object DateUtil {
    fun todayEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).toEpochDay()

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * Material's DatePicker speaks UTC-midnight millis for a *calendar date*, independent of
     * the device zone. Convert with floor division (no zone) so the picked date is the logged
     * date; "today" for capping is always judged in the local zone ([todayEpochDay]).
     */
    fun pickerMillisToEpochDay(utcMillis: Long): Long = Math.floorDiv(utcMillis, MILLIS_PER_DAY)

    fun epochDayToPickerMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    fun startOfWeek(epochDay: Long, weekStart: DayOfWeek = DayOfWeek.MONDAY): Long {
        val d = LocalDate.ofEpochDay(epochDay)
        val daysBack = ((d.dayOfWeek.value - weekStart.value) + 7) % 7
        return d.minusDays(daysBack.toLong()).toEpochDay()
    }

    fun rangeDaysBack(today: Long, days: Int): List<Long> =
        (today - (days - 1)..today).toList()

    /** Compact human label for how long ago [epochDay] was relative to [today]. */
    fun relativeDayLabel(epochDay: Long, today: Long = todayEpochDay()): String =
        when (val diff = today - epochDay) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2..6 -> "${diff}d ago"
            in 7..13 -> "1w ago"
            in 14..30 -> "${diff / 7}w ago"
            in 31..364 -> "${diff / 30}mo ago"
            else -> if (diff < 0) "Today" else "${diff / 365}y ago"
        }
}
