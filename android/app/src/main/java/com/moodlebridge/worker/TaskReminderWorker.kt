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
import com.moodlebridge.data.Event
import com.moodlebridge.data.Strings
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext)
        if (!config.taskRemindersEnabled) return Result.success()

        val eventsJson = config.taskEventCache
        val completionJson = config.taskCompletionState
        val events: List<Event> = try {
            Json.decodeFromString(eventsJson)
        } catch (_: Exception) { emptyList() }
        val completionMap: Map<String, Boolean> = try {
            Json.decodeFromString(completionJson)
        } catch (_: Exception) { emptyMap() }

        val incomplete = events.filter { completionMap[it.id] != true }
            .sortedBy { it.timestart }
        if (incomplete.isEmpty()) {
            Companion.schedule(applicationContext)
            return Result.success()
        }

        val now = Calendar.getInstance()
        val nowSeconds = now.timeInMillis / 1000

        val lang = config.language.ifBlank { "en" }
        val count = incomplete.size
        val overdueCount = incomplete.count { it.timestart < nowSeconds }
        val title = if (overdueCount > 0) {
            "$count ${Strings.get("pending", lang)} \u2014 $overdueCount ${Strings.get("tasks_overdue", lang)}"
        } else {
            Strings.get("notif_task_title", lang).replace("{count}", count.toString())
        }

        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext, 6, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
        incomplete.take(7).forEach { ev ->
            val label = relativeDateLabel(ev.timestart, nowSeconds, lang)
            inboxStyle.addLine("${ev.course.ifBlank { "?" }}: ${ev.name} ($label)")
        }
        if (count > 7) {
            inboxStyle.setSummaryText(Strings.get("notif_more", lang).replace("{n}", (count - 7).toString()))
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("DueNest")
            .setContentText(title)
            .setStyle(inboxStyle)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(101, notification)

        Companion.schedule(applicationContext)
        return Result.success()
    }

    private fun relativeDateLabel(timestart: Long, nowSeconds: Long, lang: String): String {
        val diffDays = ((timestart - nowSeconds) / (3600 * 24)).toInt()
        return when {
            diffDays < 0 -> Strings.get("tasks_overdue", lang)
            diffDays == 0 -> Strings.get("tasks_today", lang)
            diffDays == 1 -> Strings.get("tasks_tomorrow", lang)
            else -> {
                val locale = Locale(lang)
                val sdf = SimpleDateFormat("MMMM dd", locale)
                sdf.format(Date(timestart * 1000))
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "due_nest_task_reminder"
        const val WORK_NAME = "due_nest_task_reminder_worker"

        fun schedule(context: Context) {
            val config = ConfigStore(context)
            if (!config.taskRemindersEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val delayMinutes = calculateDelay(config)
            val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
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

            return when (config.taskReminderScheduleType) {
                "daily" -> 24 * 60L
                "weekly" -> 7 * 24 * 60L
                "custom" -> {
                    val times = parseTimeList(config.taskReminderCustomHours)
                    val days = parseIntList(config.taskReminderCustomDays).toSet()
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
