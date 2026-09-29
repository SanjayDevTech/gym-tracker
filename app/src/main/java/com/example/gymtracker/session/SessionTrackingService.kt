package com.example.gymtracker.session

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.example.gymtracker.GymTrackerApp
import com.example.gymtracker.data.GymRepository
import com.example.gymtracker.data.db.SessionDraftEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.example.gymtracker.data.db.SessionDraftPayload

class SessionTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentDraft: SessionDraftEntity? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val repository: GymRepository
        get() = (application as GymTrackerApp).container.repository

    override fun onCreate() {
        super.onCreate()
        SessionNotification.ensureChannel(this)
        val ok = startForegroundCompat(SessionNotification.build(this, "Session in progress", "Starting…"))
        if (!ok) {
            // The system rejected the foreground promotion (e.g. notifications disabled,
            // or background-start restrictions). Persistence still works via Room — we just
            // don't run a sticky notification. Bail out cleanly instead of crashing.
            stopSelf()
            return
        }
        observeDraft()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun observeDraft() {
        scope.launch {
            repository.observeSessionDraft().collectLatest { draft ->
                currentDraft = draft
                if (draft == null) {
                    stopSelfSafely()
                } else {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        val draft = currentDraft ?: return
        val (planName, setCount) = parseDraft(draft)
        val sets = if (setCount == 0) "no sets yet" else "$setCount set${if (setCount == 1) "" else "s"}"
        val title = planName?.let { "Session: $it" } ?: "Session in progress"

        // The elapsed clock is ticked by the system via the chronometer (startedAtMillis), so this
        // only re-posts when the set count changes — not once a second.
        val notification = SessionNotification.build(this, title, sets, startedAtMillis = draft.startedAtMillis)
        NotificationManagerCompat.from(this)
            .notify(SessionNotification.NOTIFICATION_ID, notification)
    }

    private fun parseDraft(draft: SessionDraftEntity): Pair<String?, Int> {
        val payload = runCatching {
            json.decodeFromString(SessionDraftPayload.serializer(), draft.payloadJson)
        }.getOrNull() ?: return null to 0
        val completedSets = payload.groups.sumOf { g -> g.sets.count { it.isCompleted } }
        return payload.planName to completedSets
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: android.app.Notification): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SessionNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(SessionNotification.NOTIFICATION_ID, notification)
        }
    }.isSuccess

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SessionTrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionTrackingService::class.java))
        }
    }
}
