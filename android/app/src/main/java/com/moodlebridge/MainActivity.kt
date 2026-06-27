package com.moodlebridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.Event
import com.moodlebridge.data.IcsGenerator
import com.moodlebridge.data.MarkdownGenerator
import com.moodlebridge.data.MoodleApi
import com.moodlebridge.data.PathResolver
import com.moodlebridge.data.Strings
import com.moodlebridge.worker.NotificationWorker
import com.moodlebridge.worker.SyncWorker
import androidx.work.WorkManager
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = ConfigStore(this)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { MainContent(config = config, autoSync = intent?.getStringExtra("sync") == "true") }
    }
    private fun createNotificationChannel() {
        val syncCh = NotificationChannel(SyncWorker.CHANNEL_ID, "Moodle Sync", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(syncCh)
        val reminderCh = NotificationChannel(NotificationWorker.CHANNEL_ID, "DueNest Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(reminderCh)
    }
}

@Composable
private fun MoodleBridgeTheme(themeMode: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (when (themeMode) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }) darkColorScheme() else lightColorScheme(),
        content = { Surface(Modifier.fillMaxSize()) { content() } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MainContent(config: ConfigStore, autoSync: Boolean) {
    var themeMode by remember { mutableStateOf(config.themeMode) }
    var selectedLang by remember { mutableStateOf(config.language.ifBlank { "en" }) }
    val lang = selectedLang
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var url by remember { mutableStateOf(config.moodleUrl) }
    var username by remember { mutableStateOf(config.username) }
    var password by remember { mutableStateOf(config.password) }
    var tz by remember { mutableStateOf(config.timezone.ifBlank { "UTC" }) }
    var passwordVisible by remember { mutableStateOf(false) }
    var icsEnabled by remember { mutableStateOf(config.icsEnabled) }
    var logseqEnabled by remember { mutableStateOf(config.logseqEnabled) }
    var obsidianEnabled by remember { mutableStateOf(config.obsidianEnabled) }
    var icsPath by remember { mutableStateOf(config.icsPath) }
    var logseqPath by remember { mutableStateOf(config.logseqPath) }
    var obsidianPath by remember { mutableStateOf(config.obsidianPath) }
    var daysBackText by remember { mutableStateOf(config.fetchDaysBack.toString()) }
    var limitText by remember { mutableStateOf(config.fetchLimit.toString()) }
    var savePw by remember { mutableStateOf(config.savePassword) }

    var selectedTab by remember { mutableIntStateOf(1) }
    var showSettings by remember { mutableStateOf(false) }
    var showOutputSettings by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var errorDialogMsg by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf(config.lastSyncMessage) }
    val logLines = remember { mutableStateListOf<String>() }
    var tzExpanded by remember { mutableStateOf(false) }
    var notifEnabled by remember { mutableStateOf(config.notificationsEnabled) }
    var notifScheduleType by remember { mutableStateOf(config.notificationScheduleType) }
    var notif24hFormat by remember { mutableStateOf(config.notification24hFormat) }
    fun parseDayList(s: String): List<Int> = s.split(",").mapNotNull { it.trim().toIntOrNull() }
    fun parseHourList(s: String): List<String> = s.split(",").mapNotNull { t ->
        val v = t.trim()
        if (v.contains(":")) v else v.toIntOrNull()?.let { h -> if (h < 24) "${h}:00" else "$h" }
    }
    var notifCustomDaysList by remember { mutableStateOf(parseDayList(config.notificationCustomDays)) }
    var notifCustomHoursList by remember { mutableStateOf(parseHourList(config.notificationCustomHours)) }
    var fetchedEvents by remember { mutableStateOf(listOf<Event>()) }
    var taskCompletionMap by remember { mutableStateOf(
        try { Json.decodeFromString<Map<String, Boolean>>(config.taskCompletionState) } catch (_: Exception) { emptyMap() }
    ) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }
    var tasksEnabled by remember { mutableStateOf(config.tasksEnabled) }
    var tasksOutputPath by remember { mutableStateOf(config.tasksOutputPath) }

    fun parseHour(s: String): Int {
        val parts = s.split(":")
        return parts[0].toIntOrNull() ?: 0
    }
    fun parseMinute(s: String): Int {
        val parts = s.split(":")
        return if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
    }
    fun formatTime(s: String): String {
        val h = parseHour(s); val m = parseMinute(s)
        if (notif24hFormat) return String.format("%02d:%02d", h, m)
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return String.format("%d:%02d %s", h12, m, ampm)
    }

    fun addLog(msg: String) { logLines.add(msg) }

    fun saveCompletionMap() {
        config.taskCompletionState = Json.encodeToString(taskCompletionMap)
    }

    fun regenerateTasksFile() {
        if (!tasksEnabled || tasksOutputPath.isBlank()) return
        val content = if (config.lastSyncTimestamp == 0L && fetchedEvents.isEmpty()) {
            MarkdownGenerator.generateIntroMd(lang)
        } else {
            MarkdownGenerator.generateTasksList(fetchedEvents, taskCompletionMap)
        }
        PathResolver.writeIcs(context, tasksOutputPath, content)
    }

    fun doSync(pw: String) {
        isWorking = true; logLines.clear(); statusText = ""
        scope.launch {
            doFetch(context, config, url, username, pw, tz, savePw,
                icsEnabled, logseqEnabled, obsidianEnabled, icsPath, logseqPath, obsidianPath,
                daysBackText, limitText, lang, tasksEnabled, tasksOutputPath,
                { addLog(it) }, { statusText = it }, { errorDialogMsg = it },
                { isWorking = false },
                { events ->
                    fetchedEvents = events; expandedTaskId = null
                    config.taskEventCache = Json.encodeToString(events)
                    regenerateTasksFile()
                })
        }
    }

    MoodleBridgeTheme(themeMode = themeMode) {
        if (showOutputSettings) {
            OutputSettingsPage(
                icsEnabled = icsEnabled, onIcsEnabledChange = { icsEnabled = it },
                logseqEnabled = logseqEnabled, onLogseqEnabledChange = { logseqEnabled = it },
                obsidianEnabled = obsidianEnabled, onObsidianEnabledChange = { obsidianEnabled = it },
                icsPath = icsPath, onIcsPathChange = { icsPath = it },
                logseqPath = logseqPath, onLogseqPathChange = { logseqPath = it },
                obsidianPath = obsidianPath, onObsidianPathChange = { obsidianPath = it },
                daysBackText = daysBackText, onDaysBackChange = { daysBackText = it },
                limitText = limitText, onLimitChange = { limitText = it },
                tasksEnabled = tasksEnabled, onTasksEnabledChange = { tasksEnabled = it; config.tasksEnabled = it; regenerateTasksFile() },
                tasksOutputPath = tasksOutputPath, onTasksPathChange = { tasksOutputPath = it; config.tasksOutputPath = it; regenerateTasksFile() },
                lang = lang,
                onBack = { showOutputSettings = false },
                onSettings = { showSettings = true },
            )
        } else {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(Strings.get("app_title", lang), style = MaterialTheme.typography.titleLarge)
                            Text(Strings.get("subtitle", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding ->
            LaunchedEffect(autoSync) {
                if (autoSync && config.isConfigured && !isWorking) {
                    if (config.password.isBlank()) {
                        val i = Intent(context, PasswordPromptActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(i)
                    } else {
                        config.token = ""
                        doSync(config.password)
                    }
                }
            }
            Column(Modifier.padding(padding).fillMaxSize()) {
                // ── Tabs ────────────────────────────────────────
                TabRow(selectedTabIndex = selectedTab) {
                    listOf(
                        Strings.get("connection", lang),
                        Strings.get("tasks", lang),
                    ).forEachIndexed { i, t ->
                        Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                            text = { Text(t, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.height(56.dp))
                    }
                }

                // ── Tab content ─────────────────────────────────
                Column(Modifier.weight(3f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when (selectedTab) {
                        0 -> ConnectionTabContent(url = url, onUrlChange = { url = it },
                            username = username, onUsernameChange = { username = it },
                            password = password, onPasswordChange = { password = it },
                            passwordVisible = passwordVisible, onPasswordVisibleChange = { passwordVisible = it },
                            tz = tz, onTzChange = { tz = it }, tzExpanded = tzExpanded,
                            onTzExpandedChange = { tzExpanded = it }, lang = lang)
                        else -> TasksTabContent(
                            events = fetchedEvents, completionMap = taskCompletionMap,
                            expandedId = expandedTaskId, onExpandedChange = { expandedTaskId = it },
                            onToggleCompletion = { id ->
                                taskCompletionMap = taskCompletionMap.toMutableMap().also { it[id] = !(it[id] ?: false) }
                                saveCompletionMap()
                                regenerateTasksFile()
                            },
                            onClearCompleted = {
                                taskCompletionMap = emptyMap()
                                saveCompletionMap()
                                regenerateTasksFile()
                                expandedTaskId = null
                            },
                            lang = lang,
                            showIntro = config.lastSyncTimestamp == 0L && fetchedEvents.isEmpty())
                    }
                }

                // ── Save password ───────────────────────────────
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = savePw, onCheckedChange = { savePw = it })
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.get("store_pw", lang), style = MaterialTheme.typography.bodySmall)
                }

                // ── Buttons ─────────────────────────────────────
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { doSync(password) },
                        enabled = url.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !isWorking,
                        modifier = Modifier.weight(1f)) {
                        Text(if (isWorking) Strings.get("working", lang) else Strings.get("fetch", lang))
                    }
                    OutlinedButton(onClick = { shareIcs(context, config) }, modifier = Modifier.weight(1f)) { Text(Strings.get("share_ics", lang)) }
                }

                // ── Progress ────────────────────────────────────
                if (isWorking) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)); Spacer(Modifier.height(4.dp)) }

                // ── Log ─────────────────────────────────────────
                if (selectedTab != 1) {
                    Text(Strings.get("log", lang), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                    Card(Modifier.fillMaxWidth().weight(2f).padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                            items(logLines.toList()) { line -> Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                        }
                    }
                }

                // ── Status ──────────────────────────────────────
                Text(statusText.ifBlank { Strings.get("ready", lang) }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
        } // end else

        // ── Error dialog ────────────────────────────────────────
        if (errorDialogMsg != null) {
            AlertDialog(onDismissRequest = { errorDialogMsg = null }, title = { Text(Strings.get("error", lang)) },
                text = { Text(errorDialogMsg ?: "") },
                confirmButton = { TextButton(onClick = { errorDialogMsg = null }) { Text("OK") } })
        }
        if (showSettings) {
            AlertDialog(onDismissRequest = { showSettings = false },
                title = { Text(Strings.get("settings", lang)) },
                text = {
                    Column {
                        Text(Strings.get("theme", lang), style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf("system" to Strings.get("theme_system", lang), "light" to Strings.get("theme_light", lang), "dark" to Strings.get("theme_dark", lang)).forEach { (v, lbl) ->
                                Row(Modifier.clickable { themeMode = v; config.themeMode = v }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = themeMode == v, onClick = { themeMode = v; config.themeMode = v })
                                    Text(lbl, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(Strings.get("language", lang), style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf("en" to "English", "es" to "Espa\u00f1ol").forEach { (v, lbl) ->
                                Row(Modifier.clickable { selectedLang = v; config.language = v }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedLang == v, onClick = { selectedLang = v; config.language = v })
                                    Text(lbl, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(Strings.get("notifications", lang), style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = notifEnabled, onCheckedChange = { enabled ->
                                notifEnabled = enabled
                                config.notificationsEnabled = enabled
                                if (enabled) {
                                    NotificationWorker.schedule(context)
                                } else {
                                    NotificationWorker.cancel(context)
                                }
                            })
                            Spacer(Modifier.width(4.dp))
                            Text(Strings.get("notif_enable", lang), style = MaterialTheme.typography.bodySmall)
                        }
                        AnimatedVisibility(visible = notifEnabled) {
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val types = listOf("daily" to "Daily", "weekly" to "Weekly", "custom" to "Custom")
                                    types.forEach { (key, label) ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                            .clickable {
                                                notifScheduleType = key
                                                config.notificationScheduleType = key
                                                NotificationWorker.schedule(context)
                                            }
                                            .padding(end = 12.dp),
                                        ) {
                                            RadioButton(selected = notifScheduleType == key, onClick = {
                                                notifScheduleType = key
                                                config.notificationScheduleType = key
                                                NotificationWorker.schedule(context)
                                            })
                                            Spacer(Modifier.width(2.dp))
                                            Text(label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                if (notifScheduleType == "custom") {
                                    Spacer(Modifier.height(8.dp))
                                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                        dayNames.forEachIndexed { index, name ->
                                            val isSelected = index in notifCustomDaysList
                                            Box(contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .size(36.dp)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        CircleShape,
                                                    )
                                                    .clickable {
                                                        notifCustomDaysList = if (isSelected) notifCustomDaysList - index else notifCustomDaysList + index
                                                        config.notificationCustomDays = notifCustomDaysList.sorted().joinToString(",")
                                                        NotificationWorker.schedule(context)
                                                    },
                                            ) {
                                                Text(name.first().toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        notifCustomHoursList.forEachIndexed { i, timeStr ->
                                            val h = parseHour(timeStr); val m = parseMinute(timeStr)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            val ctx = context
                                                            TimePickerDialog(ctx, { _, newH, newM ->
                                                                val newStr = String.format("%d:%02d", newH, newM)
                                                                notifCustomHoursList = notifCustomHoursList.toMutableList().also { it[i] = newStr }
                                                                config.notificationCustomHours = notifCustomHoursList.joinToString(",")
                                                                NotificationWorker.schedule(context)
                                                            }, h, m, false).show()
                                                        },
                                                        modifier = Modifier.height(24.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                    ) {
                                                        Text(formatTime(timeStr), style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                                if (notifCustomHoursList.size > 1) {
                                                    TextButton(
                                                        onClick = {
                                                            notifCustomHoursList = notifCustomHoursList.toMutableList().also { it.removeAt(i) }
                                                            config.notificationCustomHours = notifCustomHoursList.joinToString(",")
                                                            NotificationWorker.schedule(context)
                                                        },
                                                        modifier = Modifier.height(24.dp),
                                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                                    ) {
                                                        Text("\u2212",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = MaterialTheme.colorScheme.error,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        TextButton(onClick = {
                                            val ctx = context
                                            TimePickerDialog(ctx, { _, h, m ->
                                                val newStr = String.format("%d:%02d", h, m)
                                                notifCustomHoursList = notifCustomHoursList + newStr
                                                config.notificationCustomHours = notifCustomHoursList.joinToString(",")
                                                NotificationWorker.schedule(context)
                                            }, 9, 0, false).show()
                                        }, modifier = Modifier.height(24.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        ) {
                                            Text("+ ${Strings.get("add_hour", lang)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = !notif24hFormat, onClick = { notif24hFormat = false; config.notification24hFormat = false })
                                        Spacer(Modifier.width(2.dp))
                                        Text("12h", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(12.dp))
                                        RadioButton(selected = notif24hFormat, onClick = { notif24hFormat = true; config.notification24hFormat = true })
                                        Spacer(Modifier.width(2.dp))
                                        Text("24h", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showSettings = false; showOutputSettings = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Output Settings")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            config.token = ""
                            config.password = ""
                            config.moodleUrl = ""
                            config.username = ""
                            password = ""
                            url = ""
                            username = ""
                            showSettings = false
                        }) {
                            Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("OK") } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputSettingsPage(
    icsEnabled: Boolean, onIcsEnabledChange: (Boolean) -> Unit,
    logseqEnabled: Boolean, onLogseqEnabledChange: (Boolean) -> Unit,
    obsidianEnabled: Boolean, onObsidianEnabledChange: (Boolean) -> Unit,
    icsPath: String, onIcsPathChange: (String) -> Unit,
    logseqPath: String, onLogseqPathChange: (String) -> Unit,
    obsidianPath: String, onObsidianPathChange: (String) -> Unit,
    daysBackText: String, onDaysBackChange: (String) -> Unit,
    limitText: String, onLimitChange: (String) -> Unit,
    tasksEnabled: Boolean, onTasksEnabledChange: (Boolean) -> Unit,
    tasksOutputPath: String, onTasksPathChange: (String) -> Unit,
    lang: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Output Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            OutputTabContent(
                icsEnabled = icsEnabled, onIcsEnabledChange = onIcsEnabledChange,
                logseqEnabled = logseqEnabled, onLogseqEnabledChange = onLogseqEnabledChange,
                obsidianEnabled = obsidianEnabled, onObsidianEnabledChange = onObsidianEnabledChange,
                icsPath = icsPath, onIcsPathChange = onIcsPathChange,
                logseqPath = logseqPath, onLogseqPathChange = onLogseqPathChange,
                obsidianPath = obsidianPath, onObsidianPathChange = onObsidianPathChange,
                daysBackText = daysBackText, onDaysBackChange = onDaysBackChange,
                limitText = limitText, onLimitChange = onLimitChange,
                tasksEnabled = tasksEnabled, onTasksEnabledChange = onTasksEnabledChange,
                tasksOutputPath = tasksOutputPath, onTasksPathChange = onTasksPathChange,
                lang = lang,
            )
        }
    }
}

private suspend fun doFetch(
    context: Context, config: ConfigStore,
    url: String, username: String, password: String, tz: String, savePw: Boolean,
    icsEnabled: Boolean, logseqEnabled: Boolean, obsidianEnabled: Boolean,
    icsPath: String, logseqPath: String, obsidianPath: String,
    daysBackText: String, limitText: String, lang: String,
    tasksEnabled: Boolean, tasksOutputPath: String,
    onLog: (String) -> Unit, onStatus: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit,
    onEventsFetched: (List<Event>) -> Unit = {},
) {
    try {
        config.moodleUrl = url.trimEnd('/'); config.username = username.trim(); config.timezone = tz.trim()
        config.savePassword = savePw; if (savePw) config.password = password else config.password = ""
        config.icsEnabled = icsEnabled; config.logseqEnabled = logseqEnabled; config.obsidianEnabled = obsidianEnabled
        config.icsPath = icsPath; config.logseqPath = logseqPath; config.obsidianPath = obsidianPath
        config.tasksEnabled = tasksEnabled; config.tasksOutputPath = tasksOutputPath
        config.fetchDaysBack = daysBackText.toIntOrNull() ?: 7; config.fetchLimit = limitText.toIntOrNull() ?: 100

        var token = config.token
        if (token.isBlank()) {
            onLog(Strings.get("no_cached_token", lang))
            token = withContext(Dispatchers.IO) { MoodleApi.login(url, username, password) }
            config.token = token; if (!savePw) config.password = ""
            onLog(Strings.get("login_success", lang))
        }

        onLog(Strings.get("fetching_events", lang))
        val events = withContext(Dispatchers.IO) { MoodleApi(config.moodleUrl, token).fetchEvents(config.fetchDaysBack, config.fetchLimit) }
        onLog("${Strings.get("found_events", lang)} ${events.size}")

        if (config.icsEnabled) {
            val icsContent = IcsGenerator.generate(events)
            withContext(Dispatchers.IO) {
                File(context.cacheDir, "ics/calendar.ics").also { it.parentFile?.mkdirs(); it.writeText(icsContent) }
                if (config.icsPath.isNotBlank()) PathResolver.writeIcs(context, config.icsPath, icsContent)
            }
            onLog("ICS -> ${config.icsPath.ifBlank { "${context.cacheDir}/ics/calendar.ics" }}")
        }
        if (config.logseqEnabled) {
            val logseqFiles = MarkdownGenerator.generateLogseq(events)
            val logseqTarget = config.logseqPath.ifBlank { "${context.cacheDir}/logseq" }
            withContext(Dispatchers.IO) { PathResolver.writeMarkdownFiles(context, logseqTarget, "logseq", logseqFiles) }
            onLog("Logseq -> ${config.logseqPath.ifBlank { "${context.cacheDir}/logseq/" }}")
        }
        if (config.obsidianEnabled) {
            val obsidianFiles = MarkdownGenerator.generateObsidian(events)
            val obsidianTarget = config.obsidianPath.ifBlank { "${context.cacheDir}/obsidian" }
            withContext(Dispatchers.IO) { PathResolver.writeMarkdownFiles(context, obsidianTarget, "obsidian", obsidianFiles) }
            onLog("Obsidian -> ${config.obsidianPath.ifBlank { "${context.cacheDir}/obsidian/" }}")
        }

        onEventsFetched(events)

        config.lastSyncTimestamp = System.currentTimeMillis()
        config.lastSyncMessage = "${Strings.get("done", lang)} \u2014 ${events.size} events"
        config.eventCount = events.size
        onStatus("${Strings.get("done", lang)} \u2014 ${events.size} events")
        onLog(Strings.get("done", lang))
    } catch (e: Exception) {
        val msg = e.message ?: "Unknown error"
        config.lastSyncMessage = "${Strings.get("error", lang)}: $msg"
        onStatus("${Strings.get("error", lang)}: $msg")
        onLog("${Strings.get("error", lang)}: $msg")
        onError(msg)
    } finally { onDone() }
}

private fun shareIcs(context: Context, config: ConfigStore) {
    val f = File(context.cacheDir, "ics/calendar.ics"); if (!f.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/calendar"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

@Composable
private fun TasksTabContent(
    events: List<Event>, completionMap: Map<String, Boolean>,
    expandedId: String?, onExpandedChange: (String?) -> Unit,
    onToggleCompletion: (String) -> Unit, onClearCompleted: () -> Unit, lang: String,
    showIntro: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (events.isEmpty()) {
        if (showIntro) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(Strings.get("intro_title", lang), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(Strings.get("intro_welcome", lang), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Text(Strings.get("intro_setup_outputs", lang), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Text(Strings.get("intro_getting_started", lang), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(Strings.get("intro_step1", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(Strings.get("intro_step2", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(Strings.get("intro_step3", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                Text(Strings.get("intro_outputs", lang), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(Strings.get("intro_ics", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(Strings.get("intro_logseq", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(Strings.get("intro_obsidian", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(Strings.get("intro_tasks", lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(Strings.get("intro_configure", lang), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Text(Strings.get("intro_footer", lang), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(Strings.get("no_tasks", lang), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val sorted = events.sortedBy { it.timestart }
    val grouped = sorted.groupBy { it.course.ifBlank { "General" } }.toSortedMap()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        grouped.forEach { (course, courseEvents) ->
            Text(course, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            courseEvents.forEach { ev ->
                val isDone = completionMap[ev.id] == true
                val isExpanded = expandedId == ev.id
                TaskCard(
                    event = ev, isDone = isDone, isExpanded = isExpanded,
                    onToggle = { onToggleCompletion(ev.id) },
                    onExpand = { onExpandedChange(if (isExpanded) null else ev.id) },
                    lang = lang, context = context,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        if (completionMap.any { it.value }) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onClearCompleted, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(Strings.get("clear_completed", lang), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TaskCard(
    event: Event, isDone: Boolean, isExpanded: Boolean,
    onToggle: () -> Unit, onExpand: () -> Unit, lang: String, context: android.content.Context,
) {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = event.timestart * 1000 }
    val now = java.util.Calendar.getInstance()
    val diffDays = ((cal.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    val relativeDate = when {
        diffDays < 0 -> "overdue"
        diffDays == 0 -> "today"
        diffDays == 1 -> "tomorrow"
        diffDays <= 7 -> "in $diffDays days"
        else -> {
            val y = cal.get(java.util.Calendar.YEAR)
            val m = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH))
            val h = String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
            val min = String.format("%02d", cal.get(java.util.Calendar.MINUTE))
            "$y-$m-$d $h:$min"
        }
    }
    val overdueColor = MaterialTheme.colorScheme.error
    val dateColor = if (diffDays < 0) overdueColor else MaterialTheme.colorScheme.onSurfaceVariant

    val cardModifier = if (isDone && !isExpanded) {
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .drawBehind {
                drawRect(
                    color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                )
            }
            .alpha(0.6f)
            .clickable { onExpand() }
    } else {
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .clickable { onExpand() }
    }

    Column(modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isDone, onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(event.course.ifBlank { "?" }, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = event.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                maxLines = if (isDone && !isExpanded) 1 else Int.MAX_VALUE,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = relativeDate,
                style = MaterialTheme.typography.bodySmall,
                color = dateColor,
            )
            if (isDone && !isExpanded) {
                Spacer(Modifier.width(4.dp))
                Text("\u25B2", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 40.dp, end = 8.dp, bottom = 8.dp)) {
                if (event.description.isNotBlank()) {
                    Text(event.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                }
                if (event.url.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val i = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(event.url))
                            context.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(Strings.get("open_in_browser", lang), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onExpand,
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(Strings.get("collapse", lang) + " \u25B2", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionTabContent(
    url: String, onUrlChange: (String) -> Unit, username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit, passwordVisible: Boolean, onPasswordVisibleChange: (Boolean) -> Unit,
    tz: String, onTzChange: (String) -> Unit, tzExpanded: Boolean, onTzExpandedChange: (Boolean) -> Unit, lang: String,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(value = url, onValueChange = onUrlChange, label = { Text(Strings.get("moodle_url", lang)) },
                    placeholder = { Text("https://moodle.example.com") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = username, onValueChange = onUsernameChange, label = { Text(Strings.get("username", lang)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text(Strings.get("password", lang)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = tzExpanded, onExpandedChange = onTzExpandedChange) {
                    OutlinedTextField(value = tz, onValueChange = {}, readOnly = true, label = { Text(Strings.get("timezone", lang)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tzExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable), singleLine = true)
                    ExposedDropdownMenu(expanded = tzExpanded, onDismissRequest = { onTzExpandedChange(false) }) {
                        Strings.timezones.forEach { zone -> DropdownMenuItem(text = { Text(zone) }, onClick = { onTzChange(zone); onTzExpandedChange(false) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputTabContent(
    icsEnabled: Boolean, onIcsEnabledChange: (Boolean) -> Unit, logseqEnabled: Boolean, onLogseqEnabledChange: (Boolean) -> Unit,
    obsidianEnabled: Boolean, onObsidianEnabledChange: (Boolean) -> Unit,
    icsPath: String, onIcsPathChange: (String) -> Unit, logseqPath: String, onLogseqPathChange: (String) -> Unit,
    obsidianPath: String, onObsidianPathChange: (String) -> Unit,
    daysBackText: String, onDaysBackChange: (String) -> Unit, limitText: String, onLimitChange: (String) -> Unit,
    tasksEnabled: Boolean = true, onTasksEnabledChange: (Boolean) -> Unit = {},
    tasksOutputPath: String = "", onTasksPathChange: (String) -> Unit = {},
    lang: String,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val icsPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            onIcsPathChange(it.toString())
        }
    }
    val logseqPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            onLogseqPathChange(it.toString())
        }
    }
    val obsidianPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            onObsidianPathChange(it.toString())
        }
    }
    val tasksPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            onTasksPathChange(it.toString())
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.get("formats", lang), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = icsEnabled, onCheckedChange = onIcsEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("ics_label", lang)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = logseqEnabled, onCheckedChange = onLogseqEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("logseq_label", lang)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = obsidianEnabled, onCheckedChange = onObsidianEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("obsidian_label", lang)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = tasksEnabled, onCheckedChange = onTasksEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("tasks_path", lang)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.get("paths", lang), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = icsPath, onValueChange = onIcsPathChange, label = { Text(Strings.get("ics_file", lang)) }, singleLine = true, modifier = Modifier.weight(1f), enabled = icsEnabled)
                    TextButton(onClick = { icsPicker.launch("calendar.ics") }, enabled = icsEnabled) { Text("\u2026") }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = logseqPath, onValueChange = onLogseqPathChange, label = { Text(Strings.get("logseq_dir", lang)) }, singleLine = true, modifier = Modifier.weight(1f), enabled = logseqEnabled)
                    TextButton(onClick = { logseqPicker.launch(null) }, enabled = logseqEnabled) { Text("\u2026") }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = obsidianPath, onValueChange = onObsidianPathChange, label = { Text(Strings.get("obsidian_dir", lang)) }, singleLine = true, modifier = Modifier.weight(1f), enabled = obsidianEnabled)
                    TextButton(onClick = { obsidianPicker.launch(null) }, enabled = obsidianEnabled) { Text("\u2026") }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = tasksOutputPath, onValueChange = onTasksPathChange, label = { Text(Strings.get("tasks_path", lang)) }, singleLine = true, modifier = Modifier.weight(1f), enabled = tasksEnabled)
                    TextButton(onClick = { tasksPicker.launch("tasks.md") }, enabled = tasksEnabled) { Text("\u2026") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.get("fetch_params", lang), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = daysBackText, onValueChange = { onDaysBackChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(Strings.get("fetch_days", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = limitText, onValueChange = { onLimitChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(Strings.get("fetch_limit", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
