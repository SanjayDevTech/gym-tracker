package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.rememberAppHaptics

@Composable
fun NumberStepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 1,
    max: Int = 99,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledIconButton(
            onClick = {
                if (value > min) { onChange(value - 1); haptics.light() }
            },
            enabled = value > min,
        ) { Icon(Icons.Outlined.Remove, contentDescription = "Decrement") }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        FilledIconButton(
            onClick = {
                if (value < max) { onChange(value + 1); haptics.light() }
            },
            enabled = value < max,
        ) { Icon(Icons.Outlined.Add, contentDescription = "Increment") }
    }
}
