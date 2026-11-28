package com.example.gymtracker.data.backup

import com.example.gymtracker.data.SetType
import com.example.gymtracker.data.ThemeMode
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.db.SessionDraftPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Formats written before phase 3 must still decode (additive, optional fields only). */
class PayloadCompatTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun v2BackupWithoutStartedAtStillImports() {
        val v2 = """
            {"version":2,"exportedAtMillis":1,"prefs":{"haptics":true,"weightUnit":"Kg","seedVersion":3},
             "customExercises":[],"plans":[],
             "sessions":[{"clientUuid":"a","planClientUuid":null,"dateEpochDay":20000,
               "loggedAtMillis":5,"notes":"","sets":[
                 {"exerciseClientUuid":"e","setIndex":0,"repsDone":8,"weightKg":60.0,"isCompleted":true}]}]}
        """.trimIndent()
        val payload = json.decodeFromString(BackupPayload.serializer(), v2)
        assertEquals(2, payload.version)
        assertNull(payload.sessions.single().startedAtMillis)
        // v3-only fields are absent → null, which restore reads as "leave as is" / Working.
        assertNull(payload.sessions.single().sets.single().setType)
        assertNull(payload.prefs.volumeMetric)
        assertNull(payload.prefs.weeklyTargetSessions)
        assertNull(payload.prefs.theme)
        assertNull(payload.prefs.userName)
        assertNull(payload.prefs.defaultRestSeconds)
    }

    @Test
    fun v3BackupRoundTripsItsOptionalFields() {
        val payload = BackupPayload(
            exportedAtMillis = 1,
            prefs = PrefsSnapshot(
                haptics = true,
                weightUnit = "Kg",
                seedVersion = 1,
                volumeMetric = VolumeMetric.Tonnage.name,
                weeklyTargetSessions = 5,
                theme = ThemeMode.Dark.name,
                userName = "Sam",
                defaultRestSeconds = 120,
            ),
            customExercises = emptyList(),
            plans = emptyList(),
            sessions = listOf(
                SessionSnapshot(
                    "a", null, 1, 10, "",
                    listOf(SetSnapshot("e", 0, 5, 100f, true, setType = SetType.Failure.name)),
                    startedAtMillis = 4,
                ),
            ),
        )
        val back = json.decodeFromString(BackupPayload.serializer(), json.encodeToString(BackupPayload.serializer(), payload))
        assertEquals(3, back.version)
        assertEquals(4L, back.sessions.single().startedAtMillis)
        assertEquals(SetType.Failure.name, back.sessions.single().sets.single().setType)
        assertEquals(VolumeMetric.Tonnage.name, back.prefs.volumeMetric)
        assertEquals(5, back.prefs.weeklyTargetSessions)
        assertEquals(ThemeMode.Dark.name, back.prefs.theme)
        assertEquals("Sam", back.prefs.userName)
        assertEquals(120, back.prefs.defaultRestSeconds)
    }

    @Test
    fun draftWithoutRestTargetDefaultsTo90() {
        val old = """{"planName":"Push A","dateEpochDay":20000,"groups":[],"lastSetCompletedAtMillis":0}"""
        val payload = json.decodeFromString(SessionDraftPayload.serializer(), old)
        assertEquals(90, payload.restTargetSeconds)
    }
}
