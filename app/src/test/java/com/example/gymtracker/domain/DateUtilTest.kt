package com.example.gymtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DateUtilTest {
    @Test
    fun pickerMillisRoundTripToTheTappedDate() {
        val day = LocalDate.of(2026, 11, 23)
        val millis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(day.toEpochDay(), DateUtil.pickerMillisToEpochDay(millis))
        assertEquals(millis, DateUtil.epochDayToPickerMillis(day.toEpochDay()))
    }

    @Test
    fun pickerMillisIgnoreTheDeviceZone() {
        // A UTC-midnight value must map to the same calendar day whatever the local offset;
        // converting through a negative-offset zone would land on the previous day.
        val day = LocalDate.of(2026, 1, 1)
        val millis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val viaLocal = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.of("America/Los_Angeles")).toLocalDate()
        assertEquals(day.minusDays(1), viaLocal)
        assertEquals(day.toEpochDay(), DateUtil.pickerMillisToEpochDay(millis))
    }

    @Test
    fun pickerMillisBeforeEpochFloor() {
        val day = LocalDate.of(1969, 12, 31)
        val millis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 1
        assertEquals(day.toEpochDay(), DateUtil.pickerMillisToEpochDay(millis))
    }

    @Test
    fun startOfWeekMatchesLocalDateForEveryWeekStart() {
        // The arithmetic version must agree with the LocalDate one it replaced, either side of
        // the epoch and across leap years.
        for (epochDay in -1_500L..30_000L) {
            val d = LocalDate.ofEpochDay(epochDay)
            for (weekStart in DayOfWeek.entries) {
                val daysBack = ((d.dayOfWeek.value - weekStart.value) + 7) % 7
                val expected = d.minusDays(daysBack.toLong()).toEpochDay()
                assertEquals("day $epochDay, $weekStart", expected, DateUtil.startOfWeek(epochDay, weekStart))
            }
        }
    }

    @Test
    fun startOfWeekDefaultsToMonday() {
        val sunday = LocalDate.of(2026, 11, 29).toEpochDay()
        assertEquals(LocalDate.of(2026, 11, 23).toEpochDay(), DateUtil.startOfWeek(sunday))
    }
}
