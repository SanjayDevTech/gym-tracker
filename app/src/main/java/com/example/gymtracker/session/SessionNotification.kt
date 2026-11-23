package com.example.gymtracker.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.gymtracker.MainActivity
import com.example.gymtracker.R
import com.example.gymtracker.ui.format.formatClock

/**
 * The two notifications a running workout owns — §4.18.
 *
 * - **Session in progress** (ongoing, silent): `Push A · 7 of 18 sets`; while resting the text is
 *   `Rest · target 1:30` and the chronometer counts up from the last completed set, otherwise it
 *   counts the session's elapsed time.
 * - **Rest timer** (decision 5): one post when rest reaches its target while the logger is not in
 *   the foreground. High importance so it vibrates; no sound, no actions.
 */
object SessionNotification {
    const val CHANNEL_ID = "session_in_progress"
    const val NOTIFICATION_ID = 0xA17

    const val REST_CHANNEL_ID = "rest_timer"
    const val REST_NOTIFICATION_ID = 0xA18

    private val RestVibration = longArrayOf(0, 250, 150, 250)

    /** Creates both channels. Idempotent — the system keeps a channel's user-edited settings. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val session = NotificationChannel(
            CHANNEL_ID,
            "Session in progress",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing notification while a workout session is being logged."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        val rest = NotificationChannel(
            REST_CHANNEL_ID,
            "Rest timer",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Vibrates once when your rest reaches its target while the app is in the background."
            setShowBadge(false)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = RestVibration
        }
        manager.createNotificationChannels(listOf(session, rest))
    }

    /**
     * Builds the ongoing session notification. The OS renders the clock itself via a chronometer
     * based on [chronometerBaseMillis] — the last completed set while resting, else the session
     * start — so we never wake up once a second; it is rebuilt only when the text changes.
     */
    fun build(
        context: Context,
        title: String,
        content: String,
        chronometerBaseMillis: Long? = null,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                if (chronometerBaseMillis != null) {
                    setWhen(chronometerBaseMillis)
                    setShowWhen(true)
                    setUsesChronometer(true)
                } else {
                    setShowWhen(false)
                }
            }
            .setContentIntent(resumeIntent(context))
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Bypass Android's ~10s delay for short-running foreground services so the
            // notification appears the moment the session starts, not only after resume.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    /** `Push A · 7 of 18 sets`; a workout without a plan reads `Workout · …`. */
    fun title(planName: String?, done: Int, total: Int): String =
        "${planName ?: "Workout"} · $done of $total set${if (total == 1) "" else "s"}"

    /** `Rest · target 1:30` — the session notification text while resting. */
    fun restText(targetSeconds: Int): String = "Rest · target ${formatClock(targetSeconds * 1000L)}"

    /** The one-shot Rest timer alert. Tapping it returns to the logger; no actions. */
    fun buildRestReached(context: Context, targetSeconds: Int, planName: String?): Notification =
        NotificationCompat.Builder(context, REST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Rest done · ${formatClock(targetSeconds * 1000L)}")
            .setContentText("${planName ?: "Workout"} · time for the next set")
            .setContentIntent(resumeIntent(context))
            .setAutoCancel(true)
            .setSilent(false)
            .setVibrate(RestVibration)
            .setSound(null)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    private fun resumeIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_RESUME_SESSION, true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    const val EXTRA_RESUME_SESSION = "extra_resume_session"
}
