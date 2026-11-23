package com.example.gymtracker.ui.screens.plans.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/**
 * One-field sheet for plan text — New plan (Train foot), Rename and Note (Plan editor). Title,
 * an optional caption, a borderless field on a hairline (no outlined box, §1), then the primary
 * action and a quiet Cancel. [allowBlank] lets the note be cleared.
 */
@Composable
fun TextEntrySheet(
    title: String,
    initial: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    caption: String? = null,
    singleLine: Boolean = true,
    allowBlank: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    var text by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val canConfirm = allowBlank || text.isNotBlank()

    AppSheet(onDismiss = onDismiss) {
        Text(title, style = AppText.titleL, color = scheme.onSurface)
        if (caption != null) {
            Text(caption, style = AppText.bodyS, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.l))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = singleLine,
            textStyle = AppText.body.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(text) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { inner ->
                Box(Modifier.padding(vertical = Spacing.s)) {
                    if (text.isEmpty()) Text(placeholder, style = AppText.body, color = scheme.onSurfaceVariant)
                    inner()
                }
            },
        )
        AppDivider()
        AppButton(
            text = confirmLabel,
            onClick = { onConfirm(text) },
            enabled = canConfirm,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
        )
        AppTextButton(
            text = "Cancel",
            onClick = onDismiss,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        )
    }
}
