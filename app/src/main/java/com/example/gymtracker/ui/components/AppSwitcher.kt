package com.example.gymtracker.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Utensils
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppSheet
import com.example.gymtracker.ui.components.atoms.cardColor
import com.example.gymtracker.ui.components.atoms.pressable
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing

/** Foodprint, the sibling food diary. Its launcher activity is opened by package. */
const val FOODPRINT_PACKAGE = "com.example.foodtracker"

/**
 * `[Lift | Food]` — the switch between the two apps, built so the same pill can sit in Foodprint
 * with the selection flipped. This app's half is the ink capsule; tapping the other half opens
 * Foodprint, or explains how to get it when it isn't installed.
 */
@Composable
fun AppSwitcher(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    var missing by remember { mutableStateOf(false) }
    Row(
        modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(cardColor())
            .padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwitchHalf(Lucide.Dumbbell, "Lift", selected = true, onClick = {})
        SwitchHalf(Lucide.Utensils, "Food", selected = false, onClick = {
            haptics.light()
            if (!openFoodprint(context)) missing = true
        })
    }
    if (missing) {
        AppSheet(onDismiss = { missing = false }) {
            Text("Foodprint isn't installed", style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(Spacing.s))
            Text(
                "Foodprint is the food diary that pairs with Lift Up: meals, 30 nutrients and streaks, " +
                    "private on your phone. Install it and this switch takes you straight there.",
                style = AppText.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(Spacing.xl))
            AppButton("Got it", onClick = { missing = false }, modifier = Modifier.padding(bottom = Spacing.s))
        }
    }
}

@Composable
private fun SwitchHalf(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (selected) scheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = if (selected) "$label, this app" else "Open ${if (label == "Food") "Foodprint" else label}"
            }
            .pressable(enabled = !selected, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = AppText.label, color = ink)
    }
}

/** Opens Foodprint's launcher activity; false when it isn't installed. */
fun openFoodprint(context: Context): Boolean {
    val intent = context.packageManager.getLaunchIntentForPackage(FOODPRINT_PACKAGE) ?: return false
    return try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
