package com.example.gymtracker.ui.screens.history

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class HistoryFormatterTest {
    private val original = Locale.getDefault()

    @After fun restoreLocale() = Locale.setDefault(original)

    @Test fun samePatternAndLocaleReusesOneFormatter() {
        Locale.setDefault(Locale.UK)
        assertSame(fmt("EEEE d"), fmt("EEEE d"))
    }

    @Test fun followsTheDefaultLocale() {
        val day = LocalDate.of(2026, 11, 22)
        Locale.setDefault(Locale.UK)
        assertEquals("Sunday 22", day.format(fmt("EEEE d")))
        Locale.setDefault(Locale.GERMANY)
        assertEquals("Sonntag 22", day.format(fmt("EEEE d")))
    }
}
