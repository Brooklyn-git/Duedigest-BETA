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

private val BrandIndigo = Color(0xFF818CF8)
private val AccentTeal = Color(0xFF2DD4BF)
private val WidgetBg = Color(0xFF1C1C1E)
private val RowBg = Color(0xFF2C2C2E)
private val PanelBg = Color(0xEE1C1C1E)
private val TextWhite = Color.White
private val TextMuted = Color.White.copy(alpha = 0.7f)
private val TextDim = Color.White.copy(alpha = 0.5f)

class TaskOpacitySliderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            val config = remember { ConfigStore(this@TaskOpacitySliderActivity) }
            var opacity by remember { mutableFloatStateOf(config.getWidgetOpacity(appWidgetId.toString())) }

            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    // Widget preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WidgetBg.copy(alpha = opacity))
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
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "3 pending",
                                color = AccentTeal.copy(alpha = opacity),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFBBF24).copy(alpha = opacity)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Homework 7",
                                    color = TextWhite.copy(alpha = opacity),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "Today",
                                    color = Color(0xFFFBBF24).copy(alpha = opacity),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TextDim.copy(alpha = opacity)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Lab report",
                                    color = TextWhite.copy(alpha = opacity),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "Jun 30",
                                    color = TextDim.copy(alpha = opacity),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandIndigo.copy(alpha = opacity)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Open app",
                                color = TextWhite.copy(alpha = opacity),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    // Bottom slider bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PanelBg)
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
                            Text(
                                text = "0%",
                                color = TextDim,
                                fontSize = 12.sp,
                            )
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
                            Text(
                                text = "100%",
                                color = TextDim,
                                fontSize = 12.sp,
                            )
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
                            Text(
                                text = "Confirm",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
