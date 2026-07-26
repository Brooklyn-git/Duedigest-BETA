package com.moodlebridge

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.moodlebridge.data.DesktopConfigStore
import com.moodlebridge.ui.DesktopApp

fun main() = application {
    com.moodlebridge.Log.init(true)
    val config = DesktopConfigStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "DueNest",
        state = WindowState(size = DpSize(960.dp, 700.dp)),
    ) {
        DesktopApp(config)
    }
}
