package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * Whole-object deletion confirm — design-plan §2 `ConfirmSheet`, house rule 10.
 * Title, one-line consequence, destructive [AppButton] in `error`, quiet
 * [AppTextButton] cancel. Sheet in `surfaceContainer` with [Radius.sheet] top
 * corners, zero tonal elevation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Elevation.none,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(title, style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
            Text(message, style = AppText.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppButton(
                text = confirmLabel,
                onClick = onConfirm,
                destructive = true,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
            )
            AppTextButton(
                text = dismissLabel,
                onClick = onDismiss,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
