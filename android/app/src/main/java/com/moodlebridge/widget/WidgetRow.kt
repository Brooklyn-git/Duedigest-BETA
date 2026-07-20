package com.moodlebridge.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moodlebridge.data.Event
import com.moodlebridge.data.Strings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WidgetTaskRow(event: Event, lang: String = "en", format24h: Boolean = true) {
    val cal = Calendar.getInstance().apply { timeInMillis = event.timestart * 1000 }
    val nowSeconds = System.currentTimeMillis() / 1000
    val isOverdue = event.timestart <= nowSeconds
    val diffDays = {
        val c = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
        val n = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        n.set(Calendar.HOUR_OF_DAY, 0); n.set(Calendar.MINUTE, 0); n.set(Calendar.SECOND, 0); n.set(Calendar.MILLISECOND, 0)
        ((c.timeInMillis - n.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    }()
    val dateLabel = when {
        isOverdue -> Strings.get("tasks_overdue", lang)
        diffDays == 0 -> "${Strings.get("tasks_today", lang)} ${Strings.formatTimestamp(event.timestart, format24h)}"
        diffDays == 1 -> "${Strings.get("tasks_tomorrow", lang)} ${Strings.formatTimestamp(event.timestart, format24h)}"
        diffDays <= 7 -> Strings.get("tasks_in_days", lang).replace("{n}", diffDays.toString())
        else -> {
            val locale = Locale(lang)
            val sdf = SimpleDateFormat("MMMM dd", locale)
            sdf.format(Date(event.timestart * 1000))
        }
    }
    val dotColor = when {
        isOverdue -> RedOverdue
        diffDays <= 2 -> AmberSoon
        else -> GreenFuture
    }
    val dateColor = when {
        isOverdue -> RedOverdue
        diffDays <= 2 -> AmberSoon
        else -> GreenFuture
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
