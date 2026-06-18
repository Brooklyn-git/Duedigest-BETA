package com.moodlebridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.ui.SettingsScreen
import com.moodlebridge.worker.SyncWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigStore(this)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    .launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        config = config,
                        autoSync = intent?.getBooleanExtra("sync", false) == true,
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            SyncWorker.CHANNEL_ID,
            "Moodle Sync",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

@Composable
private fun MainScreen(config: ConfigStore, autoSync: Boolean) {
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(autoSync) {
        if (autoSync && config.isConfigured && !isSyncing) {
            isSyncing = true
        }
    }

    if (!config.isConfigured) {
        SettingsScreen(config = config)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Moodle Bridge",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = config.moodleUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            val lastSync = config.lastSyncTimestamp
            val message = config.lastSyncMessage
            val count = config.eventCount

            if (lastSync > 0) {
                val sdf = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.getDefault())
                Text("Last synced: ${sdf.format(Date(lastSync))}")
            } else {
                Text("Not synced yet")
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("Error") || message.startsWith("Auth"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (count > 0) {
                Spacer(Modifier.height(4.dp))
                Text("$count upcoming event(s)")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    isSyncing = true
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
                    WorkManager.getInstance(context).enqueue(request)
                },
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSyncing) "Syncing..." else "Sync Now")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    config.clear()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Configuration")
            }
        }
    }
}
