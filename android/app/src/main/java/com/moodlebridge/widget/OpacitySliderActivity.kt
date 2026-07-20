package com.moodlebridge.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodlebridge.data.ConfigStore

private val PanelBg = Color(0xEE1C1C1E)
private val PanelBgAmoled = Color(0xEE000000)
private val TextWhite = Color.White
private val TextMuted = Color.White.copy(alpha = 0.7f)
private val TextDim = Color.White.copy(alpha = 0.5f)

class OpacitySliderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            val config = remember { ConfigStore(this@OpacitySliderActivity) }
            val isAmoled = config.themeMode == "amoled_dark"
            val widgetBg = if (isAmoled) WidgetBgAmoled else WidgetBg
            val panelBg = if (isAmoled) PanelBgAmoled else PanelBg
            var opacity by remember { mutableFloatStateOf(config.getWidgetOpacity(appWidgetId.toString())) }

            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(widgetBg.copy(alpha = opacity))
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BrandIndigo.copy(alpha = opacity)),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "DueNest",
                                color = TextWhite.copy(alpha = opacity.coerceIn(0.3f, 1f)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Last sync: never",
                            color = TextMuted.copy(alpha = (opacity * 0.8f).coerceIn(0f, 1f)),
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "0",
                                color = BrandIndigo.copy(alpha = opacity),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "upcoming events",
                                color = TextDim.copy(alpha = (opacity * 0.8f).coerceIn(0f, 1f)),
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandIndigo.copy(alpha = opacity)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Fetch && sync",
                                color = TextWhite.copy(alpha = opacity),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(panelBg)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = "Widget opacity",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(text = "0%", color = TextDim, fontSize = 12.sp)
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = TextWhite,
                                    activeTrackColor = BrandIndigo,
                                    inactiveTrackColor = Color(0x66FFFFFF),
                                ),
                            )
                            Text(text = "100%", color = TextDim, fontSize = 12.sp)
                        }
                        Text(
                            text = "${(opacity * 100).toInt()}%",
                            color = TextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                config.setWidgetOpacity(appWidgetId.toString(), opacity)
                                config.lastConfiguredOpacity = opacity
                                val resultIntent = Intent().apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(text = "Confirm", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
