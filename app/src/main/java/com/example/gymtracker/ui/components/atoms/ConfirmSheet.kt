package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Spacing

/**
 * Whole-object deletion confirm: deleting a plan or session is always confirmed.
 * Title, one-line consequence, destructive [AppButton] in `error`, quiet
 * [AppTextButton] cancel. Sits in [AppSheet].
 */
@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
) {
    AppSheet(onDismiss = onDismiss) {
        Text(title, style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.s))
        Text(message, style = AppText.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton(
            text = confirmLabel,
            onClick = onConfirm,
            destructive = true,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
        )
        AppTextButton(
            text = dismissLabel,
            onClick = onDismiss,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        )
    }
}
