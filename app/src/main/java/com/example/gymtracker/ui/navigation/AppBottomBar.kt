package com.example.gymtracker.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.gymtracker.ui.components.atoms.AppDivider
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Elevation

/**
 * The three bar destinations — design-plan §5, decision 1.
 *
 * Why three and not four: logging is an *action* (Start, Quick log, Resume), not a place; the
 * exercise library is reference material reached from a context, so it is a leaf of Progress.
 * Settings is rare and lives behind Train's ⚙.
 */
enum class BarDestination(
    val label: String,
    val icon: ImageVector,
) {
    Train("Train", Icons.Outlined.FitnessCenter),
    History("History", Icons.Outlined.History),
    Progress("Progress", Icons.Outlined.Insights),
}

/** The bar destination [this] is, or null for a leaf page (which hides the bar). */
fun NavDestination?.barDestination(): BarDestination? = when {
    this == null -> null
    hierarchy.any { it.hasRoute(TrainRoute::class) } -> BarDestination.Train
    hierarchy.any { it.hasRoute(HistoryRoute::class) } -> BarDestination.History
    hierarchy.any { it.hasRoute(ProgressRoute::class) } -> BarDestination.Progress
    else -> null
}

/**
 * Food-tracker's bar, verbatim. Structure is a hairline, not elevation (§1), so the bar draws its
 * own top rule and sits on `background` — `NavigationBar`'s default container is a tonal surface,
 * which on a black canvas reads as a grey slab.
 */
@Composable
fun AppBottomBar(
    current: BarDestination?,
    onSelect: (BarDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        AppDivider()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = Elevation.none,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            BarDestination.entries.forEach { dest ->
                NavigationBarItem(
                    selected = dest == current,
                    onClick = { onSelect(dest) },
                    icon = { Icon(dest.icon, contentDescription = null) },
                    label = { Text(dest.label.uppercase(), style = AppText.eyebrow) },
                    // Labels always shown: a label that appears only when selected is a label you
                    // cannot read before you need it.
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
