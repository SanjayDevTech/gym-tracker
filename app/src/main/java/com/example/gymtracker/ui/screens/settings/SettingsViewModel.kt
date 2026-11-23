package com.example.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.domain.WeeklyTarget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val haptics: Boolean = true,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val volumeMetric: VolumeMetric = VolumeMetric.Sets,
    val weeklyTargetSessions: Int = WeeklyTarget.DEFAULT_TARGET,
)

class SettingsViewModel(
    private val prefs: PreferencesStore,
) : ViewModel() {
    val state: StateFlow<SettingsState> = combine(
        prefs.hapticsEnabled,
        prefs.weightUnit,
        prefs.volumeMetric,
        prefs.weeklyTargetSessions,
    ) { haptics, unit, metric, weeklyTarget ->
        SettingsState(haptics, unit, metric, weeklyTarget)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState(),
    )

    fun setHaptics(value: Boolean) = viewModelScope.launch { prefs.setHapticsEnabled(value) }
    fun setWeightUnit(value: WeightUnit) = viewModelScope.launch { prefs.setWeightUnit(value) }
    fun setVolumeMetric(value: VolumeMetric) = viewModelScope.launch { prefs.setVolumeMetric(value) }
    fun setWeeklyTargetSessions(value: Int) = viewModelScope.launch { prefs.setWeeklyTargetSessions(value) }
}
