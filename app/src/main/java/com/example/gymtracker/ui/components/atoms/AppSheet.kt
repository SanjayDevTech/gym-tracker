package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gymtracker.ui.theme.Elevation
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * The modal sheet every sheet in the app sits in — §1: `surfaceContainer` fill, [Radius.sheet]
 * top corners, zero tonal elevation, `skipPartiallyExpanded` (the motion table's rule for any
 * sheet taller than half). Content gets the gutter and a bottom inset; sheets never raise sheets
 * (house rule 12).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Elevation.none,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.l),
            content = content,
        )
    }
}
