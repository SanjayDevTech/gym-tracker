package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Spacing

/**
 * Borderless numeral input — design-plan §2 `NumberField`. Replaces the
 * `OutlinedTextField`s in `SetRow`.
 *
 * The value is [AppText.numberL] (28sp, legible from a bench); the [hint] — last
 * session's value — renders at the same size in `onSurfaceVariant`, so a filled
 * field and a hinted one occupy the same space. A 1dp `outlineVariant` rule sits
 * beneath; focused, it becomes a 2dp `primary` rule. No box, no fill.
 *
 * Filtering lives here: digits only, plus one `.` when [decimal]; [onValueChange]
 * receives the filtered string, truncated to [maxLength].
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    decimal: Boolean = false,
    maxLength: Int = 6,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = AppText.numberL,
    textAlign: TextAlign = TextAlign.Start,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val style = textStyle.copy(color = color, textAlign = textAlign)

    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { raw -> onValueChange(filter(raw, decimal).take(maxLength)) },
            enabled = enabled,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && hint != null) {
                        Text(
                            hint,
                            style = style.copy(color = scheme.onSurfaceVariant),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(Spacing.xs))
        // The rule's thickness grows on focus; reserve the 2dp so nothing shifts.
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (focused) 2.dp else Elevation.hairline)
                    .background(if (focused) scheme.primary else scheme.outlineVariant),
            )
        }
    }
}

private fun filter(raw: String, decimal: Boolean): String {
    if (!decimal) return raw.filter(Char::isDigit)
    val out = StringBuilder()
    var dot = false
    raw.forEach { c ->
        when {
            c.isDigit() -> out.append(c)
            (c == '.' || c == ',') && !dot -> { out.append('.'); dot = true }
        }
    }
    return out.toString()
}
