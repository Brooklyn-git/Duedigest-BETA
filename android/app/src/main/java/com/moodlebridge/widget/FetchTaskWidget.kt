package com.moodlebridge.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moodlebridge.MainActivity
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.Event
import com.moodlebridge.data.Strings
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class FetchTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = ConfigStore(context)
        val widgetId = id.toString()
        val isAmoled = config.themeMode == "amoled_dark"
        provideContent {
            FetchTaskWidgetContent(context, config, widgetId, isAmoled)
        }
    }

    companion object {
        val receiver = FetchTaskWidgetReceiver::class.qualifiedName!!
    }
}

class FetchTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FetchTaskWidget()
}

class OpenAppFromFetchAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

class RefreshFetchTaskWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, WidgetRefreshActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun FetchTaskWidgetContent(context: Context, config: ConfigStore, widgetId: String, isAmoled: Boolean = false) {
    val eventsJson = config.taskEventCache
    val completionJson = config.taskCompletionState
    val events: List<Event> = try {
        Json.decodeFromString(eventsJson)
    } catch (_: Exception) { emptyList() }
    val completionMap: Map<String, Boolean> = try {
        Json.decodeFromString(completionJson)
    } catch (_: Exception) { emptyMap() }

    val opacity = config.getWidgetOpacity(widgetId).coerceIn(0f, 1f)
    val bgColor = if (isAmoled) WidgetBgAmoled else WidgetBg
    val lang = config.language.ifBlank { "en" }

    val unchecked = events.filter { completionMap[it.id] != true }
        .sortedBy { it.timestart }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor.copy(alpha = opacity)))
            .clickable(actionRunCallback<OpenAppFromFetchAction>()),
    ) {
        Column(
            modifier = GlanceModifier
                .padding(12.dp)
                .defaultWeight(),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
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
                    fontSize = 16.sp,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "${unchecked.size} pending",
                style = TextStyle(
                    color = ColorProvider(AccentTeal),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    ),
                )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "\u21bb",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 22.sp,
                ),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshFetchTaskWidgetAction>()),
            )
            }

            if (unchecked.isEmpty()) {
                Text(
                    text = if (events.isEmpty()) "No tasks yet.\nSync to load tasks."
                           else "All tasks completed!",
                    style = TextStyle(
                        color = ColorProvider(TextSecondary),
                        fontSize = 13.sp,
                    ),
                )
            } else {
                val shown = unchecked.take(5)
                val remaining = unchecked.size - shown.size
                for (ev in shown) {
                    WidgetTaskRow(ev, lang, config.notification24hFormat)
                }
                if (remaining > 0) {
                    Text(
                        text = "+$remaining more",
                        style = TextStyle(
                            color = ColorProvider(TextSecondary),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        modifier = GlanceModifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(40.dp)
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
                    fontSize = 14.sp,
                ),
            )
        }
    }
}
