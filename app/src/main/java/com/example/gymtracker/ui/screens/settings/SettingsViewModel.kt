package com.example.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val haptics: Boolean = true,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val volumeMetric: VolumeMetric = VolumeMetric.Sets,
)

class SettingsViewModel(
    private val prefs: PreferencesStore,
) : ViewModel() {
    val state: StateFlow<SettingsState> = combine(
        prefs.hapticsEnabled,
        prefs.weightUnit,
        prefs.volumeMetric,
    ) { haptics, unit, metric ->
        SettingsState(haptics, unit, metric)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState(),
    )

    fun setHaptics(value: Boolean) = viewModelScope.launch { prefs.setHapticsEnabled(value) }
    fun setWeightUnit(value: WeightUnit) = viewModelScope.launch { prefs.setWeightUnit(value) }
    fun setVolumeMetric(value: VolumeMetric) = viewModelScope.launch { prefs.setVolumeMetric(value) }
}
