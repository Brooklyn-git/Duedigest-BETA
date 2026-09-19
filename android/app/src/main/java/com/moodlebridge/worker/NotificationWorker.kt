package com.moodlebridge.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moodlebridge.MainActivity
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.Strings
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext)
        if (!config.notificationsEnabled) return Result.success()

        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK) - 1
        val dayMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val shouldShow = when (config.notificationScheduleType) {
            "daily" -> true
            "weekly" -> true
            "custom" -> {
                val days = parseIntList(config.notificationCustomDays)
                val times = parseTimeList(config.notificationCustomHours)
                val dayOk = days.isEmpty() || currentDay in days
                val timeOk = times.any { (h, m) ->
                    val t = h * 60 + m
                    t in (dayMinutes - 15)..(dayMinutes + 2)
                }
                dayOk && timeOk
            }
            else -> false
        }

        if (shouldShow) showReminder()

        Companion.schedule(applicationContext)
        return Result.success()
    }

    private fun showReminder() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("sync", "true")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext, 4, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val fetchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("sync", "true")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fetchPendingIntent = PendingIntent.getActivity(
            applicationContext, 5, fetchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val lang = applicationContext.let {
            ConfigStore(it).language
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Duedigest")
            .setContentText(Strings.get("notif_body", lang).replace("\\n", "\n"))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(Strings.get("notif_body", lang).replace("\\n", "\n"))
            )
            .addAction(android.R.drawable.ic_menu_upload, Strings.get("fetch", lang), fetchPendingIntent)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(100, notification)
    }

    companion object {
        const val CHANNEL_ID = "due_digest_reminder"
        const val WORK_NAME = "due_digest_notification_worker"

        fun schedule(context: Context) {
            val config = ConfigStore(context)
            if (!config.notificationsEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val delayMinutes = calculateDelay(
                config.notificationScheduleType,
                config.notificationCustomHours,
                config.notificationCustomDays,
            )
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
