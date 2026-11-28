package com.example.gymtracker.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.AppTheme
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/**
 * The sheet every short sheet in the app sits in: `surfaceContainer` fill, [Radius.sheet] top
 * corners, the app's grab handle, always fully expanded (a tall sheet never opens with its footer
 * off-screen). Content gets the gutter and a bottom inset; cards inside switch to
 * [com.example.gymtracker.ui.theme.AppExtras.cardInSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppModalSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.l),
            content = content,
        )
    }
}

/** A short rounded grab bar, used by every sheet in place of Material's. */
@Composable
fun SheetHandle() {
    Box(
        Modifier
            .padding(top = Spacing.m, bottom = Spacing.s)
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(MaterialTheme.colorScheme.outline),
    )
}

/** Sheet title on the left, a round close button on the right. */
@Composable
fun SheetTitleRow(title: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = AppText.titleL,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        RoundIconButton(icon = Lucide.X, contentDescription = "Close", onClick = onClose)
    }
}

/**
 * `ModalBottomSheet` with the app's handle, fill and corners, and cards inside it switched to
 * the in-sheet card colour. Same parameters as the Material one, so call sites swap by name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = shape,
        dragHandle = { SheetHandle() },
    ) {
        CompositionLocalProvider(
            LocalCardColor provides AppTheme.extras.cardInSheet,
            LocalBehind provides containerColor,
        ) {
            content()
        }
    }
}
