/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import chromahub.rhythm.app.R
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random
import androidx.core.content.edit

/**
 * Periodic worker that sends concise Rhythm Tips reminders and listening tips.
 */
class RhythmPulseNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "RhythmPulseNotificationWorker"
        const val WORK_NAME = "rhythm_pulse_notification_work"
        private const val CHANNEL_ID = "rhythm_pulse"
        private const val NOTIFICATION_ID = 1203
        private const val PREF_NAME = "rhythm_pulse_cache"
        private const val KEY_LAST_MESSAGE_INDEX = "last_message_index"
    }

    private val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val appSettings = AppSettings.getInstance(applicationContext)
            if (!appSettings.rhythmPulseNotificationsEnabled.value) {
                Log.d(TAG, "Rhythm tips notifications disabled, skipping")
                return@withContext Result.success()
            }

            val message = choosePulseMessage()
            sendPulseNotification(message)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Rhythm tips notification", e)
            Result.retry()
        }
    }

    private fun choosePulseMessage(): String {
        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning. Start with one favorite track."
            in 12..16 -> "Good afternoon. A focus playlist can help you stay on track."
            in 17..21 -> "Good evening. A calmer playlist can help you unwind."
            else -> "Late night reminder: keep volume at a comfortable level."
        }

        val listeningTips = listOf(
            "Tip: let a full song play before skipping.",
            "Tip: lower volume if listening for a long session.",
            "Tip: taking short breaks helps reduce listening fatigue.",
            "Tip: album play can provide a smoother listening flow.",
            "Tip: use shuffle when you want variety without manual changes.",
            "Tip: keep your queue organized for easier playback control."
        )

        val candidates = listOf(greeting) + listeningTips
        val lastIndex = prefs.getInt(KEY_LAST_MESSAGE_INDEX, -1)
        var index = Random.nextInt(candidates.size)

        if (candidates.size > 1 && index == lastIndex) {
            index = (index + 1) % candidates.size
        }

        prefs.edit { putInt(KEY_LAST_MESSAGE_INDEX, index) }
        return candidates[index]
    }

    private fun sendPulseNotification(message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val channel = NotificationChannel(
    CHANNEL_ID,
    applicationContext.getString(R.string.service_rhythm_pulse),
    NotificationManager.IMPORTANCE_LOW
).apply {
    description = applicationContext.getString(R.string.service_rhythm_pulse_desc)
    enableVibration(false)
    setShowBadge(false)
}
notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            302,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.service_rhythm_pulse))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
