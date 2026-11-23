package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.format.NBSP
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Sizes
import com.example.gymtracker.ui.theme.Spacing

/**
 * The rest clock's transport — design-plan §2 `RestTransport` (S5-g).
 *
 * `− 15 s` · a 56dp [Radius.md] primary-filled `Skip` · `+ 15 s`. Skip is one of the few
 * fills §1 allows: it is the action under the thumb between every set. The step buttons are
 * quiet `onSurface` text — they adjust a value, they don't record work.
 */
@Composable
fun RestTransport(
    onMinus: () -> Unit,
    onSkip: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    stepSeconds: Int = 15,
    enabled: Boolean = true,
    skipEnabled: Boolean = enabled,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        AppTextButton(
            text = "−${NBSP}$stepSeconds${NBSP}s",
            onClick = onMinus,
            enabled = enabled,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onSkip,
            enabled = skipEnabled,
            shape = RoundedCornerShape(Radius.md),
            // Sizes, not gaps: the transport's centre key is wider than a pill button.
            modifier = Modifier
                .heightIn(min = Sizes.transportButton)
                .widthIn(min = 128.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = Elevation.none,
                pressedElevation = Elevation.none,
                focusedElevation = Elevation.none,
                hoveredElevation = Elevation.none,
                disabledElevation = Elevation.none,
            ),
        ) { Text("Skip", style = AppText.label) }
        AppTextButton(
            text = "+${NBSP}$stepSeconds${NBSP}s",
            onClick = onPlus,
            enabled = enabled,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
