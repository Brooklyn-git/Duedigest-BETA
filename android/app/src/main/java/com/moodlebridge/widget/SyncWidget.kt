package com.moodlebridge.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.moodlebridge.PasswordPromptActivity
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.worker.SyncWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = ConfigStore(context)
        provideContent {
            SyncWidgetContent(context, config)
        }
    }

    companion object {
        val receiver = SyncWidgetReceiver::class.qualifiedName!!
    }
}

class SyncWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SyncWidget()
}

class FetchAndSyncAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val config = ConfigStore(context)
        if (config.password.isBlank()) {
            val intent = Intent(context, PasswordPromptActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            config.token = ""
            val work = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}

@Composable
private fun SyncWidgetContent(context: Context, config: ConfigStore) {
    val lastSync = config.lastSyncTimestamp
    val message = config.lastSyncMessage
    val count = config.eventCount
    val opacity = config.widgetOpacity.coerceIn(0f, 1f)

    val timeText = if (lastSync > 0) {
        val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        "Last sync: ${sdf.format(Date(lastSync))}"
    } else {
        "Not synced yet"
    }

    val openApp = remember(context) {
        ComponentName(context, com.moodlebridge.MainActivity::class.java)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color(0xFFFFFFFF).copy(alpha = opacity)))
            .clickable(actionStartActivity(openApp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Moodle Bridge",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            modifier = GlanceModifier.padding(bottom = 4.dp),
        )

        Text(
            text = timeText,
            style = TextStyle(fontSize = 11.sp),
            modifier = GlanceModifier.padding(bottom = 2.dp),
        )

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = TextStyle(fontSize = 11.sp),
                modifier = GlanceModifier.padding(bottom = 6.dp),
            )
        }

        if (count > 0) {
            Text(
                text = "$count upcoming event(s)",
                style = TextStyle(fontSize = 11.sp),
                modifier = GlanceModifier.padding(bottom = 8.dp),
            )
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(40.dp)
                .background(ColorProvider(Color(0xFF43A047)))
                .clickable(actionRunCallback<FetchAndSyncAction>())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Fetch && sync",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
            )
        }
    }
}
