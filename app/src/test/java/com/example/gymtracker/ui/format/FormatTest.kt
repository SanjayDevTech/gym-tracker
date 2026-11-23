package com.example.gymtracker.ui.format

import com.example.gymtracker.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    private val n = NBSP

    @Test fun groupsThousandsWithNbsp() {
        assertEquals("12${n}400", grouped(12_400))
        assertEquals("999", grouped(999))
        assertEquals("1${n}000${n}000", grouped(1_000_000.4))
        assertEquals("-2${n}500", grouped(-2_500))
    }

    @Test fun weightDropsTrailingZeroAndUsesUnit() {
        assertEquals("60${n}kg", formatWeight(60f, WeightUnit.Kg))
        assertEquals("62.5${n}kg", formatWeight(62.5f, WeightUnit.Kg))
        assertEquals("220.5${n}lb", formatWeight(100f, WeightUnit.Lb))
        assertEquals(EM_DASH, formatWeight(null, WeightUnit.Kg))
    }

    @Test fun volumeIsInPreferredUnit() {
        assertEquals("12${n}400${n}kg", formatVolume(12_400.0, WeightUnit.Kg))
        assertEquals("27${n}337${n}lb", formatVolume(12_400.0, WeightUnit.Lb))
    }

    @Test fun clockAndDuration() {
        assertEquals("0:07", formatClock(7_000))
        assertEquals("1:02:03", formatClock(3_723_000))
        assertEquals("0:00", formatClock(-5))
        assertEquals("48${n}min", formatDuration(48 * 60_000L))
        assertEquals("1${n}h 05${n}min", formatDuration(65 * 60_000L))
        assertEquals(EM_DASH, formatDuration(null))
    }
}
