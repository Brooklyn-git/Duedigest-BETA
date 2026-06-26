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
            .setContentTitle("DueNest")
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
        const val CHANNEL_ID = "due_nest_reminder"
        const val WORK_NAME = "due_nest_notification_worker"

        fun schedule(context: Context) {
            val config = ConfigStore(context)
            if (!config.notificationsEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val delayMinutes = calculateDelay(config)
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

        private fun calculateDelay(config: ConfigStore): Long {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val currentDay = now.get(Calendar.DAY_OF_WEEK) - 1

            return when (config.notificationScheduleType) {
                "daily" -> 24 * 60L
                "weekly" -> 7 * 24 * 60L
                "custom" -> {
                    val times = parseTimeList(config.notificationCustomHours)
                    val days = parseIntList(config.notificationCustomDays).toSet()
                    val allDays = days.isEmpty()
                    if (times.isEmpty()) return 60L

                    val sortedTimes = times.map { (h, m) -> h * 60 + m }.sorted()

                    for (t in sortedTimes) {
                        if (t > currentMinutes) return (t - currentMinutes).toLong()
                    }

                    for (offset in 1..7) {
                        val d = (currentDay + offset) % 7
                        if (allDays || d in days) {
                            val t = sortedTimes.first()
                            return (offset * 24 * 60L + t - currentMinutes).coerceAtLeast(1)
                        }
                    }
                    7 * 24 * 60L
                }
                else -> 24 * 60L
            }
        }

        private fun parseIntList(value: String): List<Int> =
            value.split(",").mapNotNull {
                val t = it.trim()
                if (t.contains(":")) t.substringBefore(":").toIntOrNull() else t.toIntOrNull()
            }

        private fun parseTimeList(value: String): List<Pair<Int, Int>> =
            value.split(",").mapNotNull {
                val t = it.trim()
                val parts = t.split(":")
                val h = parts[0].toIntOrNull() ?: return@mapNotNull null
                val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                h to m
            }
    }
}
