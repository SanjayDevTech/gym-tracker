package com.example.gymtracker.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.PreferencesStore
import com.example.gymtracker.data.backup.BackupManager
import com.example.gymtracker.data.db.AppDatabase
import com.example.gymtracker.data.plan.PlansTransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(applicationContext: Context) {
    val database: AppDatabase = AppDatabase.get(applicationContext)
    val repository: GymRepository = GymRepository(database)
    val prefs: PreferencesStore = PreferencesStore(applicationContext)
    val backup: BackupManager = BackupManager(applicationContext, database, repository, prefs)
    val plansTransfer: PlansTransferManager = PlansTransferManager(applicationContext, repository)

    /** Outlives ViewModels — used to flush pending writes that would otherwise be cancelled
     *  when a screen is popped (e.g. session-draft persistence on back press). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One-shot signal that the user wants to jump straight to the in-progress session
     *  (e.g. tapped the foreground-service notification). Consumed by AppNav. */
    private val _resumeSessionRequests = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val resumeSessionRequests: SharedFlow<Unit> = _resumeSessionRequests.asSharedFlow()

    fun requestResumeSession() { _resumeSessionRequests.tryEmit(Unit) }

    /** JSON text of a plan opened/shared from outside the app (e.g. a WhatsApp attachment),
     *  awaiting the "ready to import" review screen. StateFlow (not a one-shot) so a cold start
     *  where the file is read before the UI subscribes still gets picked up. Consumed by the
     *  import screen via [consumePendingImport]. */
    private val _pendingImport = MutableStateFlow<String?>(null)
    val pendingImport: StateFlow<String?> = _pendingImport.asStateFlow()

    fun requestImportPlan(text: String) { _pendingImport.value = text }

    fun consumePendingImport(): String? {
        val text = _pendingImport.value
        _pendingImport.value = null
        return text
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
