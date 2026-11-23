package com.example.gymtracker.session

import android.app.Notification
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
import com.example.gymtracker.data.db.SessionDraftPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SessionTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json { ignoreUnknownKeys = true }

    // Rest timer alert: the (base, target) the pending job was scheduled for, the job itself,
    // and the base an alert has already been posted for (one post per rest).
    private var restKey: Pair<Long, Int>? = null
    private var restJob: Job? = null
    private var postedRestBase: Long = 0L

    private val repository: GymRepository
        get() = (application as GymTrackerApp).container.repository

    override fun onCreate() {
        super.onCreate()
        SessionNotification.ensureChannel(this)
        val ok = startForegroundCompat(SessionNotification.build(this, "Workout", "Starting…"))
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
                if (draft == null) {
                    cancelRestAlert()
                    stopSelfSafely()
                } else {
                    val payload = parsePayload(draft)
                    updateNotification(draft, payload)
                    scheduleRestAlert(payload)
                }
            }
        }
    }

    private fun updateNotification(draft: SessionDraftEntity, payload: SessionDraftPayload?) {
        val sets = payload?.groups?.flatMap { it.sets }.orEmpty()
        val title = SessionNotification.title(payload?.planName, sets.count { it.isCompleted }, sets.size)
        val restBase = payload?.lastSetCompletedAtMillis?.takeIf { it > 0L }
        // Resting: count up from the last completed set; otherwise the session clock. Either way
        // the system ticks the chronometer, so this re-posts only when the draft changes.
        val notification = if (payload != null && restBase != null) {
            SessionNotification.build(this, title, SessionNotification.restText(payload.restTargetSeconds), restBase)
        } else {
            SessionNotification.build(this, title, "Workout in progress", draft.startedAtMillis)
        }
        notifySafely(SessionNotification.NOTIFICATION_ID, notification)
    }

    /**
     * One Rest timer post at `lastSetCompletedAt + restTarget` (decision 5). Re-keyed whenever the
     * draft's rest base or target changes; a new base (the next completed set, Restart) or a
     * cleared one (Skip) also withdraws an alert already shown. Posts only when the logger isn't
     * resumed — on screen, the dock's own haptic does the job. A target already in the past when
     * scheduled (service restart, target shortened below the elapsed rest) never fires.
     */
    private fun scheduleRestAlert(payload: SessionDraftPayload?) {
        val base = payload?.lastSetCompletedAtMillis ?: 0L
        val target = payload?.restTargetSeconds ?: 0
        val key = base to target
        if (key == restKey) return
        if (base != restKey?.first) cancelRestAlert()
        restKey = key
        restJob?.cancel()
        restJob = null
        if (payload == null || base <= 0L) return

        val wait = base + target * 1000L - System.currentTimeMillis()
        if (wait <= 0L) return
        val planName = payload.planName
        restJob = scope.launch {
            delay(wait)
            if (!LoggerPresence.resumed.value && postedRestBase != base) {
                postedRestBase = base
                notifySafely(
                    SessionNotification.REST_NOTIFICATION_ID,
                    SessionNotification.buildRestReached(this@SessionTrackingService, target, planName),
                )
            }
        }
    }

    private fun cancelRestAlert() {
        restJob?.cancel()
        restJob = null
        NotificationManagerCompat.from(this).cancel(SessionNotification.REST_NOTIFICATION_ID)
    }

    private fun notifySafely(id: Int, notification: Notification) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        // POST_NOTIFICATIONS can be revoked at any time; a denied post is simply not shown.
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun parsePayload(draft: SessionDraftEntity): SessionDraftPayload? = runCatching {
        json.decodeFromString(SessionDraftPayload.serializer(), draft.payloadJson)
    }.getOrNull()

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cancelRestAlert()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: Notification): Boolean = runCatching {
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
