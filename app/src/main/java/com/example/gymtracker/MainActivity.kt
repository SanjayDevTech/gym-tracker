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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtracker.data.ThemeMode
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
        enableEdgeToEdge()
        val container = (application as GymTrackerApp).container
        handleResumeIntent(intent)
        handleImportIntent(intent)
        setContent {
            val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle(initialValue = null)
            // Nothing until the preference is read (a few ms, under the splash), so a dark-mode
            // user never sees a light frame.
            val mode = themeMode ?: return@setContent
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Status/navigation bar icons follow the app's choice, not the system's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            CompositionLocalProvider(LocalAppContainer provides container) {
                GymTrackerTheme(dark = dark) {
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
            // The application's resolver, not this Activity's: the read runs on the app scope
            // and may outlive the Activity (rotation mid-read of a slow provider), which would
            // otherwise keep the destroyed Activity alive until it finished.
            val resolver = applicationContext.contentResolver
            container.applicationScope.launch {
                val text = runCatching {
                    resolver.openInputStream(uri)?.use { it.bufferedReader(Charsets.UTF_8).readText() }
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
