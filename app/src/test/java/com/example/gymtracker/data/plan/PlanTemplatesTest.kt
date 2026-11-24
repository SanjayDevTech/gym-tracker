package com.example.gymtracker.data.plan

import com.example.gymtracker.data.db.SeedPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlanTemplatesTest {

    private val seedNames: Set<String> by lazy {
        val raw = File("src/main/res/raw/exercises_seed.json").readText()
        Json { ignoreUnknownKeys = true }.decodeFromString(SeedPayload.serializer(), raw)
            .exercises.mapTo(mutableSetOf()) { it.name }
    }

    private val allPlans get() = PlanTemplates.all.flatMap { t -> t.options.flatMap { it.plans } }

    @Test
    fun everyExerciseIsASeededLibraryName() {
        // Exact spelling, so adding a template matches built-ins and never creates an exercise.
        val missing = allPlans.flatMap { it.exercises }.map { it.name }.filter { it !in seedNames }.distinct()
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun targetsAreValid() {
        allPlans.flatMap { it.exercises }.forEach {
            assertTrue("${it.name} sets", it.sets >= 1)
            assertTrue("${it.name} reps", it.repsLow in 1..it.repsHigh)
        }
    }

    @Test
    fun catalogueIsWellFormed() {
        val templates = PlanTemplates.all
        assertEquals(13, templates.size)
        assertEquals(templates.size, templates.map { it.id }.toSet().size)
        templates.forEach { t ->
            assertTrue(t.id, t.options.isNotEmpty())
            t.options.forEach { option ->
                assertTrue(t.id, option.plans.isNotEmpty())
                // One add never makes two plans with the same name.
                assertEquals(t.id, option.plans.size, option.plans.map { it.name.lowercase() }.toSet().size)
                option.plans.forEach { assertTrue(it.name, it.exercises.isNotEmpty()) }
            }
            assertEquals(t, PlanTemplates.find(t.id))
        }
    }

    @Test
    fun abTemplatesDefaultToAPlusBAndKeepABalancedLegDayForAOnly() {
        val ppl = PlanTemplates.find("ppl")!!
        assertEquals(listOf("Push A", "Pull A", "Legs A", "Push B", "Pull B", "Legs B"), ppl.options[0].plans.map { it.name })
        assertEquals(listOf("Push", "Pull", "Legs"), ppl.options[1].plans.map { it.name })
        // A only's legs day trains both sides; A + B splits the lean across the two days.
        val legs = ppl.options[1].plans.last().exercises.map { it.name }
        assertTrue(legs.containsAll(listOf("Back Squat", "Romanian Deadlift", "Lying Leg Curl", "Leg Extension")))
    }

    @Test
    fun uniqueNameNumbersClashesIgnoringCase() {
        assertEquals("Push A", PlanTemplateInstaller.uniqueName("Push A", setOf("pull a")))
        assertEquals("Push A 2", PlanTemplateInstaller.uniqueName("Push A", setOf("push a")))
        assertEquals("Push A 3", PlanTemplateInstaller.uniqueName("Push A", setOf("push a", "push a 2")))
    }
}
