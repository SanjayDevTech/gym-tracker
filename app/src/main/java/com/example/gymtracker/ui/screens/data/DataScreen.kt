package com.example.gymtracker.ui.screens.data

import com.example.gymtracker.ui.theme.Pastel
import com.example.gymtracker.ui.components.atoms.PastelIcon
import com.example.gymtracker.ui.components.atoms.AppOutlinedButton
import com.example.gymtracker.ui.components.atoms.AppCard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.LayoutTemplate
import com.composables.icons.lucide.FileJson
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.ClipboardList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.backup.BackupManager
import com.example.gymtracker.data.plan.PlansTransferManager
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.components.atoms.AppSnackbarHost
import com.example.gymtracker.ui.components.atoms.AppTextButton
import com.example.gymtracker.ui.components.atoms.AppTopBar
import com.example.gymtracker.ui.components.atoms.ConfirmSheet
import com.example.gymtracker.ui.components.atoms.Section
import com.example.gymtracker.ui.components.atoms.Spinner
import com.example.gymtracker.ui.format.plural
import com.example.gymtracker.ui.rememberAppHaptics
import com.example.gymtracker.ui.theme.AppText
import com.example.gymtracker.ui.theme.Radius
import com.example.gymtracker.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Which file operation is running — drives the row spinner and locks every action. */
private enum class DataOp { PlansExport, PlansImport, BackupExport, BackupRestore }

private enum class DataConfirm { PlansImport, BackupRestore }

/**
 * Plans & backup — design-plan §4.15. Four [Section]s, each a one-line description and a foot of
 * text-button actions: IMPORT A PLAN (one reviewed plan → §4.16, always a new plan), PLAN
 * TEMPLATES (→ §4.20, always new plans), ALL PLANS
 * (bundle export/import, upserts by name) and FULL BACKUP (export/restore). The running
 * operation shows a [Spinner] in its own foot; results arrive by snackbar. The two imports that
 * overwrite are confirmed first: plans import by [ConfirmSheet], backup restore by an
 * [AlertDialog] (§5 — the one data-wide hard stop).
 */
@Composable
fun DataScreen(
    onBack: () -> Unit,
    onImportPlan: () -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf<DataOp?>(null) }
    var confirm by remember { mutableStateOf<DataConfirm?>(null) }

    /** Runs [block] as [op], then shows its message. One operation at a time. */
    fun run(op: DataOp, block: suspend () -> String) {
        scope.launch {
            busy = op
            val message = try {
                block()
            } finally {
                busy = null
            }
            snackbar.showSnackbar(message)
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        run(DataOp.BackupExport) {
            val result = container.backup.exportTo(uri)
            if (result.isSuccess) "Backup exported" else "Export failed: ${result.exceptionOrNull()?.message}"
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        run(DataOp.BackupRestore) {
            val result = container.backup.importFrom(uri)
            if (result.isSuccess) "Backup restored" else "Restore failed: ${result.exceptionOrNull()?.message}"
        }
    }

    val plansExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        run(DataOp.PlansExport) {
            val result = container.plansTransfer.exportTo(uri)
            val count = result.getOrNull()
            if (count != null) "Exported $count ${plural(count, "plan")}"
            else "Export failed: ${result.exceptionOrNull()?.message}"
        }
    }

    val plansImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        run(DataOp.PlansImport) {
            val result = container.plansTransfer.importFrom(uri)
            val r = result.getOrNull()
            if (r == null) {
                "Import failed: ${result.exceptionOrNull()?.message}"
            } else {
                buildString {
                    append("Imported ${r.plansTotal} ${plural(r.plansTotal, "plan")}")
                    if (r.plansUpdated > 0) append(" (${r.plansUpdated} updated)")
                    if (r.exercisesCreated > 0) {
                        append(" · ${r.exercisesCreated} new ${plural(r.exercisesCreated, "exercise")}")
                    }
                }
            }
        }
    }

    val idle = busy == null

    Scaffold(
        topBar = { AppTopBar(title = "Plans & backup", onBack = onBack) },
        snackbarHost = { AppSnackbarHost(snackbar) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            DataSection(
                title = "Import a plan",
                icon = Lucide.FileJson,
                pastel = Pastel.Sky,
                description = "Review one plan from a JSON file, matching its exercises to your " +
                    "library, before it's added. Always creates a new plan.",
                running = false,
            ) {
                AppOutlinedButton(
                    text = "Choose file…",
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.light(); onImportPlan() },
                    enabled = idle,
                )
            }

            DataSection(
                title = "Plan templates",
                icon = Lucide.LayoutTemplate,
                pastel = Pastel.Rose,
                description = "Ready-made splits such as Push / Pull / Legs and Upper / Lower. " +
                    "Adding one creates new plans; your own plans aren't changed.",
                running = false,
            ) {
                AppOutlinedButton(
                    text = "Browse",
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.light(); onOpenTemplates() },
                    enabled = idle,
                )
            }

            DataSection(
                title = "All plans",
                icon = Lucide.ClipboardList,
                pastel = Pastel.Lavender,
                description = "Every plan in one JSON file — plans only, no history or settings. " +
                    "Importing updates plans with the same name and adds the rest.",
                running = busy == DataOp.PlansExport || busy == DataOp.PlansImport,
            ) {
                AppOutlinedButton(
                    text = "Export",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptics.light()
                        plansExportLauncher.launch(PlansTransferManager.suggestedFileName())
                    },
                    enabled = idle,
                )
                AppOutlinedButton(
                    text = "Import",
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.light(); confirm = DataConfirm.PlansImport },
                    enabled = idle,
                )
            }

            DataSection(
                title = "Full backup",
                icon = Lucide.Database,
                pastel = Pastel.Mint,
                description = "A JSON snapshot of your plans, history, custom exercises and settings.",
                running = busy == DataOp.BackupExport || busy == DataOp.BackupRestore,
            ) {
                AppOutlinedButton(
                    text = "Export",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptics.light()
                        backupExportLauncher.launch(BackupManager.suggestedFileName())
                    },
                    enabled = idle,
                )
                AppOutlinedButton(
                    text = "Restore",
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.light(); confirm = DataConfirm.BackupRestore },
                    enabled = idle,
                )
            }
        }
    }

    when (confirm) {
        DataConfirm.PlansImport -> ConfirmSheet(
            title = "Import plans?",
            message = "A plan whose name matches one of yours is updated — its exercises replaced. " +
                "New names are added, and exercises missing from your library are recreated as " +
                "custom. History and settings aren't touched.",
            confirmLabel = "Choose file",
            onConfirm = {
                confirm = null
                plansImportLauncher.launch(arrayOf("application/json"))
            },
            onDismiss = { confirm = null },
        )
        // §5: a data-wide restore is a hard modal stop, so it stays a system AlertDialog
        // (surfaceContainer fill, zero elevation) rather than a ConfirmSheet.
        DataConfirm.BackupRestore -> AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Restore a backup?", style = AppText.titleL) },
            text = {
                Text(
                    "Plans, sessions and custom exercises in the file replace the copies you " +
                        "already have; anything not in the file stays. Your settings are restored too.",
                    style = AppText.body,
                )
            },
            confirmButton = {
                AppTextButton(
                    text = "Choose file",
                    onClick = {
                        confirm = null
                        backupRestoreLauncher.launch(arrayOf("application/json"))
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            },
            dismissButton = {
                AppTextButton(
                    text = "Cancel",
                    onClick = { confirm = null },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(Radius.lg),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
        )
        null -> Unit
    }
}

/** Standing head · one-line description · a foot of text actions with the spinner at its end. */
@Composable
private fun DataSection(
    title: String,
    icon: ImageVector,
    pastel: Pastel,
    description: String,
    running: Boolean,
    actions: @Composable RowScope.() -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            PastelIcon(icon, pastel)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = AppText.titleM, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = AppText.bodyS, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (running) {
                Spacer(Modifier.width(Spacing.s))
                Spinner()
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            content = actions,
        )
    }
}
