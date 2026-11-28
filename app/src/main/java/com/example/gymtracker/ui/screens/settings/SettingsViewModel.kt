package com.example.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.DEFAULT_REST_SECONDS
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.ThemeMode
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.db.SessionDao
import com.example.gymtracker.domain.WeeklyTarget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** All-time figures for the profile card on You. */
data class Lifetime(
    val sessions: Int = 0,
    val sets: Int = 0,
    val volumeKg: Double = 0.0,
    /** Day of the first logged session; null before any. */
    val sinceEpochDay: Long? = null,
    val bestRunWeeks: Int = 0,
)

data class SettingsState(
    val haptics: Boolean = true,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val volumeMetric: VolumeMetric = VolumeMetric.Sets,
    val weeklyTargetSessions: Int = WeeklyTarget.DEFAULT_TARGET,
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val themeMode: ThemeMode = ThemeMode.Light,
    val name: String = "",
    val lifetime: Lifetime = Lifetime(),
)

class SettingsViewModel(
    private val prefs: PreferencesStore,
    sessionDao: SessionDao,
) : ViewModel() {
    private val training = combine(
        prefs.weightUnit,
        prefs.volumeMetric,
        prefs.weeklyTargetSessions,
        prefs.defaultRestSeconds,
    ) { unit, metric, weeklyTarget, defaultRest -> Training(unit, metric, weeklyTarget, defaultRest) }

    val state: StateFlow<SettingsState> = combine(
        training,
        prefs.hapticsEnabled,
        prefs.themeMode,
        prefs.userName,
        sessionDao.observeSessionVolumes(),
    ) { t, haptics, theme, name, sessions ->
        val days = sessions.map { it.dateEpochDay }
        SettingsState(
            haptics = haptics,
            weightUnit = t.unit,
            volumeMetric = t.metric,
            weeklyTargetSessions = t.weeklyTarget,
            defaultRestSeconds = t.defaultRest,
            themeMode = theme,
            name = name,
            lifetime = Lifetime(
                sessions = sessions.size,
                sets = sessions.sumOf { it.doneSets },
                volumeKg = sessions.sumOf { it.volumeKg },
                sinceEpochDay = days.minOrNull(),
                bestRunWeeks = WeeklyTarget.bestRunOfWeeks(days, t.weeklyTarget),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState(),
    )

    fun setHaptics(value: Boolean) = viewModelScope.launch { prefs.setHapticsEnabled(value) }
    fun setWeightUnit(value: WeightUnit) = viewModelScope.launch { prefs.setWeightUnit(value) }
    fun setVolumeMetric(value: VolumeMetric) = viewModelScope.launch { prefs.setVolumeMetric(value) }
    fun setWeeklyTargetSessions(value: Int) = viewModelScope.launch { prefs.setWeeklyTargetSessions(value) }
    fun setDefaultRestSeconds(value: Int) = viewModelScope.launch { prefs.setDefaultRestSeconds(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(value) }
    fun setName(value: String) = viewModelScope.launch { prefs.setUserName(value) }

    private data class Training(
        val unit: WeightUnit,
        val metric: VolumeMetric,
        val weeklyTarget: Int,
        val defaultRest: Int,
    )
}
