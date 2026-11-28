package com.example.gymtracker.domain

import com.example.gymtracker.data.db.ExerciseEntity
import java.util.Locale

/**
 * Name validation for the custom-exercise form: the trimmed name must be
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
            !it.isDeleted && it.id != editingId && normalizedName(it.name) == normalizedName(trimmed)
        }
        return if (clash != null) Result.Duplicate(clash.name) else Result.Ok(trimmed)
    }

    /**
     * The one key every name-based exercise match uses (form duplicate check, plan import, backup
     * restore): surrounding whitespace dropped, case folded with the root locale. Kept in Kotlin
     * rather than SQL because SQLite's LOWER/NOCASE only fold ASCII and TRIM only strips spaces.
     */
    fun normalizedName(name: String): String = name.trim().lowercase(Locale.ROOT)

    /**
     * The live exercise in [candidates] whose name matches [name] under [normalizedName], or null.
     * Soft-deleted rows never match. If an older database already holds several live rows that
     * differ only by case/space, the pick is deterministic: a built-in beats a custom, then the
     * lowest id wins.
     */
    fun findLiveMatch(name: String, candidates: List<ExerciseEntity>): ExerciseEntity? {
        val key = normalizedName(name)
        if (key.isEmpty()) return null
        return candidates
            .filter { !it.isDeleted && normalizedName(it.name) == key }
            .minWithOrNull(compareBy<ExerciseEntity>({ it.isCustom }, { it.id }))
    }
}
