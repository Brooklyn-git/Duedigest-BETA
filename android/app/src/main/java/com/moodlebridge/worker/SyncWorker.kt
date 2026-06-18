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
import com.moodlebridge.data.MoodleApi
import java.io.File

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext)
        if (!config.isConfigured) {
            config.lastSyncMessage = "Not configured"
            return Result.failure()
        }

        val api = MoodleApi(config.moodleUrl, config.token)

        return try {
            val events = api.fetchEvents()
            val ics = IcsGenerator.generate(events)

            val icsDir = File(applicationContext.cacheDir, "ics")
            icsDir.mkdirs()
            val icsFile = File(icsDir, "calendar.ics")
            icsFile.writeText(ics)

            config.lastSyncTimestamp = System.currentTimeMillis()
            config.lastSyncMessage = "Synced ${events.size} events"
            config.eventCount = events.size

            notifyIcsFile(icsFile, events.size)
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

    private fun notifyIcsFile(icsFile: File, eventCount: Int) {
        val uri = FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.fileprovider",
            icsFile,
        )

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/calendar")
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
