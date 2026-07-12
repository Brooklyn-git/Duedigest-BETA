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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.Event
import com.moodlebridge.ui.DueNestTheme
import com.moodlebridge.data.IcsGenerator
import com.moodlebridge.data.MarkdownGenerator
import com.moodlebridge.data.MoodleApi
import com.moodlebridge.data.PathResolver
import com.moodlebridge.data.Strings
import com.moodlebridge.worker.NotificationWorker
import com.moodlebridge.worker.TaskReminderWorker
import com.moodlebridge.worker.SyncWorker
import androidx.work.WorkManager
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.glance.appwidget.updateAll
import com.moodlebridge.widget.TaskWidget
import com.moodlebridge.widget.FetchTaskWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = ConfigStore(this)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        TaskReminderWorker.schedule(this)
        setContent { MainContent(config = config, autoSync = intent?.getStringExtra("sync") == "true") }
    }
    private fun createNotificationChannel() {
        val syncCh = NotificationChannel(SyncWorker.CHANNEL_ID, "Moodle Sync", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(syncCh)
        val reminderCh = NotificationChannel(NotificationWorker.CHANNEL_ID, "DueNest Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(reminderCh)
        val taskCh = NotificationChannel(TaskReminderWorker.CHANNEL_ID, "DueNest Task Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(taskCh)
    }
}

private fun parseHour(s: String): Int {
    val parts = s.split(":")
    return parts[0].toIntOrNull() ?: 0
}
private fun parseMinute(s: String): Int {
    val parts = s.split(":")
    return if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
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

    val pagerState = rememberPagerState(pageCount = { 2 })
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
    var taskRemindEnabled by remember { mutableStateOf(config.taskRemindersEnabled) }
    var taskRemindScheduleType by remember { mutableStateOf(config.taskReminderScheduleType) }
    var taskRemindCustomDaysList by remember { mutableStateOf(parseDayList(config.taskReminderCustomDays)) }
    var taskRemindCustomHoursList by remember { mutableStateOf(parseHourList(config.taskReminderCustomHours)) }
    var fetchedEvents by remember { mutableStateOf(listOf<Event>()) }
    var manualEvents by remember { mutableStateOf(
        try { Json.decodeFromString<List<Event>>(config.manualEventCache) } catch (_: Exception) { emptyList() }
    ) }
    var taskCompletionMap by remember { mutableStateOf(
        try { Json.decodeFromString<Map<String, Boolean>>(config.taskCompletionState) } catch (_: Exception) { emptyMap() }
    ) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }
    var tasksEnabled by remember { mutableStateOf(config.tasksEnabled) }
    var tasksOutputPath by remember { mutableStateOf(config.tasksOutputPath) }

    var showTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Event?>(null) }
    var dialogName by remember { mutableStateOf("") }
    var dialogDescription by remember { mutableStateOf("") }
    var dialogCourse by remember { mutableStateOf("") }
    var dialogDate by remember { mutableStateOf("") }
    var dialogTime by remember { mutableStateOf("") }
    var dialogDuration by remember { mutableStateOf("60") }
    var dialogUrl by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showClearCompletedConfirm by remember { mutableStateOf(false) }
    var showClearCredsConfirm by remember { mutableStateOf(false) }
    var courseExpanded by remember { mutableStateOf(false) }
    var showNewCourseDialog by remember { mutableStateOf(false) }
    var newCourseName by remember { mutableStateOf("") }

    val courseOptions by remember {
        derivedStateOf {
            val courses = (fetchedEvents + manualEvents).map { it.course }.filter { it.isNotBlank() && it != "General" }.toMutableSet()
            courses.add("General")
            courses.sorted()
        }
    }

    fun formatTime(s: String): String { return if (notif24hFormat) formatTime24(s) else formatTime12(s) }

    fun addLog(msg: String) { logLines.add(msg) }
    val formatTimeFn: (String) -> String = { formatTime(it) }

    fun saveCompletionMap() {
        config.taskCompletionState = Json.encodeToString(taskCompletionMap)
    }

    fun fileExists(path: String): Boolean {
        return if (path.startsWith("content://")) {
            try {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            } catch (_: Exception) { false }
        } else {
            File(path).exists()
        }
    }

    fun getMergedEvents(): List<Event> {
        val merged = fetchedEvents.toMutableList()
        merged.addAll(manualEvents)
        return merged.sortedBy { it.timestart }
    }

    fun regenerateTasksFile() {
        if (!tasksEnabled || tasksOutputPath.isBlank()) return
        val merged = getMergedEvents()
        val isFirstLaunch = config.lastSyncTimestamp == 0L && merged.isEmpty()
        if (isFirstLaunch && fileExists(tasksOutputPath)) return
        val content = if (isFirstLaunch) {
            MarkdownGenerator.generateIntroMd(lang)
        } else {
            MarkdownGenerator.generateTasksList(merged, taskCompletionMap)
        }
        PathResolver.writeIcs(context, tasksOutputPath, content)
    }

    fun persistMergedEvents() {
        val merged = getMergedEvents()
        config.taskEventCache = Json.encodeToString(merged)
        regenerateTasksFile()
        TaskReminderWorker.showNotification(context)
        scope.launch {
            TaskWidget().updateAll(context)
            FetchTaskWidget().updateAll(context)
        }
    }

    fun addOrUpdateManualEvent(event: Event) {
        manualEvents = manualEvents.toMutableList().also { list ->
            val idx = list.indexOfFirst { it.id == event.id }
            if (idx >= 0) list[idx] = event else list.add(event)
        }
        config.manualEventCache = Json.encodeToString(manualEvents)
        persistMergedEvents()
    }

    fun deleteManualEvent(id: String) {
        manualEvents = manualEvents.filter { it.id != id }
        config.manualEventCache = Json.encodeToString(manualEvents)
        taskCompletionMap = taskCompletionMap.toMutableMap().also { it.remove(id) }
        saveCompletionMap()
        persistMergedEvents()
    }

    fun openTaskDialog(task: Event? = null) {
        editingTask = task
        if (task != null) {
            dialogName = task.name
            dialogDescription = task.description
            dialogCourse = task.course
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = task.timestart * 1000 }
            val y = cal.get(java.util.Calendar.YEAR)
            val m = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH))
            dialogDate = "$y-$m-$d"
            val h = String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
            val min = String.format("%02d", cal.get(java.util.Calendar.MINUTE))
            dialogTime = "$h:$min"
            dialogDuration = (task.timeduration / 60).toString()
            dialogUrl = task.url
        } else {
            dialogName = ""; dialogDescription = ""; dialogCourse = ""
            val now = java.util.Calendar.getInstance()
            val y = now.get(java.util.Calendar.YEAR)
            val m = String.format("%02d", now.get(java.util.Calendar.MONTH) + 1)
            val d = String.format("%02d", now.get(java.util.Calendar.DAY_OF_MONTH))
            dialogDate = "$y-$m-$d"
            val h = String.format("%02d", now.get(java.util.Calendar.HOUR_OF_DAY))
            val curMin = String.format("%02d", now.get(java.util.Calendar.MINUTE))
            dialogTime = "$h:$curMin"
            dialogDuration = "60"; dialogUrl = ""
        }
        showTaskDialog = true
    }

    fun saveTaskDialog() {
        val parts = dialogDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: return
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return
        val d = parts.getOrNull(2)?.toIntOrNull() ?: return
        val timeParts = dialogTime.split(":")
        val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val min = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = java.util.Calendar.getInstance().apply {
            set(y, m - 1, d, h, min, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val timestart = cal.timeInMillis / 1000
        val durationSec = (dialogDuration.toLongOrNull() ?: 60) * 60
        val id = editingTask?.id ?: "manual_${UUID.randomUUID()}"
        addOrUpdateManualEvent(Event(
            id = id,
            name = dialogName.ifBlank { "Untitled" },
            description = dialogDescription,
            timestart = timestart,
            timeduration = durationSec,
            eventtype = "manual",
            url = dialogUrl,
            course = dialogCourse.ifBlank { "General" },
            modname = "manual",
            source = "manual",
        ))
        showTaskDialog = false
        editingTask = null
    }

    fun doSync(pw: String) {
        isWorking = true; logLines.clear(); statusText = ""
        scope.launch {
            val manual = manualEvents.toList()
            doFetch(context, config, url, username, pw, tz, savePw,
                icsEnabled, logseqEnabled, obsidianEnabled, icsPath, logseqPath, obsidianPath,
                daysBackText, limitText, lang, tasksEnabled, tasksOutputPath, manual,
                { addLog(it) }, { statusText = it }, { errorDialogMsg = it },
                { isWorking = false },
                { events ->
                    fetchedEvents = events; expandedTaskId = null
                    persistMergedEvents()
                    val icsFile = File(context.cacheDir, "ics/calendar.ics")
                    val openIntent: Intent
                    val bodyText: String
                    if (icsEnabled && icsFile.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", icsFile)
                        openIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "text/calendar")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        bodyText = "${events.size} events — tap to open in calendar"
                    } else {
                        openIntent = Intent(context, com.moodlebridge.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        bodyText = "${events.size} events synced"
                    }
                    val pi = android.app.PendingIntent.getActivity(
                        context, 0, openIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    )
                    val notification = androidx.core.app.NotificationCompat.Builder(context, com.moodlebridge.worker.SyncWorker.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                        .setContentTitle("Moodle Sync Complete")
                        .setContentText(bodyText)
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build()
                    androidx.core.app.NotificationManagerCompat.from(context).notify(1, notification)
                })
        }
    }

    DueNestTheme(themeMode = themeMode) {
        val currentThemeIsDark = when (themeMode) {
            "amoled_dark" -> true
            "dark" -> true
            "light" -> false
            else -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !currentThemeIsDark
            }
        }
        if (showOutputSettings) {
            OutputSettingsPage(
                icsEnabled = icsEnabled, onIcsEnabledChange = { icsEnabled = it; config.icsEnabled = it },
                logseqEnabled = logseqEnabled, onLogseqEnabledChange = { logseqEnabled = it; config.logseqEnabled = it },
                obsidianEnabled = obsidianEnabled, onObsidianEnabledChange = { obsidianEnabled = it; config.obsidianEnabled = it },
                icsPath = icsPath, onIcsPathChange = { icsPath = it; config.icsPath = it },
                logseqPath = logseqPath, onLogseqPathChange = { logseqPath = it; config.logseqPath = it },
                obsidianPath = obsidianPath, onObsidianPathChange = { obsidianPath = it; config.obsidianPath = it },
                daysBackText = daysBackText, onDaysBackChange = { daysBackText = it; config.fetchDaysBack = it.toIntOrNull() ?: 7 },
                limitText = limitText, onLimitChange = { limitText = it; config.fetchLimit = it.toIntOrNull() ?: 100 },
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
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    listOf(
                        Strings.get("tasks", lang),
                        Strings.get("connection", lang),
                    ).forEachIndexed { i, t ->
                        Tab(selected = pagerState.currentPage == i,
                            onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                            text = { Text(t, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.height(56.dp))
                    }
                }

                // ── Swipeable tab content ───────────────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { page ->
                    if (page == 0) {
                        Box(Modifier.fillMaxSize()) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 72.dp)) {
                                TasksTabContent(
                                    events = getMergedEvents(), completionMap = taskCompletionMap,
                                    expandedId = expandedTaskId, onExpandedChange = { expandedTaskId = it },
                                    onToggleCompletion = { id ->
                                        taskCompletionMap = taskCompletionMap.toMutableMap().also { it[id] = !(it[id] ?: false) }
                                        saveCompletionMap()
                                        persistMergedEvents()
                                    },
                                    onClearCompleted = { showClearCompletedConfirm = true },
                                    onEditTask = { openTaskDialog(it) },
                                    onDeleteTask = { showDeleteConfirm = it },
                                    lang = lang, use24h = notif24hFormat,
                                    showIntro = config.lastSyncTimestamp == 0L && fetchedEvents.isEmpty())

                                // ── Progress ─────────────────────────
                                if (isWorking) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)); Spacer(Modifier.height(4.dp)) }

                                // ── Status ───────────────────────────
                                Text(statusText.ifBlank { Strings.get("ready", lang) }, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            }
                            FloatingActionButton(
                                onClick = { openTaskDialog() },
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Icon(Icons.Default.Add, contentDescription = Strings.get("add_task", lang))
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            ConnectionTabContent(url = url, onUrlChange = { url = it },
                                username = username, onUsernameChange = { username = it },
                                password = password, onPasswordChange = { password = it },
                                passwordVisible = passwordVisible, onPasswordVisibleChange = { passwordVisible = it },
                                tz = tz, onTzChange = { tz = it }, tzExpanded = tzExpanded,
                                onTzExpandedChange = { tzExpanded = it }, lang = lang)

                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = savePw, onCheckedChange = { savePw = it })
                                Spacer(Modifier.width(4.dp))
                                Text(Strings.get("store_pw", lang), style = MaterialTheme.typography.bodySmall)
                            }

                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { doSync(password) },
                                    enabled = url.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !isWorking,
                                    modifier = Modifier.weight(1f)) {
                                    Text(if (isWorking) Strings.get("working", lang) else Strings.get("fetch", lang))
                                }
                                if (icsEnabled) {
                                    OutlinedButton(onClick = { shareIcs(context, config) }, modifier = Modifier.weight(1f)) { Text(Strings.get("share_ics", lang)) }
                                }
                            }

                            if (isWorking) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)); Spacer(Modifier.height(4.dp)) }

                            Text(Strings.get("log", lang), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                    logLines.toList().forEach { line -> Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                                }
                            }

                            Text(statusText.ifBlank { Strings.get("ready", lang) }, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Add Task dialog ─────────────────────────────────────
    val cs = MaterialTheme.colorScheme
    if (showDeleteConfirm != null) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = null },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(Strings.get("delete_task", lang)) },
            text = { Text(Strings.get("delete_task_confirm", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteManualEvent(showDeleteConfirm!!)
                    showDeleteConfirm = null
                }) { Text(Strings.get("delete_task", lang), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text(Strings.get("cancel", lang)) }
            })
    }
    if (showTaskDialog) {
        AlertDialog(
            onDismissRequest = { showTaskDialog = false; editingTask = null },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(if (editingTask != null) Strings.get("edit_task", lang) else Strings.get("add_task", lang)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = dialogName, onValueChange = { dialogName = it },
                        label = { Text(Strings.get("task_name", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = dialogDescription, onValueChange = { dialogDescription = it },
                        label = { Text(Strings.get("task_description", lang)) }, singleLine = false, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = it }) {
                        OutlinedTextField(value = dialogCourse, onValueChange = {}, readOnly = true,
                            label = { Text(Strings.get("task_course", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                            courseOptions.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = {
                                    dialogCourse = c; courseExpanded = false
                                })
                            }
                            if (courseOptions.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            DropdownMenuItem(text = { Text("+ ${Strings.get("add_course", lang)}", color = MaterialTheme.colorScheme.primary) }, onClick = {
                                courseExpanded = false; newCourseName = ""; showNewCourseDialog = true
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(value = dialogDate, onValueChange = {}, readOnly = true,
                                label = { Text(Strings.get("task_date", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Box(Modifier.matchParentSize().clickable {
                                val parts = dialogDate.split("-")
                                val y = parts.getOrNull(0)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                val m = (parts.getOrNull(1)?.toIntOrNull() ?: (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)) - 1
                                val d = parts.getOrNull(2)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
                                DatePickerDialog(context, { _, year, month, day ->
                                    dialogDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                                }, y, m, d).show()
                            })
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(value = formatTime(dialogTime), onValueChange = {}, readOnly = true,
                                label = { Text(Strings.get("task_time", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Box(Modifier.matchParentSize().clickable {
                                val timeParts = dialogTime.split(":")
                                val h = timeParts.getOrNull(0)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                val min = timeParts.getOrNull(1)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
                                TimePickerDialog(context, { _, hour, minute ->
                                    dialogTime = String.format("%02d:%02d", hour, minute)
                                }, h, min, false).show()
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = dialogDuration, onValueChange = { dialogDuration = it.filter { c -> c.isDigit() } },
                        label = { Text(Strings.get("task_duration", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = dialogUrl, onValueChange = { dialogUrl = it },
                        label = { Text(Strings.get("task_url", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { saveTaskDialog() }) { Text(Strings.get("save", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showTaskDialog = false; editingTask = null }) { Text(Strings.get("cancel", lang)) }
            })
    }

    if (showNewCourseDialog) {
        AlertDialog(
            onDismissRequest = { showNewCourseDialog = false },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(Strings.get("task_course", lang)) },
            text = {
                OutlinedTextField(value = newCourseName, onValueChange = { newCourseName = it },
                    label = { Text(Strings.get("task_course", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCourseName.isNotBlank()) dialogCourse = newCourseName
                    showNewCourseDialog = false
                }) { Text(Strings.get("save", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewCourseDialog = false }) { Text(Strings.get("cancel", lang)) }
            })
    }

    // ── Error dialog ────────────────────────────────────────
        if (errorDialogMsg != null) {
            AlertDialog(onDismissRequest = { errorDialogMsg = null },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("error", lang)) },
                text = { Text(errorDialogMsg ?: "") },
                confirmButton = { TextButton(onClick = { errorDialogMsg = null }) { Text("OK") } })
        }
        if (showSettings) {
            AlertDialog(onDismissRequest = { showSettings = false },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("settings", lang)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(Strings.get("theme", lang), style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            listOf("system" to Strings.get("theme_system", lang), "light" to Strings.get("theme_light", lang), "dark" to Strings.get("theme_dark", lang), "amoled_dark" to Strings.get("theme_amoled", lang)).forEach { (v, lbl) ->
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    ReminderScheduleSection(
                                        scheduleType = notifScheduleType,
                                        onScheduleTypeChange = { key ->
                                            notifScheduleType = key
                                            config.notificationScheduleType = key
                                            NotificationWorker.schedule(context)
                                        },
                                        customDaysList = notifCustomDaysList,
                                        onCustomDaysListChange = { list ->
                                            notifCustomDaysList = list
                                            config.notificationCustomDays = list.sorted().joinToString(",")
                                            NotificationWorker.schedule(context)
                                        },
                                        customHoursList = notifCustomHoursList,
                                        onCustomHoursListChange = { list ->
                                            notifCustomHoursList = list
                                            config.notificationCustomHours = list.joinToString(",")
                                            NotificationWorker.schedule(context)
                                        },
                                        lang = lang,
                                        context = context,
                                        formatTime = formatTimeFn,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = taskRemindEnabled, onCheckedChange = { enabled ->
                                taskRemindEnabled = enabled
                                config.taskRemindersEnabled = enabled
                                if (enabled) {
                                    TaskReminderWorker.schedule(context)
                                } else {
                                    TaskReminderWorker.cancel(context)
                                }
                            })
                            Spacer(Modifier.width(4.dp))
                            Text(Strings.get("notif_task_enable", lang), style = MaterialTheme.typography.bodySmall)
                        }
                        AnimatedVisibility(visible = taskRemindEnabled) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    ReminderScheduleSection(
                                        scheduleType = taskRemindScheduleType,
                                        onScheduleTypeChange = { key ->
                                            taskRemindScheduleType = key
                                            config.taskReminderScheduleType = key
                                            TaskReminderWorker.schedule(context)
                                        },
                                        customDaysList = taskRemindCustomDaysList,
                                        onCustomDaysListChange = { list ->
                                            taskRemindCustomDaysList = list
                                            config.taskReminderCustomDays = list.sorted().joinToString(",")
                                            TaskReminderWorker.schedule(context)
                                        },
                                        customHoursList = taskRemindCustomHoursList,
                                        onCustomHoursListChange = { list ->
                                            taskRemindCustomHoursList = list
                                            config.taskReminderCustomHours = list.joinToString(",")
                                            TaskReminderWorker.schedule(context)
                                        },
                                        lang = lang,
                                        context = context,
                                        formatTime = formatTimeFn,
                                    )
                                }
                            }
                        }
                        Column {
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
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showSettings = false; showOutputSettings = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Output Settings")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showClearCredsConfirm = true }) {
                            Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("OK") } })
        }

    // ── Clear completed confirmation ────────────────────────
    if (showClearCompletedConfirm) {
        AlertDialog(onDismissRequest = { showClearCompletedConfirm = false },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(Strings.get("clear_completed", lang)) },
            text = { Text(Strings.get("clear_completed_confirm", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    val completedManual = manualEvents.filter { taskCompletionMap[it.id] == true }
                    for (ev in completedManual) deleteManualEvent(ev.id)
                    taskCompletionMap = emptyMap()
                    saveCompletionMap()
                    persistMergedEvents()
                    expandedTaskId = null
                    showClearCompletedConfirm = false
                }) { Text(Strings.get("clear_completed", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedConfirm = false }) { Text(Strings.get("cancel", lang)) }
            })
    }

    // ── Clear credentials confirmation ──────────────────────
    if (showClearCredsConfirm) {
        AlertDialog(onDismissRequest = { showClearCredsConfirm = false },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(Strings.get("clear_creds", lang)) },
            text = { Text(Strings.get("clear_creds_confirm", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    config.token = ""
                    config.password = ""
                    config.moodleUrl = ""
                    config.username = ""
                    password = ""
                    url = ""
                    username = ""
                    showSettings = false
                    showClearCredsConfirm = false
                }) { Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCredsConfirm = false }) { Text(Strings.get("cancel", lang)) }
            })
    }
    }
}

private fun formatTime24(s: String): String {
    val h = parseHour(s); val m = parseMinute(s)
    return String.format("%02d:%02d", h, m)
}
private fun formatTime12(s: String): String {
    val h = parseHour(s); val m = parseMinute(s)
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return String.format("%d:%02d %s", h12, m, ampm)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderScheduleSection(
    scheduleType: String,
    onScheduleTypeChange: (String) -> Unit,
    customDaysList: List<Int>,
    onCustomDaysListChange: (List<Int>) -> Unit,
    customHoursList: List<String>,
    onCustomHoursListChange: (List<String>) -> Unit,
    lang: String,
    context: android.content.Context,
    formatTime: (String) -> String = { formatTime24(it) },
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val types = listOf("daily" to "Daily", "weekly" to "Weekly", "custom" to "Custom")
            types.forEach { (key, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .clickable { onScheduleTypeChange(key) }
                    .padding(end = 12.dp),
                ) {
                    RadioButton(selected = scheduleType == key, onClick = { onScheduleTypeChange(key) })
                    Spacer(Modifier.width(2.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (scheduleType == "custom") {
            Spacer(Modifier.height(8.dp))
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            FlowRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                dayNames.forEachIndexed { index, name ->
                    val isSelected = index in customDaysList
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(36.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                CircleShape,
                            )
                            .clickable {
                                onCustomDaysListChange(
                                    if (isSelected) customDaysList - index else customDaysList + index
                                )
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
                customHoursList.forEachIndexed { i, timeStr ->
                    val h = parseHour(timeStr); val m = parseMinute(timeStr)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)),
                        ) {
                            TextButton(
                                onClick = {
                                    TimePickerDialog(context, { _, newH, newM ->
                                        val newStr = String.format("%d:%02d", newH, newM)
                                        onCustomHoursListChange(
                                            customHoursList.toMutableList().also { it[i] = newStr }
                                        )
                                    }, h, m, false).show()
                                },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            ) {
                                Text(formatTime(timeStr), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (customHoursList.size > 1) {
                            TextButton(
                                onClick = {
                                    onCustomHoursListChange(
                                        customHoursList.toMutableList().also { it.removeAt(i) }
                                    )
                                },
                                modifier = Modifier.height(24.dp).width(20.dp),
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
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
                    TimePickerDialog(context, { _, h, m ->
                        val newStr = String.format("%d:%02d", h, m)
                        onCustomHoursListChange(customHoursList + newStr)
                    }, 9, 0, false).show()
                }, modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text("+ ${Strings.get("add_hour", lang)}", style = MaterialTheme.typography.bodySmall)
                }
            }
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
    manualEvents: List<Event> = emptyList(),
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
        val apiEvents = withContext(Dispatchers.IO) { MoodleApi(config.moodleUrl, token).fetchEvents(config.fetchDaysBack, config.fetchLimit) }
        onLog("${Strings.get("found_events", lang)} ${apiEvents.size}")
        val events = (apiEvents + manualEvents).sortedBy { it.timestart }

        if (icsEnabled) {
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

        onEventsFetched(apiEvents)

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
    onToggleCompletion: (String) -> Unit, onClearCompleted: () -> Unit,
    onEditTask: (Event) -> Unit, onDeleteTask: (String) -> Unit,
    lang: String,
    showIntro: Boolean = false,
    use24h: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        if (events.isEmpty()) {
            if (showIntro) {
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
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(48.dp))
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).then(Modifier.padding(bottom = 16.dp)),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Text(Strings.get("no_tasks", lang), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val sorted = events.sortedBy { it.timestart }
            val activeEvents = sorted.filter { completionMap[it.id] != true }
            val completedEvents = sorted.filter { completionMap[it.id] == true }

            val activeGrouped = activeEvents.groupBy { it.course.ifBlank { "General" } }.toSortedMap()
            activeGrouped.forEach { (course, courseEvents) ->
                Text(course, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                courseEvents.forEach { ev ->
                    val isExpanded = expandedId == ev.id
                    TaskCard(
                        event = ev, isDone = false, isExpanded = isExpanded,
                        onToggle = { onToggleCompletion(ev.id) },
                        onExpand = { onExpandedChange(if (isExpanded) null else ev.id) },
                        onEdit = if (ev.isManual) {{ onEditTask(ev) }} else null,
                        onDelete = if (ev.isManual) {{ onDeleteTask(ev.id) }} else null,
                        lang = lang, context = context, use24h = use24h,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (completedEvents.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                var showCompleted by remember { mutableStateOf(false) }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showCompleted = !showCompleted }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(Strings.get("completed_tasks", lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        imageVector = if (showCompleted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showCompleted) Strings.get("collapse", lang) else "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                if (showCompleted) {
                    val completedGrouped = completedEvents.groupBy { it.course.ifBlank { "General" } }.toSortedMap()
                    completedGrouped.forEach { (course, courseEvents) ->
                        Text(course, style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        courseEvents.forEach { ev ->
                            val isExpanded = expandedId == ev.id
                            TaskCard(
                                event = ev, isDone = true, isExpanded = isExpanded,
                                onToggle = { onToggleCompletion(ev.id) },
                                onExpand = { onExpandedChange(if (isExpanded) null else ev.id) },
                                onEdit = if (ev.isManual) {{ onEditTask(ev) }} else null,
                                onDelete = if (ev.isManual) {{ onDeleteTask(ev.id) }} else null,
                                lang = lang, context = context, use24h = use24h,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
                HorizontalDivider()
                if (completionMap.any { it.value }) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onClearCompleted,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(Strings.get("clear_completed", lang), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

    }

}

}

@Composable
private fun TaskCard(
    event: Event, isDone: Boolean, isExpanded: Boolean,
    onToggle: () -> Unit, onExpand: () -> Unit,
    onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null,
    lang: String, context: android.content.Context,
    use24h: Boolean = true,
) {
    val nowSeconds = System.currentTimeMillis() / 1000
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = event.timestart * 1000 }
    val isOverdue = event.timestart <= nowSeconds
    val diffDays = {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
        val n = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0); c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        n.set(java.util.Calendar.HOUR_OF_DAY, 0); n.set(java.util.Calendar.MINUTE, 0); n.set(java.util.Calendar.SECOND, 0); n.set(java.util.Calendar.MILLISECOND, 0)
        ((c.timeInMillis - n.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    }()
    val timeStr = com.moodlebridge.data.Strings.formatTimestamp(event.timestart, use24h)
    val relativeDate = when {
        isOverdue -> Strings.get("tasks_overdue", lang)
        diffDays == 0 -> "${Strings.get("tasks_today", lang)} $timeStr"
        diffDays == 1 -> "${Strings.get("tasks_tomorrow", lang)} $timeStr"
        diffDays <= 7 -> Strings.get("tasks_in_days", lang).replace("{n}", diffDays.toString())
        else -> {
            val locale = if (lang == "es") java.util.Locale("es") else java.util.Locale.ENGLISH
            val sdf = java.text.SimpleDateFormat("MMMM dd", locale)
            sdf.format(java.util.Date(event.timestart * 1000))
        }
    }

    val overdueColor = MaterialTheme.colorScheme.error
    val dateColor = when {
        isOverdue -> overdueColor
        diffDays <= 2 -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF22C55E)
    }
    val dateIcon: androidx.compose.ui.graphics.vector.ImageVector
    val dateTint: Color
    when {
        isOverdue -> { dateIcon = Icons.Default.Warning; dateTint = overdueColor }
        diffDays <= 2 -> { dateIcon = Icons.Default.DateRange; dateTint = MaterialTheme.colorScheme.tertiary }
        else -> { dateIcon = Icons.Default.DateRange; dateTint = Color(0xFF22C55E) }
    }

    val cardBg by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.surfaceVariant
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
    )
    val cardAlpha = if (isDone && !isExpanded) 0.65f else 1f

    Card(
        modifier = Modifier.fillMaxWidth().alpha(cardAlpha).animateContentSize(tween(250)).clickable { onExpand() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
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
            Spacer(Modifier.width(8.dp))
            Text(
                text = event.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                maxLines = if (isDone && !isExpanded) 1 else Int.MAX_VALUE,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = dateIcon,
                    contentDescription = relativeDate,
                    modifier = Modifier.size(14.dp),
                    tint = dateTint,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = relativeDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = dateTint,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) Strings.get("collapse", lang) else "Expand",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 42.dp, end = 12.dp, bottom = 10.dp)) {
                if (event.isManual) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Strings.get("manual", lang), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (event.description.isNotBlank()) {
                    Text(event.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.isManual && onEdit != null) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(Strings.get("edit_task", lang), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (event.isManual && onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(Strings.get("delete_task", lang), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (event.url.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val i = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(event.url))
                                context.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(Strings.get("open_in_browser", lang), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = onExpand,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(Strings.get("collapse", lang), style = MaterialTheme.typography.bodySmall)
                    }
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
