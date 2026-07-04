package com.moodlebridge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.moodlebridge.data.MoodleApi
import com.moodlebridge.ui.DueNestTheme

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "DueNest") {
        DueNestTheme {
            DesktopApp()
        }
    }
}

@Composable
fun DesktopApp() {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready") }

    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DueNest Desktop", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Moodle URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                status = "Working..."
                try {
                    val token = MoodleApi.login(url, username, password)
                    status = "Logged in! Token: $token"
                } catch (e: Exception) {
                    status = "Error: ${e.message}"
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Login")
            }
            Spacer(Modifier.height(8.dp))
            Text(status)
        }
    }
}
