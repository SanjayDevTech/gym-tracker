package com.example.gymtracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.rememberAppHaptics

/**
 * Single-select horizontal chip group. With allowDeselect=true, tapping the selected option
 * clears the selection (onSelect(null)).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> ExclusiveButtonGroup(
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    allowDeselect: Boolean = false,
) {
    val haptics = rememberAppHaptics()
    val scroll = rememberScrollState()
    Row(
        modifier = modifier.horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            val isSelected = selected == opt
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.light()
                    onSelect(if (isSelected && allowDeselect) null else opt)
                },
                label = { Text(labelFor(opt)) },
            )
        }
    }
}
