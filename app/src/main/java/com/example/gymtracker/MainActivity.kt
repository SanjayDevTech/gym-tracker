package com.example.gymtracker

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.gymtracker.session.SessionNotification
import com.example.gymtracker.ui.LocalAppContainer
import com.example.gymtracker.ui.navigation.AppNav
import com.example.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run BEFORE super.onCreate so the splash window installs correctly.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Dark-only app: system bar icons are always light, whatever the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        val container = (application as GymTrackerApp).container
        handleResumeIntent(intent)
        handleImportIntent(intent)
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                GymTrackerTheme {
                    AppNav()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleResumeIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleResumeIntent(intent: Intent?) {
        val resume = intent?.getBooleanExtra(SessionNotification.EXTRA_RESUME_SESSION, false) == true
        if (resume) {
            (application as GymTrackerApp).container.requestResumeSession()
            // Consume so a config change doesn't replay the deep-link.
            intent.removeExtra(SessionNotification.EXTRA_RESUME_SESSION)
        }
    }

    /**
     * Handles a plan JSON opened (ACTION_VIEW) or shared (ACTION_SEND, e.g. from WhatsApp) into
     * the app. Reads the content off the main thread and hands it to the container, which drives
     * navigation to the "ready to import" review screen. Nothing is written until the user
     * confirms there.
     */
    private fun handleImportIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_IMPORT_HANDLED, false)) return

        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        val sharedText = if (intent.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        if (uri == null && sharedText.isNullOrBlank()) return
        // Mark consumed so a config change (rotation) doesn't replay the deep-link.
        intent.putExtra(EXTRA_IMPORT_HANDLED, true)

        val container = (application as GymTrackerApp).container
        if (uri != null) {
            container.applicationScope.launch {
                val text = runCatching {
                    contentResolver.openInputStream(uri)?.use { it.bufferedReader(Charsets.UTF_8).readText() }
                }.getOrNull()
                if (!text.isNullOrBlank()) container.requestImportPlan(text)
            }
        } else if (!sharedText.isNullOrBlank()) {
            container.requestImportPlan(sharedText)
        }
    }

    private companion object {
        const val EXTRA_IMPORT_HANDLED = "com.example.gymtracker.IMPORT_HANDLED"
    }
}
