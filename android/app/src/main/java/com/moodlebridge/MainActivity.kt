package com.moodlebridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import com.moodlebridge.data.MergeResult
import com.moodlebridge.data.SyncManager
import com.moodlebridge.worker.NotificationWorker
import com.moodlebridge.worker.TaskReminderWorker
import com.moodlebridge.worker.SyncWorker
import androidx.work.WorkManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import androidx.concurrent.futures.await

import androidx.glance.appwidget.updateAll
import com.moodlebridge.widget.TaskWidget
import com.moodlebridge.widget.FetchTaskWidget

class MainActivity : ComponentActivity() {
    private var lastNetworkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = ConfigStore(this)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        TaskReminderWorker.schedule(this)

        val cm = getSystemService(android.net.ConnectivityManager::class.java)
        val currentNetId = cm.activeNetwork
        val lastNetId = prefs().getString("last_network_id", null)
        if (lastNetId != null && currentNetId?.toString() != lastNetId && config.lastSyncUrl.isNotBlank()) {
            config.lastSyncUrl = ""
            android.util.Log.d("DueNest", "Network changed — cleared sync URL")
        }
        prefs().edit().putString("last_network_id", currentNetId?.toString() ?: "").apply()

        setContent { MainContent(config = config, autoSync = intent?.getStringExtra("sync") == "true") }
    }

    private fun prefs() = getSharedPreferences("duenest_network", android.content.Context.MODE_PRIVATE)
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

private val SYNC_INTERVAL_OPTIONS = listOf(0, 15, 30, 60, 300, 600, 1800)

private fun syncIntervalLabel(seconds: Int, lang: String): String = when (seconds) {
    0 -> Strings.get("sync_manual", lang)
    15 -> Strings.get("sync_15s", lang)
    30 -> Strings.get("sync_30s", lang)
    60 -> Strings.get("sync_1m", lang)
    300 -> Strings.get("sync_5m", lang)
    600 -> Strings.get("sync_10m", lang)
    1800 -> Strings.get("sync_30m", lang)
    else -> Strings.get("sync_30s", lang)
}

private fun syncIntervalFromLabel(label: String): Int = when (label) {
    Strings.get("sync_manual", "en"), Strings.get("sync_manual", "es") -> 0
    Strings.get("sync_15s", "en"), Strings.get("sync_15s", "es") -> 15
    Strings.get("sync_30s", "en"), Strings.get("sync_30s", "es") -> 30
    Strings.get("sync_1m", "en"), Strings.get("sync_1m", "es") -> 60
    Strings.get("sync_5m", "en"), Strings.get("sync_5m", "es") -> 300
    Strings.get("sync_10m", "en"), Strings.get("sync_10m", "es") -> 600
    Strings.get("sync_30m", "en"), Strings.get("sync_30m", "es") -> 1800
    else -> 30
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
    var showSettingsPage by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var errorDialogMsg by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf(config.lastSyncMessage) }
    val logLines = remember { mutableStateListOf<String>() }
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
    var deletedEventIds by remember { mutableStateOf(
        try { Json.decodeFromString<Set<String>>(config.deletedEventIds) } catch (_: Exception) { emptySet() }
    ) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }
    var tasksEnabled by remember { mutableStateOf(config.tasksEnabled) }
    var tasksOutputPath by remember { mutableStateOf(config.tasksOutputPath) }
    var syncIntervalLbl by remember { mutableStateOf(syncIntervalLabel(config.syncIntervalSeconds, lang)) }

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
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncScanMode by remember { mutableStateOf(true) }
    var syncMergeMsg by remember { mutableStateOf<String?>(null) }
    var persistMergeRef by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            syncScanMode = false
            syncMergeMsg = Strings.get("sync_error", lang)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val bytes = inputStream.readBytes()
            inputStream.close()
            val payload = if (mimeType == "application/json" || uri.lastPathSegment?.endsWith(".json") == true) {
                SyncManager.deserializePayload(bytes.decodeToString())
            } else {
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@rememberLauncherForActivityResult
                val w = bmp.width; val h = bmp.height
                val pixels = IntArray(w * h)
                bmp.getPixels(pixels, 0, w, 0, 0, w, h)
                val source = RGBLuminanceSource(w, h, pixels)
                val qrResult = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)))
                val (_, payload) = SyncManager.decodeQrContent(qrResult.text)
                payload
            }
            if (payload != null) {
                val syncPayload = SyncManager.generatePayload(manualEvents, taskCompletionMap, deletedEventIds, config.deviceId, config.syncIntervalSeconds)
                val merged = SyncManager.mergePayload(syncPayload, payload)
                manualEvents = merged.manualEvents
                taskCompletionMap = merged.taskCompletion
                deletedEventIds = merged.deletedEventIds
                config.manualEventCache = Json.encodeToString(manualEvents)
                config.taskCompletionState = Json.encodeToString(taskCompletionMap)
                config.deletedEventIds = Json.encodeToString(deletedEventIds)
                persistMergeRef?.invoke()
            }
        } catch (_: Exception) {}
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val syncPayload = SyncManager.generatePayload(manualEvents, taskCompletionMap, deletedEventIds, config.deviceId, config.syncIntervalSeconds)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(SyncManager.serializePayload(syncPayload).toByteArray())
            }
        } catch (_: Exception) {}
    }

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
    persistMergeRef = { persistMergedEvents() }

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
        deletedEventIds = deletedEventIds + id
        config.deletedEventIds = Json.encodeToString(deletedEventIds)
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
            val params = FetchParams(
                url = url, username = username, password = pw, savePw = savePw,
                icsEnabled = icsEnabled, logseqEnabled = logseqEnabled, obsidianEnabled = obsidianEnabled,
                icsPath = icsPath, logseqPath = logseqPath, obsidianPath = obsidianPath,
                daysBackText = daysBackText, limitText = limitText, lang = lang,
                tasksEnabled = tasksEnabled, tasksOutputPath = tasksOutputPath, manualEvents = manual,
            )
            doFetch(context, config, params,
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
        LaunchedEffect(Unit) {
            val androidIp: String? = try {
                java.net.NetworkInterface.getNetworkInterfaces().toList().asSequence()
                    .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                    .flatMap { it.inetAddresses.toList().asSequence() }
                    .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                    ?.hostAddress
            } catch (_: Exception) { null }

            var firstRun = true
            while (true) {
                val interval = config.syncIntervalSeconds
                if (interval > 0) {
                    val savedUrl = config.lastSyncUrl
                    if (savedUrl.isNotBlank()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val sp = SyncManager.generatePayload(manualEvents, taskCompletionMap, deletedEventIds, config.deviceId, config.syncIntervalSeconds)
                                val client = OkHttpClient.Builder()
                                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                val reqBody = SyncManager.serializePayload(sp)
                                    .toRequestBody("text/plain".toMediaType())
                                val reqBuilder = OkHttpRequest.Builder().url("$savedUrl/sync").post(reqBody)
                                if (androidIp != null) reqBuilder.addHeader("X-Sync-Server-URL", "http://$androidIp:8766")
                                val resp = client.newCall(reqBuilder.build()).execute()
                                val respBody = resp.body?.string()
                                if (resp.isSuccessful && respBody != null) {
                                    val peerUrl = resp.header("X-Sync-Server-URL")
                                    if (!peerUrl.isNullOrBlank()) config.lastSyncUrl = peerUrl
                                    val remote = SyncManager.deserializePayload(respBody)
                                    if (remote != null) {
                                        val merged = SyncManager.mergePayload(sp, remote)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (merged.manualEvents != manualEvents || merged.taskCompletion != taskCompletionMap || merged.deletedEventIds != deletedEventIds) {
                                                manualEvents = merged.manualEvents
                                                taskCompletionMap = merged.taskCompletion
                                                deletedEventIds = merged.deletedEventIds
                                                config.manualEventCache = Json.encodeToString(manualEvents)
                                                config.taskCompletionState = Json.encodeToString(taskCompletionMap)
                                                config.deletedEventIds = Json.encodeToString(deletedEventIds)
                                                persistMergeRef?.invoke()
                                                Log.d("DueNest", "Auto-sync applied: ${merged.manualEvents.size} events")
                                            }
                                            if (merged.syncIntervalSeconds != config.syncIntervalSeconds) {
                                                config.syncIntervalSeconds = merged.syncIntervalSeconds
                                                syncIntervalLbl = syncIntervalLabel(merged.syncIntervalSeconds, lang)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d("DueNest", "Auto-sync skipped: ${e.message}")
                            }
                        }
                    }
                }
                val delayMs = if (firstRun) { firstRun = false; 1_000 } else (interval * 1000L).coerceAtLeast(1_000)
                if (interval > 0) kotlinx.coroutines.delay(delayMs) else kotlinx.coroutines.delay(60_000)
            }
        }

        val syncServerChannel = remember { kotlinx.coroutines.channels.Channel<MergeResult>(kotlinx.coroutines.channels.Channel.CONFLATED) }

        LaunchedEffect(Unit) {
            val server = try {
                val s = object : fi.iki.elonen.NanoHTTPD(8766) {
                    override fun serve(session: fi.iki.elonen.NanoHTTPD.IHTTPSession): fi.iki.elonen.NanoHTTPD.Response {
                        if (session.method != fi.iki.elonen.NanoHTTPD.Method.POST) {
                            return newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "")
                        }
                        try {
                            val files = HashMap<String, String>()
                            session.parseBody(files)
                            val body = files["postData"] ?: ""
                            val remote = SyncManager.deserializePayload(body)
                            if (remote != null) {
                                val peerUrl = session.headers["x-sync-server-url"]
                                if (!peerUrl.isNullOrBlank() && config.lastSyncUrl != peerUrl) {
                                    config.lastSyncUrl = peerUrl
                                }
                                val le = try { Json.decodeFromString<List<Event>>(config.manualEventCache) } catch (_: Exception) { emptyList() }
                                val lc = try { Json.decodeFromString<Map<String, Boolean>>(config.taskCompletionState) } catch (_: Exception) { emptyMap() }
                                val ld = try { Json.decodeFromString<Set<String>>(config.deletedEventIds) } catch (_: Exception) { emptySet() }
                                val local = SyncManager.generatePayload(le, lc, ld, config.deviceId, config.syncIntervalSeconds)
                                val merged = SyncManager.mergePayload(local, remote)
                                val respBody = SyncManager.serializePayload(
                                    SyncManager.generatePayload(merged.manualEvents, merged.taskCompletion, merged.deletedEventIds, config.deviceId, merged.syncIntervalSeconds)
                                )
                                val androidIp: String? = try {
                                    java.net.NetworkInterface.getNetworkInterfaces().toList().asSequence()
                                        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                                        .flatMap { it.inetAddresses.toList().asSequence() }
                                        .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                                        ?.hostAddress
                                } catch (_: Exception) { null }
                                val resp = newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT, respBody)
                                if (androidIp != null) resp.addHeader("X-Sync-Server-URL", "http://$androidIp:8766")
                                syncServerChannel.trySend(merged)
                                return resp
                            } else {
                                return newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "")
                            }
                        } catch (_: Exception) {
                            return newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "")
                        }
                    }
                }
                s.start()
                s
            } catch (_: Exception) { null }

            try {
                for (merged in syncServerChannel) {
                    manualEvents = merged.manualEvents
                    taskCompletionMap = merged.taskCompletion
                    deletedEventIds = merged.deletedEventIds
                    config.manualEventCache = Json.encodeToString(manualEvents)
                    config.taskCompletionState = Json.encodeToString(taskCompletionMap)
                    config.deletedEventIds = Json.encodeToString(deletedEventIds)
                    if (merged.syncIntervalSeconds != config.syncIntervalSeconds) {
                        config.syncIntervalSeconds = merged.syncIntervalSeconds
                        syncIntervalLbl = syncIntervalLabel(merged.syncIntervalSeconds, lang)
                    }
                    persistMergeRef?.invoke()
                }
            } finally {
                server?.stop()
            }
        }

        if (showSettingsPage) {
            SettingsPage(
                themeMode = themeMode, onThemeModeChange = { themeMode = it; config.themeMode = it },
                selectedLang = selectedLang, onLangChange = { selectedLang = it; config.language = it },
                notif24hFormat = notif24hFormat, onNotif24hFormatChange = { notif24hFormat = it; config.notification24hFormat = it },
                icsEnabled = icsEnabled, onIcsEnabledChange = { icsEnabled = it; config.icsEnabled = it },
                logseqEnabled = logseqEnabled, onLogseqEnabledChange = { logseqEnabled = it; config.logseqEnabled = it },
                obsidianEnabled = obsidianEnabled, onObsidianEnabledChange = { obsidianEnabled = it; config.obsidianEnabled = it },
                icsPath = icsPath, onIcsPathChange = { icsPath = it; config.icsPath = it },
                logseqPath = logseqPath, onLogseqPathChange = { logseqPath = it; config.logseqPath = it },
                obsidianPath = obsidianPath, onObsidianPathChange = { obsidianPath = it; config.obsidianPath = it },
                tasksEnabled = tasksEnabled, onTasksEnabledChange = { tasksEnabled = it; config.tasksEnabled = it; regenerateTasksFile() },
                tasksOutputPath = tasksOutputPath, onTasksPathChange = { tasksOutputPath = it; config.tasksOutputPath = it; regenerateTasksFile() },
                daysBackText = daysBackText, onDaysBackChange = { daysBackText = it; config.fetchDaysBack = it.toIntOrNull() ?: 7 },
                limitText = limitText, onLimitChange = { limitText = it; config.fetchLimit = it.toIntOrNull() ?: 100 },
                syncIntervalLabel = syncIntervalLbl, onSyncIntervalChange = {
                    syncIntervalLbl = it; config.syncIntervalSeconds = syncIntervalFromLabel(it)
                },
                notifEnabled = notifEnabled, onNotifEnabledChange = { enabled ->
                    notifEnabled = enabled; config.notificationsEnabled = enabled
                    if (enabled) NotificationWorker.schedule(context) else NotificationWorker.cancel(context)
                },
                notifScheduleType = notifScheduleType, onNotifScheduleTypeChange = { key ->
                    notifScheduleType = key; config.notificationScheduleType = key; NotificationWorker.schedule(context)
                },
                notifCustomDaysList = notifCustomDaysList, onNotifCustomDaysListChange = { list ->
                    notifCustomDaysList = list; config.notificationCustomDays = list.sorted().joinToString(",")
                    NotificationWorker.schedule(context)
                },
                notifCustomHoursList = notifCustomHoursList, onNotifCustomHoursListChange = { list ->
                    notifCustomHoursList = list; config.notificationCustomHours = list.joinToString(",")
                    NotificationWorker.schedule(context)
                },
                taskRemindEnabled = taskRemindEnabled, onTaskRemindEnabledChange = { enabled ->
                    taskRemindEnabled = enabled; config.taskRemindersEnabled = enabled
                    if (enabled) TaskReminderWorker.schedule(context) else TaskReminderWorker.cancel(context)
                },
                taskRemindScheduleType = taskRemindScheduleType, onTaskRemindScheduleTypeChange = { key ->
                    taskRemindScheduleType = key; config.taskReminderScheduleType = key; TaskReminderWorker.schedule(context)
                },
                taskRemindCustomDaysList = taskRemindCustomDaysList, onTaskRemindCustomDaysListChange = { list ->
                    taskRemindCustomDaysList = list; config.taskReminderCustomDays = list.sorted().joinToString(",")
                    TaskReminderWorker.schedule(context)
                },
                taskRemindCustomHoursList = taskRemindCustomHoursList, onTaskRemindCustomHoursListChange = { list ->
                    taskRemindCustomHoursList = list; config.taskReminderCustomHours = list.joinToString(",")
                    TaskReminderWorker.schedule(context)
                },
                lang = lang,
                onBack = { showSettingsPage = false },
                onClearCreds = { showClearCredsConfirm = true },
                onImportFromFile = { importFileLauncher.launch(arrayOf("application/json", "image/*")) },
                onExportToFile = { showExportFormatDialog = true },
                formatTime = formatTimeFn,
                context = context,
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
                        IconButton(onClick = { showSyncDialog = true; syncScanMode = true; syncMergeMsg = null }) {
                            Icon(painterResource(R.drawable.ic_qr), contentDescription = Strings.get("sync", lang),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showSettingsPage = true }) {
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
                                lang = lang)

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
                    config.clear()
                    password = ""
                    url = ""
                    username = ""
                    manualEvents = emptyList()
                    taskCompletionMap = emptyMap()
                    deletedEventIds = emptySet()
                    fetchedEvents = emptyList()
                    isWorking = false
                    statusText = ""
                    tasksEnabled = true
                    tasksOutputPath = ""
                    showSettingsPage = false
                    showClearCredsConfirm = false
                }) { Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCredsConfirm = false }) { Text(Strings.get("cancel", lang)) }
            })
    }

    // ── Export format dialog ──────────────────────────────
    if (showExportFormatDialog) {
        AlertDialog(onDismissRequest = { showExportFormatDialog = false },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(Strings.get("export_format", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        showExportFormatDialog = false
                        exportJsonLauncher.launch("duenest-sync.json")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.get("export_as_json", lang))
                    }
                    OutlinedButton(onClick = {
                        showExportFormatDialog = false
                        try {
                            val syncPayload = SyncManager.generatePayload(manualEvents, taskCompletionMap, deletedEventIds, config.deviceId, config.syncIntervalSeconds)
                            val ms = SyncManager.serializePayload(syncPayload)
                            val writer = QRCodeWriter()
                            val matrix = writer.encode(ms, BarcodeFormat.QR_CODE, 400, 400)
                            val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
                            for (x in 0 until 400) for (y in 0 until 400)
                                bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            val file = File(context.cacheDir, "ics/duenest-qr.png")
                            file.parentFile?.mkdirs()
                            java.io.FileOutputStream(file).use { out ->
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, Strings.get("sync_export_qr", lang)))
                        } catch (_: Exception) {}
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.get("export_as_qr", lang))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExportFormatDialog = false }) { Text(Strings.get("cancel", lang)) } })
    }

    // ── Sync dialog ──────────────────────────────────────
    if (showSyncDialog) {
        val syncPayload = remember(manualEvents, taskCompletionMap, deletedEventIds) {
            SyncManager.generatePayload(manualEvents, taskCompletionMap, deletedEventIds, config.deviceId, config.syncIntervalSeconds)
        }

        if (syncScanMode && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            LaunchedEffect(Unit) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
        }

        val qrContent = remember(syncPayload) {
            val androidIp: String? = try {
                java.net.NetworkInterface.getNetworkInterfaces().toList().asSequence()
                    .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                    .flatMap { it.inetAddresses.toList().asSequence() }
                    .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                    ?.hostAddress
            } catch (_: Exception) { null }
            if (androidIp != null) SyncManager.encodeQrContent("http://$androidIp:8766", syncPayload)
            else SyncManager.serializePayload(syncPayload)
        }
        val qrImage: ImageBitmap? = remember(qrContent) {
            try {
                val writer = QRCodeWriter()
                val matrix: BitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 400, 400)
                val w = matrix.width
                val h = matrix.height
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                for (x in 0 until w) {
                    for (y in 0 until h) {
                        bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                bmp.asImageBitmap()
            } catch (_: Exception) { null }
        }

        val scanChannel = remember { kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.CONFLATED) }
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        DisposableEffect(Unit) {
            onDispose { cameraExecutor.shutdown() }
        }

        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
            title = { Text(Strings.get("sync_qr_title", lang), modifier = Modifier.width(250.dp)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(250.dp)) {
                    if (syncScanMode) {
                        Box(
                            modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            val previewView = remember { PreviewView(context) }
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp))
                            )
                            LaunchedEffect(Unit) {
                                try {
                                    val cameraProvider = ProcessCameraProvider.getInstance(context).await()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        decodeQrFromImage(imageProxy) { content ->
                                            scanChannel.trySend(content)
                                        }
                                    }
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, imageAnalysis
                                    )
                                } catch (_: Exception) {}
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { syncScanMode = false }) {
                            Text(Strings.get("sync_show_qr", lang))
                        }
                    } else {
                        if (qrImage != null) {
                            Image(qrImage, contentDescription = "QR Code",
                                modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp)))
                        } else {
                            Text(Strings.get("sync_error", lang), color = MaterialTheme.colorScheme.error)
                        }
                        if (syncMergeMsg != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(syncMergeMsg!!, color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            syncScanMode = true
                            syncMergeMsg = null
                        }) {
                            Text(Strings.get("sync_scan_qr", lang))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSyncDialog = false }) { Text(Strings.get("cancel", lang)) } })

        LaunchedEffect(Unit) {
            for (content in scanChannel) {
                Log.d("DueNest", "QR raw content length: ${content.length}, starts: ${content.take(80)}")
                val (url, remotePayload) = SyncManager.decodeQrContent(content)
                Log.d("DueNest", "QR decoded: url=$url, remoteEvents=${remotePayload?.manualEvents?.size}, remoteCompletions=${remotePayload?.taskCompletion?.size}")
                val applyMerged: (MergeResult) -> Unit = { merged ->
                    manualEvents = merged.manualEvents
                    taskCompletionMap = merged.taskCompletion
                    deletedEventIds = merged.deletedEventIds
                    config.manualEventCache = Json.encodeToString(manualEvents)
                    config.taskCompletionState = Json.encodeToString(taskCompletionMap)
                    config.deletedEventIds = Json.encodeToString(deletedEventIds)
                    if (merged.syncIntervalSeconds != config.syncIntervalSeconds) {
                        config.syncIntervalSeconds = merged.syncIntervalSeconds
                        syncIntervalLbl = syncIntervalLabel(merged.syncIntervalSeconds, lang)
                    }
                    persistMergeRef?.invoke()
                    syncScanMode = false
                    syncMergeMsg = Strings.get("sync_merged", lang)
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1500)
                        showSyncDialog = false
                    }
                }
                if (url != null) {
                    Log.d("DueNest", "POST to $url/sync with ${syncPayload.manualEvents.size} events, ${syncPayload.taskCompletion.size} completions")
                    syncMergeMsg = Strings.get("sync_syncing", lang)
                    withContext(Dispatchers.IO) {
                        try {
                            val androidIp: String? = try {
                                java.net.NetworkInterface.getNetworkInterfaces().toList().asSequence()
                                    .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                                    .flatMap { it.inetAddresses.toList().asSequence() }
                                    .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                                    ?.hostAddress
                            } catch (_: Exception) { null }
                            val client = OkHttpClient.Builder()
                                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            val reqBody = SyncManager.serializePayload(syncPayload)
                                .toRequestBody("text/plain".toMediaType())
                            val reqBuilder = OkHttpRequest.Builder().url("$url/sync").post(reqBody)
                            if (androidIp != null) reqBuilder.addHeader("X-Sync-Server-URL", "http://$androidIp:8766")
                            val resp = client.newCall(reqBuilder.build()).execute()
                            val respBody = resp.body?.string()
                            Log.d("DueNest", "POST response: code=${resp.code}, bodyLen=${respBody?.length}")
                            if (resp.isSuccessful && respBody != null) {
                                val peerUrl = resp.header("X-Sync-Server-URL")
                                if (!peerUrl.isNullOrBlank()) config.lastSyncUrl = peerUrl
                                else config.lastSyncUrl = url
                                val remote = SyncManager.deserializePayload(respBody)
                                if (remote != null) {
                                    val merged = SyncManager.mergePayload(syncPayload, remote)
                                    withContext(Dispatchers.Main) { applyMerged(merged) }
                                } else if (remotePayload != null) {
                                    val merged = SyncManager.mergePayload(syncPayload, remotePayload)
                                    withContext(Dispatchers.Main) { applyMerged(merged) }
                                }
                            } else if (remotePayload != null) {
                                val merged = SyncManager.mergePayload(syncPayload, remotePayload)
                                withContext(Dispatchers.Main) { applyMerged(merged) }
                            }
                        } catch (e: Exception) {
                            Log.e("DueNest", "POST failed: ${e.message}")
                            if (remotePayload != null) {
                                val merged = SyncManager.mergePayload(syncPayload, remotePayload)
                                withContext(Dispatchers.Main) {
                                    applyMerged(merged)
                                    syncMergeMsg = Strings.get("sync_connection_failed", lang)
                                }
                            } else withContext(Dispatchers.Main) {
                                syncScanMode = false
                                syncMergeMsg = Strings.get("sync_connection_failed", lang)
                            }
                        }
                    }
                } else if (remotePayload != null) {
                    val merged = SyncManager.mergePayload(syncPayload, remotePayload)
                    applyMerged(merged)
                } else {
                    syncScanMode = false
                    syncMergeMsg = Strings.get("sync_no_data", lang)
                }
            }
        }
    }
    }
}

private fun decodeQrFromImage(imageProxy: ImageProxy, onFound: (String) -> Unit) {
    try {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val source = PlanarYUVLuminanceSource(
            bytes, imageProxy.width, imageProxy.height, 0, 0, imageProxy.width, imageProxy.height, false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = QRCodeReader().decode(binaryBitmap)
        onFound(result.text)
    } catch (_: Exception) {
    } finally {
        imageProxy.close()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsPage(
    themeMode: String, onThemeModeChange: (String) -> Unit,
    selectedLang: String, onLangChange: (String) -> Unit,
    notif24hFormat: Boolean, onNotif24hFormatChange: (Boolean) -> Unit,
    icsEnabled: Boolean, onIcsEnabledChange: (Boolean) -> Unit,
    logseqEnabled: Boolean, onLogseqEnabledChange: (Boolean) -> Unit,
    obsidianEnabled: Boolean, onObsidianEnabledChange: (Boolean) -> Unit,
    icsPath: String, onIcsPathChange: (String) -> Unit,
    logseqPath: String, onLogseqPathChange: (String) -> Unit,
    obsidianPath: String, onObsidianPathChange: (String) -> Unit,
    tasksEnabled: Boolean, onTasksEnabledChange: (Boolean) -> Unit,
    tasksOutputPath: String, onTasksPathChange: (String) -> Unit,
    daysBackText: String, onDaysBackChange: (String) -> Unit,
    limitText: String, onLimitChange: (String) -> Unit,
    syncIntervalLabel: String, onSyncIntervalChange: (String) -> Unit,
    notifEnabled: Boolean, onNotifEnabledChange: (Boolean) -> Unit,
    notifScheduleType: String, onNotifScheduleTypeChange: (String) -> Unit,
    notifCustomDaysList: List<Int>, onNotifCustomDaysListChange: (List<Int>) -> Unit,
    notifCustomHoursList: List<String>, onNotifCustomHoursListChange: (List<String>) -> Unit,
    taskRemindEnabled: Boolean, onTaskRemindEnabledChange: (Boolean) -> Unit,
    taskRemindScheduleType: String, onTaskRemindScheduleTypeChange: (String) -> Unit,
    taskRemindCustomDaysList: List<Int>, onTaskRemindCustomDaysListChange: (List<Int>) -> Unit,
    taskRemindCustomHoursList: List<String>, onTaskRemindCustomHoursListChange: (List<String>) -> Unit,
    lang: String,
    onBack: () -> Unit,
    onClearCreds: () -> Unit,
    onImportFromFile: () -> Unit,
    onExportToFile: () -> Unit,
    formatTime: (String) -> String,
    context: Context,
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("settings", lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 32.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── General ──
                Text(Strings.get("general", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Language
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_language), null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("language", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    var langExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = it }) {
                        OutlinedTextField(value = Strings.langLabel(selectedLang), onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                            singleLine = true,
                            modifier = Modifier.widthIn(max = 160.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                            listOf("en" to "English", "es" to "Espa\u00f1ol").forEach { (code, name) ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { onLangChange(code); langExpanded = false })
                            }
                        }
                    }
                }

                // Theme
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val themeIcon: Painter = when (themeMode) {
                        "light" -> painterResource(R.drawable.ic_sun)
                        "solarized_light" -> painterResource(R.drawable.ic_solarized_light)
                        "high_contrast" -> painterResource(R.drawable.ic_high_contrast)
                        "amoled_dark" -> painterResource(R.drawable.ic_moon_amoled)
                        "sakura" -> painterResource(R.drawable.ic_sakura)
                        "solarized_dark" -> painterResource(R.drawable.ic_solarized_dark)
                        "nord" -> painterResource(R.drawable.ic_nord)
                        "dracula" -> painterResource(R.drawable.ic_dracula)
                        "catppuccin" -> painterResource(R.drawable.ic_catppuccin)
                        else -> painterResource(R.drawable.ic_moon_dark)
                    }
                    Icon(themeIcon, null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("theme", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    var themeExpanded by remember { mutableStateOf(false) }
                    val themeOptions = listOf(
                        "system" to Strings.get("theme_system", lang),
                        "light" to Strings.get("theme_light", lang),
                        "dark" to Strings.get("theme_dark", lang),
                        "amoled_dark" to Strings.get("theme_amoled", lang),
                        "solarized_light" to Strings.get("theme_solarized_light", lang),
                        "solarized_dark" to Strings.get("theme_solarized_dark", lang),
                        "nord" to Strings.get("theme_nord", lang),
                        "dracula" to Strings.get("theme_dracula", lang),
                        "catppuccin" to Strings.get("theme_catppuccin", lang),
                        "high_contrast" to Strings.get("theme_high_contrast", lang),
                        "sakura" to Strings.get("theme_sakura", lang),
                    )
                    ExposedDropdownMenuBox(expanded = themeExpanded, onExpandedChange = { themeExpanded = it }) {
                        OutlinedTextField(
                            value = themeOptions.firstOrNull { it.first == themeMode }?.second ?: "",
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            singleLine = true,
                            modifier = Modifier.widthIn(max = 160.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                                    themeOptions.forEach { (value, label) ->
                                        val icon: Painter = when (value) {
                                            "light" -> painterResource(R.drawable.ic_sun)
                                            "solarized_light" -> painterResource(R.drawable.ic_solarized_light)
                                            "high_contrast" -> painterResource(R.drawable.ic_high_contrast)
                                            "amoled_dark" -> painterResource(R.drawable.ic_moon_amoled)
                                            "sakura" -> painterResource(R.drawable.ic_sakura)
                                            "solarized_dark" -> painterResource(R.drawable.ic_solarized_dark)
                                            "nord" -> painterResource(R.drawable.ic_nord)
                                            "dracula" -> painterResource(R.drawable.ic_dracula)
                                            "catppuccin" -> painterResource(R.drawable.ic_catppuccin)
                                            else -> painterResource(R.drawable.ic_moon_dark)
                                        }
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(icon, null, modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(8.dp))
                                            Text(label)
                                        }
                                    },
                                    onClick = { onThemeModeChange(value); themeExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Time Format
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("time_format", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    var timeExpanded by remember { mutableStateOf(false) }
                    val timeOptions = listOf(Strings.get("time_12h", lang), Strings.get("time_24h", lang))
                    ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = { timeExpanded = it }) {
                        OutlinedTextField(
                            value = if (notif24hFormat) Strings.get("time_24h", lang) else Strings.get("time_12h", lang),
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                            singleLine = true,
                            modifier = Modifier.widthIn(max = 120.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                            timeOptions.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { onNotif24hFormatChange(opt == Strings.get("time_24h", lang)); timeExpanded = false })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Notifications (Android only) ──
                Text(Strings.get("notifications", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notifEnabled, onCheckedChange = onNotifEnabledChange)
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.get("notif_enable", lang), style = MaterialTheme.typography.bodySmall)
                }
                AnimatedVisibility(visible = notifEnabled) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ReminderScheduleSection(
                                scheduleType = notifScheduleType, onScheduleTypeChange = onNotifScheduleTypeChange,
                                customDaysList = notifCustomDaysList, onCustomDaysListChange = onNotifCustomDaysListChange,
                                customHoursList = notifCustomHoursList, onCustomHoursListChange = onNotifCustomHoursListChange,
                                lang = lang, context = context, formatTime = formatTime,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = taskRemindEnabled, onCheckedChange = onTaskRemindEnabledChange)
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.get("notif_task_enable", lang), style = MaterialTheme.typography.bodySmall)
                }
                AnimatedVisibility(visible = taskRemindEnabled) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ReminderScheduleSection(
                                scheduleType = taskRemindScheduleType, onScheduleTypeChange = onTaskRemindScheduleTypeChange,
                                customDaysList = taskRemindCustomDaysList, onCustomDaysListChange = onTaskRemindCustomDaysListChange,
                                customHoursList = taskRemindCustomHoursList, onCustomHoursListChange = onTaskRemindCustomHoursListChange,
                                lang = lang, context = context, formatTime = formatTime,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Output ──
                Text(Strings.get("output", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // ICS
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("ics_label", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = icsEnabled, onCheckedChange = onIcsEnabledChange)
                }
                if (icsEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_folder), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = icsPath, onValueChange = onIcsPathChange, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { icsPicker.launch("calendar.ics") }) { Text(Strings.get("browse", lang)) }
                    }
                }

                // Logseq
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_logseq), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("logseq_label", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = logseqEnabled, onCheckedChange = onLogseqEnabledChange)
                }
                if (logseqEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_folder), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = logseqPath, onValueChange = onLogseqPathChange, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { logseqPicker.launch(null) }) { Text(Strings.get("browse", lang)) }
                    }
                }

                // Obsidian
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_obsidian), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("obsidian_label", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = obsidianEnabled, onCheckedChange = onObsidianEnabledChange)
                }
                if (obsidianEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_folder), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = obsidianPath, onValueChange = onObsidianPathChange, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { obsidianPicker.launch(null) }) { Text(Strings.get("browse", lang)) }
                    }
                }

                // DueNest
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_folder), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.get("tasks_path", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = tasksEnabled, onCheckedChange = onTasksEnabledChange)
                }
                if (tasksEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_folder), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = tasksOutputPath, onValueChange = onTasksPathChange, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { tasksPicker.launch("tasks.md") }) { Text(Strings.get("browse", lang)) }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Advanced ──
                Text(Strings.get("advanced", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.get("fetch_days", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = daysBackText, onValueChange = { onDaysBackChange(it.filter { c -> c.isDigit() }) },
                        singleLine = true, modifier = Modifier.widthIn(max = 100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.get("fetch_limit", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = limitText, onValueChange = { onLimitChange(it.filter { c -> c.isDigit() }) },
                        singleLine = true, modifier = Modifier.widthIn(max = 100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.get("sync_interval", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    var syncIntervalExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = syncIntervalExpanded, onExpandedChange = { syncIntervalExpanded = it }) {
                        OutlinedTextField(
                            value = syncIntervalLabel,
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = syncIntervalExpanded) },
                            singleLine = true,
                            modifier = Modifier.widthIn(max = 160.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = syncIntervalExpanded, onDismissRequest = { syncIntervalExpanded = false }) {
                            SYNC_INTERVAL_OPTIONS.forEach { seconds ->
                                val label = syncIntervalLabel(seconds, lang)
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { onSyncIntervalChange(label); syncIntervalExpanded = false })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                HorizontalDivider()
                Text(Strings.get("data_management", lang), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onImportFromFile) {
                        Text(Strings.get("sync_import", lang))
                    }
                    TextButton(onClick = onExportToFile) {
                        Text(Strings.get("sync_export", lang))
                    }
                    TextButton(onClick = onClearCreds) {
                        Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

data class FetchParams(
    val url: String,
    val username: String,
    val password: String,
    val savePw: Boolean,
    val icsEnabled: Boolean,
    val logseqEnabled: Boolean,
    val obsidianEnabled: Boolean,
    val icsPath: String,
    val logseqPath: String,
    val obsidianPath: String,
    val daysBackText: String,
    val limitText: String,
    val lang: String,
    val tasksEnabled: Boolean,
    val tasksOutputPath: String,
    val manualEvents: List<Event> = emptyList(),
)

private suspend fun doFetch(
    context: Context, config: ConfigStore, params: FetchParams,
    onLog: (String) -> Unit, onStatus: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit,
    onEventsFetched: (List<Event>) -> Unit = {},
) {
    val (url, username, password, savePw, icsEnabled, logseqEnabled, obsidianEnabled,
         icsPath, logseqPath, obsidianPath, daysBackText, limitText, lang,
         tasksEnabled, tasksOutputPath, manualEvents) = params

    try {
        config.moodleUrl = url.trimEnd('/'); config.username = username.trim()
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
    val relativeDate = if (isDone) {
        Strings.get("delivered", lang)
    } else when {
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
    val dateColor = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else when {
        isOverdue -> overdueColor
        diffDays <= 2 -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF22C55E)
    }
    val dateIcon: androidx.compose.ui.graphics.vector.ImageVector
    val dateTint: Color
    if (isDone) {
        dateIcon = Icons.Default.CheckCircle
        dateTint = MaterialTheme.colorScheme.onSurfaceVariant
    } else when {
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
    lang: String,
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
            }
        }
    }
}
