package com.moodlebridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.worker.SyncWorker
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordPromptActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = ConfigStore(this)
        setContent {
            MaterialTheme(
                colorScheme = when (config.themeMode) {
                    "dark" -> darkColorScheme(); "light" -> lightColorScheme()
                    else -> if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                },
            ) {
                PasswordDialog(
                    onDismiss = { finish() },
                    onSubmit = { password ->
                        config.token = ""
                        val work = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                            .setInputData(androidx.work.Data.Builder().putString("override_password", password).build())
                            .build()
                        androidx.work.WorkManager.getInstance(this@PasswordPromptActivity).enqueue(work)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Moodle Password") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Enter password to sync:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isWorking = true
                    onSubmit(password)
                },
                enabled = password.isNotBlank() && !isWorking,
            ) { Text(if (isWorking) "Working\u2026" else "Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") }
        },
    )
}
