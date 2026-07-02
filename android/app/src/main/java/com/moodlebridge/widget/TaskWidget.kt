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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BrandIndigo = Color(0xFF818CF8)
private val AccentTeal = Color(0xFF2DD4BF)
private val RedOverdue = Color(0xFFF87171)
private val AmberSoon = Color(0xFFFBBF24)
private val WidgetBg = Color(0xFF1C1C1E)
private val WidgetBgAmoled = Color.Black
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF9CA3AF)
private val TextDim = Color(0xFF6B7280)

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = ConfigStore(context)
        val widgetId = id.toString()
        val isAmoled = config.themeMode == "amoled_dark"
        provideContent {
            TaskWidgetContent(context, config, widgetId, isAmoled)
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
private fun TaskWidgetContent(context: Context, config: ConfigStore, widgetId: String, isAmoled: Boolean = false) {
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
            .clickable(actionRunCallback<OpenAppAction>()),
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
                    TaskWidgetRow(ev, lang)
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

    }
}

@Composable
private fun TaskWidgetRow(event: Event, lang: String = "en") {
    val cal = Calendar.getInstance().apply { timeInMillis = event.timestart * 1000 }
    val now = Calendar.getInstance()
    val diffDays = {
        val c = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
        val n = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        n.set(Calendar.HOUR_OF_DAY, 0); n.set(Calendar.MINUTE, 0); n.set(Calendar.SECOND, 0); n.set(Calendar.MILLISECOND, 0)
        ((c.timeInMillis - n.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    }()
    val dateLabel = when {
        diffDays < 0 -> Strings.get("tasks_overdue", lang)
        diffDays == 0 -> Strings.get("tasks_today", lang)
        diffDays == 1 -> Strings.get("tasks_tomorrow", lang)
        diffDays <= 7 -> Strings.get("tasks_in_days", lang).replace("{n}", diffDays.toString())
        else -> {
            val locale = Locale(lang)
            val sdf = SimpleDateFormat("MMMM dd", locale)
            sdf.format(Date(event.timestart * 1000))
        }
    }
    val dotColor = when {
        diffDays < 0 -> RedOverdue
        diffDays == 0 -> AmberSoon
        diffDays == 1 -> AmberSoon
        else -> TextDim
    }
    val dateColor = when {
        diffDays < 0 -> RedOverdue
        diffDays == 0 -> AmberSoon
        diffDays == 1 -> AmberSoon
        else -> TextSecondary
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp, 8.dp)
                .background(ColorProvider(dotColor)),
            content = {},
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = event.name,
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 13.sp,
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = dateLabel,
            style = TextStyle(
                color = ColorProvider(dateColor),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
