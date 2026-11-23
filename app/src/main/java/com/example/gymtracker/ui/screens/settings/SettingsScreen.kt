package com.example.gymtracker.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.VolumeMetric
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppButton
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.UnderlineSelector
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.launch
import com.example.gymtracker.ui.theme.Radius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    // Which backup op is running ("export"/"import"/null) — drives the spinners and disables the
    // buttons so a backup can't be fired twice or both at once.
    var busyOp by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showPlansImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busyOp = "export"
                try {
                    val result = container.backup.exportTo(uri)
                    snackbar.showSnackbar(
                        if (result.isSuccess) "Backup exported"
                        else "Export failed: ${result.exceptionOrNull()?.message}"
                    )
                } finally {
                    busyOp = null
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busyOp = "import"
                try {
                    val result = container.backup.importFrom(uri)
                    snackbar.showSnackbar(
                        if (result.isSuccess) "Backup imported"
                        else "Import failed: ${result.exceptionOrNull()?.message}"
                    )
                } finally {
                    busyOp = null
                }
            }
        }
    }

    val plansExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busyOp = "plans-export"
                try {
                    val result = container.plansTransfer.exportTo(uri)
                    val count = result.getOrNull() ?: 0
                    snackbar.showSnackbar(
                        if (result.isSuccess) "Exported $count plan${if (count == 1) "" else "s"}"
                        else "Export failed: ${result.exceptionOrNull()?.message}"
                    )
                } finally {
                    busyOp = null
                }
            }
        }
    }

    val plansImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busyOp = "plans-import"
                try {
                    val result = container.plansTransfer.importFrom(uri)
                    val r = result.getOrNull()
                    snackbar.showSnackbar(
                        when {
                            r == null -> "Import failed: ${result.exceptionOrNull()?.message}"
                            else -> buildString {
                                append("Imported ${r.plansTotal} plan${if (r.plansTotal == 1) "" else "s"}")
                                if (r.plansUpdated > 0) append(" (${r.plansUpdated} updated)")
                                if (r.exercisesCreated > 0) {
                                    append(" · ${r.exercisesCreated} new exercise${if (r.exercisesCreated == 1) "" else "s"}")
                                }
                            }
                        }
                    )
                } finally {
                    busyOp = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Settings", onBack = onBack)
        },
        snackbarHost = { AppSnackbarHost(snackbar) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            SettingsCard(icon = Icons.Outlined.Scale, title = "Weight unit") {
                UnderlineSelector(
                    options = WeightUnit.entries.toList(),
                    selected = state.weightUnit,
                    onSelect = viewModel::setWeightUnit,
                    label = { it.label },
                )
            }

            SettingsCard(icon = Icons.Outlined.BarChart, title = "Volume metric") {
                UnderlineSelector(
                    options = VolumeMetric.entries.toList(),
                    selected = state.volumeMetric,
                    onSelect = viewModel::setVolumeMetric,
                    label = { it.label },
                )
                Text(
                    "Sets counts each working set once, so bodyweight work (pull-ups, planks) counts " +
                        "fully. Tonnage weighs reps × load and ignores bodyweight sets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsCard(icon = Icons.Outlined.Vibration, title = "Haptics") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subtle vibration", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Light feedback on taps, confirmations, and reorders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.haptics, onCheckedChange = { viewModel.setHaptics(it) })
                }
            }

            SettingsCard(icon = Icons.Outlined.ImportExport, title = "Plans") {
                Text(
                    "Export all your plans to a JSON file, or import them back. Plans only — no " +
                        "history or settings. Re-importing updates plans with the same name instead " +
                        "of duplicating them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    AppButton(
                        text = if (busyOp == "plans-export") "Working…" else "Export all",
                        onClick = {
                            plansExportLauncher.launch(
                                com.example.gymtracker.data.plan.PlansTransferManager.suggestedFileName()
                            )
                        },
                        enabled = busyOp == null,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CloudUpload,
                    )
                    AppOutlinedButton(
                        text = if (busyOp == "plans-import") "Working…" else "Import all",
                        onClick = { showPlansImportConfirm = true },
                        enabled = busyOp == null,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CloudDownload,
                    )
                }
            }

            SettingsCard(icon = Icons.Outlined.FitnessCenter, title = "Backup") {
                Text(
                    "Save a JSON snapshot of all your plans, history, and custom exercises.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    AppButton(
                        text = if (busyOp == "export") "Working…" else "Export",
                        onClick = {
                            exportLauncher.launch(com.example.gymtracker.data.backup.BackupManager.suggestedFileName())
                        },
                        enabled = busyOp == null,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CloudUpload,
                    )
                    AppOutlinedButton(
                        text = if (busyOp == "import") "Working…" else "Import",
                        onClick = { showImportConfirm = true },
                        enabled = busyOp == null,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CloudDownload,
                    )
                }
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "This adds the backup's plans, sessions, and custom exercises to what you " +
                        "already have — it doesn't replace anything. Importing a backup you've " +
                        "already restored can create duplicate plans and sessions.",
                )
            },
            confirmButton = {
                AppTextButton(
                    text = "Choose file",
                    onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                )
            },
            dismissButton = {
                AppTextButton(text = "Cancel", onClick = { showImportConfirm = false })
            },
        )
    }

    if (showPlansImportConfirm) {
        AlertDialog(
            onDismissRequest = { showPlansImportConfirm = false },
            icon = { Icon(Icons.Outlined.ImportExport, contentDescription = null) },
            title = { Text("Import plans?") },
            text = {
                Text(
                    "This adds the file's plans to yours. A plan whose name matches one you " +
                        "already have is updated (its exercises replaced); new names are added. " +
                        "Exercises the file references that aren't in your library are recreated " +
                        "as custom. Your history and settings aren't touched.",
                )
            },
            confirmButton = {
                AppTextButton(
                    text = "Choose file",
                    onClick = {
                        showPlansImportConfirm = false
                        plansImportLauncher.launch(arrayOf("application/json"))
                    },
                )
            },
            dismissButton = {
                AppTextButton(text = "Cancel", onClick = { showPlansImportConfirm = false })
            },
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}
