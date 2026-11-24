package com.example.gymtracker.data.db

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ExerciseSeedVersionTest {
    @Test
    fun readsTheLeadingVersion() {
        assertEquals(7, ExerciseSeed.leadingSeedVersion("{\n  \"seedVersion\": 7,\n  \"exercises\": []}"))
        assertEquals(12, ExerciseSeed.leadingSeedVersion("{\"seedVersion\":12}"))
    }

    @Test
    fun anythingElseFallsBackToTheFullDecode() {
        // Not the first key, not a plain integer, or not an object: null, so the caller decodes in full.
        assertNull(ExerciseSeed.leadingSeedVersion("{\"exercises\": [], \"seedVersion\": 7}"))
        assertNull(ExerciseSeed.leadingSeedVersion("{\"seedVersion\": \"7\", \"exercises\": []}"))
        assertNull(ExerciseSeed.leadingSeedVersion("{\"seedVersion\": 7.5, \"exercises\": []}"))
        assertNull(ExerciseSeed.leadingSeedVersion("[\"seedVersion\", 7]"))
        assertNull(ExerciseSeed.leadingSeedVersion(""))
    }

    @Test
    fun shippedSeedHeadAgreesWithTheFullDecode() {
        val raw = File("src/main/res/raw/exercises_seed.json").readText()
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(SeedPayload.serializer(), raw)
        assertEquals(decoded.seedVersion, ExerciseSeed.leadingSeedVersion(raw))
    }
}
