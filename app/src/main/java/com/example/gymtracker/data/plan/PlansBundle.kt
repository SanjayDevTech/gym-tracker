package com.example.gymtracker.data.plan

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A whole-library plan export: a list of the same name-based [PlanImportPayload]s used for
 *  single-plan sharing, so a bundle file and a single-plan file are interchangeable on import. */
@Serializable
data class PlansBundle(
    val version: Int = 1,
    val kind: String = KIND,
    val exportedAtMillis: Long = 0L,
    val plans: List<PlanImportPayload> = emptyList(),
) {
    companion object {
        const val KIND = "gymtracker-plans"
    }
}

object PlansBundleParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed class Result {
        data class Ok(val bundle: PlansBundle) : Result()
        data class Error(val message: String) : Result()
    }

    /** Accepts either a [PlansBundle] (many plans) or a bare [PlanImportPayload] (one shared plan),
     *  normalising both to a bundle. */
    fun parse(text: String): Result {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.Error("File is empty.")
        return runCatching {
            // A bundle has a "plans" array of plan objects. A single-plan file has no "plans" key,
            // so it decodes to an empty-plans bundle here and we fall back to the single parser.
            val asBundle = runCatching {
                json.decodeFromString(PlansBundle.serializer(), trimmed)
            }.getOrNull()
            if (asBundle != null && asBundle.plans.isNotEmpty()) {
                return@runCatching Result.Ok(asBundle)
            }
            val asSingle = runCatching {
                json.decodeFromString(PlanImportPayload.serializer(), trimmed)
            }.getOrNull()
            if (asSingle != null && asSingle.name.isNotBlank()) {
                return@runCatching Result.Ok(PlansBundle(plans = listOf(asSingle)))
            }
            Result.Error("Not a valid plans file.")
        }.getOrElse { Result.Error("Invalid JSON: ${it.message ?: "unknown error"}") }
    }
}
