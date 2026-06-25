package com.moodlebridge.worker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.IcsGenerator
import com.moodlebridge.data.MarkdownGenerator
import com.moodlebridge.data.MoodleApi
import com.moodlebridge.data.PathResolver
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext)
        if (!config.isConfigured) {
            config.lastSyncMessage = "Not configured"
            notifyNeedConfig()
            return Result.failure()
        }

        val overridePassword = inputData.getString("override_password") ?: ""
        val pw = if (overridePassword.isNotBlank()) overridePassword else config.password
        if (pw.isBlank()) {
            config.lastSyncMessage = "Password needed"
            notifyNeedPassword()
            return Result.failure()
        }

        val newToken = try {
            withContext(Dispatchers.IO) {
                MoodleApi.login(config.moodleUrl, config.username, pw)
            }
        } catch (e: Exception) {
            config.lastSyncMessage = "Auth failed: ${e.message}"
            return Result.failure()
        }
        config.token = newToken

        val api = MoodleApi(config.moodleUrl, config.token)

        return try {
            val events = api.fetchEvents(config.fetchDaysBack, config.fetchLimit)

            if (config.icsEnabled) {
                val icsContent = IcsGenerator.generate(events)
                // Always write to cache for FileProvider auto-open
                File(applicationContext.cacheDir, "ics/calendar.ics").also {
                    it.parentFile?.mkdirs(); it.writeText(icsContent)
                }
                // Also write to user path if configured
                if (config.icsPath.isNotBlank()) {
                    PathResolver.writeIcs(applicationContext, config.icsPath, icsContent)
                }
                openIcsInCalendar()
            }
            if (config.logseqEnabled) {
                PathResolver.writeMarkdownFiles(applicationContext,
                    config.logseqPath.ifBlank { "${applicationContext.cacheDir}/logseq" },
                    "logseq",
                    MarkdownGenerator.generateLogseq(events))
            }
            if (config.obsidianEnabled) {
                PathResolver.writeMarkdownFiles(applicationContext,
                    config.obsidianPath.ifBlank { "${applicationContext.cacheDir}/obsidian" },
                    "obsidian",
                    MarkdownGenerator.generateObsidian(events))
            }

            config.lastSyncTimestamp = System.currentTimeMillis()
            config.lastSyncMessage = "Synced ${events.size} events"
            config.eventCount = events.size

            notifyResult(events.size)
            Result.success()
        } catch (e: Exception) {
            config.lastSyncMessage = "Error: ${e.message ?: "Unknown"}"
            if (e.message?.contains("Login", ignoreCase = true) == true ||
                e.message?.contains("token", ignoreCase = true) == true
            ) {
                config.lastSyncMessage = "Auth failed — reconfigure"
            }
            Result.retry()
        }
    }

    private fun openIcsInCalendar() {
        val icsFile = File(applicationContext.cacheDir, "ics/calendar.ics")
        if (!icsFile.exists()) return
        val uri = FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.fileprovider", icsFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/calendar")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        applicationContext.startActivity(intent)
    }

    private fun notifyNeedPassword() {
        val intent = Intent(applicationContext, com.moodlebridge.MainActivity::class.java).apply {
            putExtra("sync", "true")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 2, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Moodle Sync")
            .setContentText("Password needed — tap to enter")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(2, notification)
    }

    private fun notifyNeedConfig() {
        val intent = Intent(applicationContext, com.moodlebridge.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 3, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Moodle Sync")
            .setContentText("Configure the app first")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(3, notification)
    }

    private fun notifyResult(eventCount: Int) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    applicationContext,
                    "${applicationContext.packageName}.fileprovider",
                    File(applicationContext.cacheDir, "ics/calendar.ics"),
                ),
                "text/calendar",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Moodle Sync Complete")
            .setContentText("$eventCount events — tap to open in calendar")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1, notification)
    }

    companion object {
        const val CHANNEL_ID = "moodle_sync"
    }
}
