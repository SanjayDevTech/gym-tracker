package com.example.gymtracker

import android.app.Application
import com.example.gymtracker.data.db.ExerciseSeed
import com.example.gymtracker.session.SessionNotification
import com.example.gymtracker.session.SessionTrackingService
import com.example.gymtracker.ui.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GymTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SessionNotification.ensureChannel(this)
        applicationScope.launch {
            ExerciseSeed.ensureSeeded(this@GymTrackerApp, container.repository, container.prefs)
            // Builds before plan cleanup on delete could leave deleted exercises in plans.
            container.repository.removeDeletedExercisesFromPlans()
        }
        // Start the foreground tracker whenever a draft appears; the service stops itself when the
        // draft is cleared. This survives process death — restart hydrates from Room.
        applicationScope.launch {
            container.repository.observeSessionDraft()
                .map { it != null }
                .distinctUntilChanged()
                .collect { hasDraft ->
                    if (hasDraft) SessionTrackingService.start(this@GymTrackerApp)
                }
        }
    }
}
