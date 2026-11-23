package com.example.gymtracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gym_prefs")

const val DEFAULT_REST_SECONDS = 90

class PreferencesStore(private val context: Context) {
    private object Keys {
        val Haptics = booleanPreferencesKey("haptics_enabled")
        val WeightUnit = stringPreferencesKey("weight_unit")
        val VolumeMetric = stringPreferencesKey("volume_metric")
        val SeedVersion = intPreferencesKey("seed_version")
        val DefaultRestSeconds = intPreferencesKey("default_rest_seconds")
    }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.Haptics] ?: true }

    val weightUnit: Flow<WeightUnit> = context.dataStore.data
        .map { prefs -> prefs[Keys.WeightUnit]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.Kg }

    val volumeMetric: Flow<VolumeMetric> = context.dataStore.data
        .map { prefs -> prefs[Keys.VolumeMetric]?.let { runCatching { VolumeMetric.valueOf(it) }.getOrNull() } ?: VolumeMetric.Sets }

    val seedVersion: Flow<Int> = context.dataStore.data
        .map { it[Keys.SeedVersion] ?: 0 }

    /** Rest target a new workout starts with (§6.3); today's constant, 90s, until changed. */
    val defaultRestSeconds: Flow<Int> = context.dataStore.data
        .map { it[Keys.DefaultRestSeconds] ?: DEFAULT_REST_SECONDS }

    suspend fun setDefaultRestSeconds(value: Int) =
        context.dataStore.edit { it[Keys.DefaultRestSeconds] = value }

    suspend fun setHapticsEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.Haptics] = value }

    suspend fun setWeightUnit(value: WeightUnit) =
        context.dataStore.edit { it[Keys.WeightUnit] = value.name }

    suspend fun setVolumeMetric(value: VolumeMetric) =
        context.dataStore.edit { it[Keys.VolumeMetric] = value.name }

    suspend fun setSeedVersion(value: Int) =
        context.dataStore.edit { it[Keys.SeedVersion] = value }
}
