package com.example.gymtracker.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.gymtracker.MainActivity
import com.example.gymtracker.R

object SessionNotification {
    const val CHANNEL_ID = "session_in_progress"
    const val NOTIFICATION_ID = 0xA17

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Session in progress",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing notification while a workout session is being logged."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the ongoing session notification. When [startedAtMillis] is non-null, the OS renders
     * the elapsed-time clock itself via a chronometer — so we never have to wake up once a second
     * to advance it. The notification then only needs rebuilding when [content] (the set count)
     * actually changes.
     */
    fun build(
        context: Context,
        title: String,
        content: String,
        startedAtMillis: Long? = null,
    ): android.app.Notification {
        val resumeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_RESUME_SESSION, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Let the system tick the elapsed clock instead of waking us every second.
            .apply {
                if (startedAtMillis != null) {
                    setWhen(startedAtMillis)
                    setShowWhen(true)
                    setUsesChronometer(true)
                } else {
                    setShowWhen(false)
                }
            }
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Bypass Android's ~10s delay for short-running foreground services so the
            // notification appears the moment the session starts, not only after resume.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    const val EXTRA_RESUME_SESSION = "extra_resume_session"
}
