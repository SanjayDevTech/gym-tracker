package com.example.gymtracker.domain

import com.example.gymtracker.data.db.ExerciseEntity

/**
 * Name validation for the custom-exercise form (§4.19, phase 9): the trimmed name must be
 * non-empty and must not match (ignoring case) any other exercise that isn't soft-deleted.
 * A deleted exercise's name is free to reuse — it no longer appears anywhere you pick from.
 */
object ExerciseNameRule {

    sealed interface Result {
        data object Blank : Result
        /** [existingName] is the clashing exercise's name as stored. */
        data class Duplicate(val existingName: String) : Result
        data class Ok(val name: String) : Result
    }

    /**
     * [editingId] is the exercise being edited (it may keep its own name, in any case), or null
     * when creating.
     */
    fun validate(name: String, existing: List<ExerciseEntity>, editingId: Long? = null): Result {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Blank
        val clash = existing.firstOrNull {
            !it.isDeleted && it.id != editingId && it.name.trim().equals(trimmed, ignoreCase = true)
        }
        return if (clash != null) Result.Duplicate(clash.name) else Result.Ok(trimmed)
    }
}
