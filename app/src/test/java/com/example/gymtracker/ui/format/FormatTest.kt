package com.example.gymtracker.ui.format

import com.example.gymtracker.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    private val n = NBSP

    @Test fun groupsThousandsWithCommas() {
        assertEquals("12,400", grouped(12_400))
        assertEquals("999", grouped(999))
        assertEquals("1,000,000", grouped(1_000_000.4))
        assertEquals("-2,500", grouped(-2_500))
    }

    @Test fun weightDropsTrailingZeroAndUsesUnit() {
        assertEquals("60${n}kg", formatWeight(60f, WeightUnit.Kg))
        assertEquals("62.5${n}kg", formatWeight(62.5f, WeightUnit.Kg))
        assertEquals("220.5${n}lb", formatWeight(100f, WeightUnit.Lb))
        assertEquals(EM_DASH, formatWeight(null, WeightUnit.Kg))
    }

    @Test fun volumeIsInPreferredUnit() {
        assertEquals("12,400${n}kg", formatVolume(12_400.0, WeightUnit.Kg))
        assertEquals("27,337${n}lb", formatVolume(12_400.0, WeightUnit.Lb))
    }

    @Test fun clockAndDuration() {
        assertEquals("0:07", formatClock(7_000))
        assertEquals("1:02:03", formatClock(3_723_000))
        assertEquals("0:00", formatClock(-5))
        assertEquals("48${n}min", formatDuration(48 * 60_000L))
        assertEquals("1${n}h 05${n}min", formatDuration(65 * 60_000L))
        assertEquals(EM_DASH, formatDuration(null))
    }

    @Test fun pluralAgreesWithCount() {
        assertEquals("set", plural(1, "set"))
        assertEquals("sets", plural(0, "set"))
        assertEquals("sets", plural(3, "set"))
        assertEquals("weeks", plural(2L, "week"))
        assertEquals("PR", plural(1, "PR"))
    }
}
