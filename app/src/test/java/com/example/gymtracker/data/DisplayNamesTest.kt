package com.example.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNamesTest {
    @Test fun splitsCamelCaseIntoSentenceCase() {
        assertEquals("Front delts", Muscle.FrontDelts.displayName)
        assertEquals("Upper back", Muscle.UpperBack.displayName)
        assertEquals("Chest", Muscle.Chest.displayName)
        assertEquals("Bodyweight", Equipment.Bodyweight.displayName)
        assertEquals("Compound", MovementType.Compound.displayName)
        assertEquals("Warm-up", SetType.Warmup.displayName)
    }
}
