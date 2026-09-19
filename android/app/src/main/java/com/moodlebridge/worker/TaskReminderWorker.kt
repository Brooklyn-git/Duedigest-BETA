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
        showNotification(applicationContext)
        Companion.schedule(applicationContext)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "due_digest_task_reminder"
        const val WORK_NAME = "due_digest_task_reminder_worker"

        fun showNotification(context: Context) {
            val config = ConfigStore(context)
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
                NotificationManagerCompat.from(context).cancel(101)
                return
            }

            val now = Calendar.getInstance()
            val nowSeconds = now.timeInMillis / 1000
            val nowMidnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis / 1000

            val lang = config.language.ifBlank { "en" }
            val count = incomplete.size
            val overdueCount = incomplete.count { it.timestart < nowMidnight }
            val title = if (overdueCount > 0) {
                "$count ${Strings.get("pending", lang)} \u2014 $overdueCount ${Strings.get("tasks_overdue", lang)}"
            } else {
                Strings.get("notif_task_title", lang).replace("{count}", count.toString())
            }

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context, 6, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
            incomplete.take(7).forEach { ev ->
                val label = relativeDateLabel(ev.timestart, nowSeconds, lang, config.notification24hFormat)
                inboxStyle.addLine("${ev.course.ifBlank { "?" }}: ${ev.name} ($label)")
            }
            if (count > 7) {
                inboxStyle.setSummaryText(Strings.get("notif_more", lang).replace("{n}", (count - 7).toString()))
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("Duedigest")
                .setContentText(title)
                .setStyle(inboxStyle)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()

            NotificationManagerCompat.from(context).notify(101, notification)
        }

        private fun relativeDateLabel(timestart: Long, nowSeconds: Long, lang: String, format24h: Boolean): String {
            val hasNoDueDate = timestart == Long.MAX_VALUE
            val isOverdue = !hasNoDueDate && timestart <= nowSeconds
            val diffDays = {
                val c = Calendar.getInstance().apply { timeInMillis = timestart * 1000 }
                val n = Calendar.getInstance().apply { timeInMillis = nowSeconds * 1000 }
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                n.set(Calendar.HOUR_OF_DAY, 0); n.set(Calendar.MINUTE, 0); n.set(Calendar.SECOND, 0); n.set(Calendar.MILLISECOND, 0)
                ((c.timeInMillis - n.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            }()
            return if (hasNoDueDate) {
                Strings.get("no_date", lang)
            } else when {
                isOverdue -> Strings.get("tasks_overdue", lang)
                diffDays == 0 -> "${Strings.get("tasks_today", lang)} ${Strings.formatTimestamp(timestart, format24h)}"
                diffDays == 1 -> "${Strings.get("tasks_tomorrow", lang)} ${Strings.formatTimestamp(timestart, format24h)}"
                else -> {
                    val locale = Locale(lang)
                    val sdf = SimpleDateFormat("MMMM dd", locale)
                    sdf.format(Date(timestart * 1000))
                }
            }
        }

        fun schedule(context: Context) {
            val config = ConfigStore(context)
            if (!config.taskRemindersEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val delayMinutes = calculateDelay(
                config.taskReminderScheduleType,
                config.taskReminderCustomHours,
                config.taskReminderCustomDays,
            )
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
    }
}
