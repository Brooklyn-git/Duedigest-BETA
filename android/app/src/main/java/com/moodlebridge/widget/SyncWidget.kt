package com.moodlebridge.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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

private val BrandIndigo = Color(0xFF818CF8)
private val WidgetBg = Color(0xFF1C1C1E)
private val WidgetBgAmoled = Color.Black
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF9CA3AF)

class SyncWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = ConfigStore(context)
        val widgetId = id.toString()
        val isAmoled = config.themeMode == "amoled_dark"
        provideContent {
            SyncWidgetContent(context, config, widgetId, isAmoled)
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
private fun SyncWidgetContent(context: Context, config: ConfigStore, widgetId: String, isAmoled: Boolean = false) {
    val lastSync = config.lastSyncTimestamp
    val message = config.lastSyncMessage
    val count = config.eventCount
    val opacity = config.getWidgetOpacity(widgetId).coerceIn(0f, 1f)
    val bgColor = if (isAmoled) WidgetBgAmoled else WidgetBg

    val timeText = if (lastSync > 0) {
        val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        "Last sync: ${sdf.format(Date(lastSync))}"
    } else {
        "Not synced yet"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(bgColor.copy(alpha = opacity)))
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(10.dp, 10.dp)
                    .background(ColorProvider(BrandIndigo)),
                content = {},
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "DueNest",
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
            )
        }

        Text(
            text = timeText,
            style = TextStyle(
                color = ColorProvider(TextSecondary),
                fontSize = 11.sp,
            ),
            modifier = GlanceModifier.padding(bottom = 2.dp),
        )

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 11.sp,
                ),
                modifier = GlanceModifier.padding(bottom = 4.dp),
            )
        }

        if (count > 0) {
            Row(
                modifier = GlanceModifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$count",
                    style = TextStyle(
                        color = ColorProvider(BrandIndigo),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "upcoming",
                    style = TextStyle(
                        color = ColorProvider(TextSecondary),
                        fontSize = 11.sp,
                    ),
                )
                Text(
                    text = if (count == 1) " event" else " events",
                    style = TextStyle(
                        color = ColorProvider(TextSecondary),
                        fontSize = 11.sp,
                    ),
                )
            }
        } else {
            Text(
                text = "No upcoming events",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 11.sp,
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp),
            )
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(38.dp)
                .background(ColorProvider(BrandIndigo))
                .clickable(actionRunCallback<FetchAndSyncAction>())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Fetch && sync",
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
            )
        }
    }
}
