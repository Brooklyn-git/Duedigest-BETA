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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moodlebridge.MainActivity
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.Event
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = ConfigStore(context)
        val widgetId = id.toString()
        provideContent {
            TaskWidgetContent(context, config, widgetId)
        }
    }

    companion object {
        val receiver = TaskWidgetReceiver::class.qualifiedName!!
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TaskWidget()
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun TaskWidgetContent(context: Context, config: ConfigStore, widgetId: String) {
    val eventsJson = config.taskEventCache
    val completionJson = config.taskCompletionState
    val events: List<Event> = try {
        Json.decodeFromString(eventsJson)
    } catch (_: Exception) { emptyList() }
    val completionMap: Map<String, Boolean> = try {
        Json.decodeFromString(completionJson)
    } catch (_: Exception) { emptyMap() }

    val opacity = config.getWidgetOpacity(widgetId).coerceIn(0f, 1f)

    val unchecked = events.filter { completionMap[it.id] != true }
        .sortedBy { it.timestart }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1C1C1E).copy(alpha = opacity))),
    ) {
        Column(
            modifier = GlanceModifier
                .padding(12.dp)
                .defaultWeight(),
        ) {
            Text(
                text = "Pending Tasks",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                modifier = GlanceModifier.padding(bottom = 6.dp),
            )

            if (unchecked.isEmpty()) {
                Text(
                    text = if (events.isEmpty()) "No tasks yet.\nSync to load tasks."
                           else "All tasks completed!",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF888888)),
                        fontSize = 11.sp,
                    ),
                )
            } else {
                Text(
                    text = "${unchecked.size} pending",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF888888)),
                        fontSize = 11.sp,
                    ),
                    modifier = GlanceModifier.padding(bottom = 6.dp),
                )
                val shown = unchecked.take(6)
                val remaining = unchecked.size - shown.size
                for (ev in shown) {
                    TaskWidgetRow(ev)
                }
                if (remaining > 0) {
                    Text(
                        text = "+$remaining more",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 10.sp,
                        ),
                        modifier = GlanceModifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(36.dp)
                .background(ColorProvider(Color(0xFF1976D2)))
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Open app",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun TaskWidgetRow(event: Event) {
    val cal = Calendar.getInstance().apply { timeInMillis = event.timestart * 1000 }
    val now = Calendar.getInstance()
    val diffDays = ((cal.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    val dateLabel = when {
        diffDays < 0 -> "\u26A0 Overdue"
        diffDays == 0 -> "Today"
        diffDays == 1 -> "Tomorrow"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(event.timestart * 1000))
        }
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = "\u2022 ",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFF5252)),
                fontSize = 12.sp,
            ),
        )
        Text(
            text = event.name,
            style = TextStyle(
                color = ColorProvider(Color(0xFFFFFFFF)),
                fontSize = 11.sp,
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        Text(
            text = dateLabel,
            style = TextStyle(
                color = ColorProvider(Color(0xFF888888)),
                fontSize = 10.sp,
            ),
            modifier = GlanceModifier.padding(start = 4.dp),
        )
    }
}
