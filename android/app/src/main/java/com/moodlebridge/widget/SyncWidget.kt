package com.moodlebridge.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.dp
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.moodlebridge.MainActivity
import com.moodlebridge.data.ConfigStore
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

@Composable
private fun SyncWidgetContent(context: Context, config: ConfigStore) {
    val lastSync = config.lastSyncTimestamp
    val message = config.lastSyncMessage
    val count = config.eventCount

    val timeText = if (lastSync > 0) {
        val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        "Last sync: ${sdf.format(Date(lastSync))}"
    } else {
        "Not synced yet"
    }

    val intent = remember(context) {
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("sync", true)
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Moodle Bridge",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.dp,
            ),
            modifier = GlanceModifier.padding(bottom = 4.dp),
        )

        Text(
            text = timeText,
            style = TextStyle(fontSize = 11.dp),
            modifier = GlanceModifier.padding(bottom = 2.dp),
        )

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = TextStyle(fontSize = 11.dp),
                modifier = GlanceModifier.padding(bottom = 6.dp),
            )
        }

        if (count > 0) {
            Text(
                text = "$count upcoming event(s)",
                style = TextStyle(fontSize = 11.dp),
                modifier = GlanceModifier.padding(bottom = 8.dp),
            )
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF1976D2))
                .clickable(actionStartActivity(intent))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sync Now",
                style = TextStyle(
                    color = Color.WHITE,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.dp,
                ),
            )
        }
    }
}
