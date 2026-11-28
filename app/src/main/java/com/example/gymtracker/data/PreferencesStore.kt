package com.example.gymtracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gymtracker.domain.WeeklyTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gym_prefs")

const val DEFAULT_REST_SECONDS = 90

/**
 * Every preference is read from the one DataStore file, so a write to any key re-emits [data] for
 * all of them. Each flow below is `distinctUntilChanged`, so a write to one key (the logger saving
 * the default rest, the seed version) doesn't wake up every collector of the others (Progress,
 * History, Train recompute their whole combine on each emission).
 */
class PreferencesStore(private val context: Context) {
    private object Keys {
        val Haptics = booleanPreferencesKey("haptics_enabled")
        val WeightUnit = stringPreferencesKey("weight_unit")
        val VolumeMetric = stringPreferencesKey("volume_metric")
        val SeedVersion = intPreferencesKey("seed_version")
        val DefaultRestSeconds = intPreferencesKey("default_rest_seconds")
        val WeeklyTargetSessions = intPreferencesKey("weekly_target_sessions")
        val NotificationPermissionAsked = booleanPreferencesKey("notification_permission_asked")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val Onboarded = booleanPreferencesKey("onboarded")
        val UserName = stringPreferencesKey("user_name")
    }

    /** Light / Dark / Match system (You → Appearance). Light for a new install, as in Foodprint. */
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { ThemeMode.fromName(it[Keys.ThemeMode]) }
        .distinctUntilChanged()

    suspend fun setThemeMode(value: ThemeMode) =
        context.dataStore.edit { it[Keys.ThemeMode] = value.name }

    /** False until the first-run flow finishes; the app then opens on Home. */
    val onboarded: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.Onboarded] ?: false }
        .distinctUntilChanged()

    suspend fun setOnboarded() = context.dataStore.edit { it[Keys.Onboarded] = true }

    /** First name for the Home greeting; blank when skipped. */
    val userName: Flow<String> = context.dataStore.data
        .map { it[Keys.UserName].orEmpty() }
        .distinctUntilChanged()

    suspend fun setUserName(value: String) = context.dataStore.edit { it[Keys.UserName] = value.trim() }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.Haptics] ?: true }
        .distinctUntilChanged()

    val weightUnit: Flow<WeightUnit> = context.dataStore.data
        .map { prefs -> prefs[Keys.WeightUnit]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.Kg }
        .distinctUntilChanged()

    val volumeMetric: Flow<VolumeMetric> = context.dataStore.data
        .map { prefs -> prefs[Keys.VolumeMetric]?.let { runCatching { VolumeMetric.valueOf(it) }.getOrNull() } ?: VolumeMetric.Sets }
        .distinctUntilChanged()

    val seedVersion: Flow<Int> = context.dataStore.data
        .map { it[Keys.SeedVersion] ?: 0 }
        .distinctUntilChanged()

    /** Rest target a new workout starts with (§6.3); today's constant, 90s, until changed. */
    val defaultRestSeconds: Flow<Int> = context.dataStore.data
        .map { it[Keys.DefaultRestSeconds] ?: DEFAULT_REST_SECONDS }
        .distinctUntilChanged()

    suspend fun setDefaultRestSeconds(value: Int) =
        context.dataStore.edit { it[Keys.DefaultRestSeconds] = value }

    /** Sessions a week counted as "on target" (decision 7); 1–7, default 3. */
    val weeklyTargetSessions: Flow<Int> = context.dataStore.data
        .map { (it[Keys.WeeklyTargetSessions] ?: WeeklyTarget.DEFAULT_TARGET).coerceIn(WeeklyTarget.MIN_TARGET, WeeklyTarget.MAX_TARGET) }
        .distinctUntilChanged()

    suspend fun setWeeklyTargetSessions(value: Int) =
        context.dataStore.edit {
            it[Keys.WeeklyTargetSessions] = value.coerceIn(WeeklyTarget.MIN_TARGET, WeeklyTarget.MAX_TARGET)
        }

    /** Whether the logger has already asked for POST_NOTIFICATIONS (§6.1 step 3: once, on the
     *  first workout, not at launch). */
    val notificationPermissionAsked: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.NotificationPermissionAsked] ?: false }
        .distinctUntilChanged()

    suspend fun setNotificationPermissionAsked() =
        context.dataStore.edit { it[Keys.NotificationPermissionAsked] = true }

    suspend fun setHapticsEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.Haptics] = value }

    suspend fun setWeightUnit(value: WeightUnit) =
        context.dataStore.edit { it[Keys.WeightUnit] = value.name }

    suspend fun setVolumeMetric(value: VolumeMetric) =
        context.dataStore.edit { it[Keys.VolumeMetric] = value.name }

    suspend fun setSeedVersion(value: Int) =
        context.dataStore.edit { it[Keys.SeedVersion] = value }
}
