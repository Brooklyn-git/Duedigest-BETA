package com.moodlebridge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.moodlebridge.data.DesktopConfigStore
import com.moodlebridge.data.Event
import com.moodlebridge.data.MarkdownGenerator
import com.moodlebridge.data.MoodleApi
import com.moodlebridge.data.Strings
import com.moodlebridge.data.SyncManager
import com.moodlebridge.data.MergeResult
import com.moodlebridge.data.SyncPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.File
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter

private const val SYNC_PORT = 8765

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(config: DesktopConfigStore) {
    var themeMode by remember { mutableStateOf(config.themeMode) }
    var selectedLang by remember { mutableStateOf(config.language.ifBlank { "en" }) }
    val lang = selectedLang
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(config.moodleUrl) }
    var username by remember { mutableStateOf(config.username) }
    var password by remember { mutableStateOf(config.password) }
    var tz by remember { mutableStateOf(config.timezone.ifBlank { "UTC" }) }
    var passwordVisible by remember { mutableStateOf(false) }
    var logseqEnabled by remember { mutableStateOf(config.logseqEnabled) }
    var obsidianEnabled by remember { mutableStateOf(config.obsidianEnabled) }
    var logseqPath by remember { mutableStateOf(config.logseqPath) }
    var obsidianPath by remember { mutableStateOf(config.obsidianPath) }
    var daysBackText by remember { mutableStateOf(config.fetchDaysBack.toString()) }
    var limitText by remember { mutableStateOf(config.fetchLimit.toString()) }
    var savePw by remember { mutableStateOf(config.savePassword) }

    var use24h by remember { mutableStateOf(config.use24h) }
    var isWorking by remember { mutableStateOf(false) }
    var errorDialogMsg by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf(config.lastSyncMessage) }
    val logLines = remember { mutableStateListOf<String>() }
    var tzExpanded by remember { mutableStateOf(false) }

    var fetchedEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var manualEvents by remember {
        mutableStateOf<List<Event>>(
            try { Json.decodeFromString(config.manualEventCache) } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to parse manualEventCache", e); emptyList() }
        )
    }
    var taskCompletionMap by remember {
        mutableStateOf<Map<String, Boolean>>(
            try { Json.decodeFromString(config.taskCompletionState) } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to parse taskCompletionState", e); emptyMap() }
        )
    }
    var deletedEventIds by remember {
        mutableStateOf<Set<String>>(
            try { Json.decodeFromString<Set<String>>(config.deletedEventIds) } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to parse deletedEventIds", e); emptySet() }
        )
    }
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
    var showNewCourseDialog by remember { mutableStateOf(false) }
    var newCourseName by remember { mutableStateOf("") }
    var showSyncDialog by remember { mutableStateOf(false) }

    val courseOptions: List<String> by remember {
        derivedStateOf {
            val allEvents = (fetchedEvents as List<Event>) + (manualEvents as List<Event>)
            val courses = allEvents.map { e -> e.course }.filter { c -> c.isNotBlank() && c != "General" }.toMutableSet()
            courses.add("General")
            courses.sorted()
        }
    }

    fun addLog(msg: String) { logLines.add(msg) }

    fun saveCompletionMap() {
        config.taskCompletionState = Json.encodeToString(taskCompletionMap)
    }

    fun getMergedEvents(): List<Event> {
        val a: List<Event> = fetchedEvents.filter { it.id !in deletedEventIds }
        val b: List<Event> = manualEvents.filter { it.id !in deletedEventIds }
        return (a + b).sortedBy { it.timestart }
    }

    fun persistMergedEvents() {
        val merged = getMergedEvents()
        config.taskEventCache = Json.encodeToString(merged)
        if (tasksEnabled && tasksOutputPath.isNotBlank()) {
            val content = MarkdownGenerator.generateTasksList(merged, taskCompletionMap)
            File(tasksOutputPath).also { it.parentFile?.mkdirs() }?.writeText(content)
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
        val m: Map<String, Boolean> = taskCompletionMap
        val upd = HashMap<String, Boolean>(m)
        upd.remove(id)
        taskCompletionMap = upd
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
            dialogName = ""
            dialogDescription = ""
            dialogCourse = ""
            val now = java.util.Calendar.getInstance()
            val y = now.get(java.util.Calendar.YEAR)
            val m = String.format("%02d", now.get(java.util.Calendar.MONTH) + 1)
            val d = String.format("%02d", now.get(java.util.Calendar.DAY_OF_MONTH))
            dialogDate = "$y-$m-$d"
            val h = String.format("%02d", now.get(java.util.Calendar.HOUR_OF_DAY))
            val curMin = String.format("%02d", now.get(java.util.Calendar.MINUTE))
            dialogTime = "$h:$curMin"
            dialogDuration = "60"
            dialogUrl = ""
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
        addOrUpdateManualEvent(
            Event(
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
            )
        )
        showTaskDialog = false
        editingTask = null
    }

    fun doSync(pw: String) {
        isWorking = true
        logLines.clear()
        statusText = ""
        scope.launch {
            try {
                addLog(Strings.get("no_cached_token", lang))
                val effectiveToken = if (config.token.isNotBlank()) {
                    addLog("Using cached token")
                    config.token
                } else {
                    addLog(Strings.get("no_cached_token", lang))
                    val newToken = withContext(Dispatchers.IO) { MoodleApi.login(url, username, pw) }
                    config.token = newToken
                    addLog(Strings.get("login_success", lang))
                    newToken
                }
                addLog(Strings.get("fetching_events", lang))
                val api = MoodleApi(url, effectiveToken)
                val events = withContext(Dispatchers.IO) {
                    api.fetchEvents(daysBackText.toIntOrNull() ?: 7, limitText.toIntOrNull() ?: 100)
                }
                fetchedEvents = events
                expandedTaskId = null
                persistMergedEvents()

                val merged = getMergedEvents()
                addLog("${Strings.get("found_events", lang)} ${merged.size} events")

                if (logseqEnabled && logseqPath.isNotBlank()) {
                    val files = MarkdownGenerator.generateLogseq(merged)
                    val dir = File(logseqPath).also { it.mkdirs() }
                    for ((name, content) in files) File(dir, name).writeText(content)
                    addLog("Logseq files written to $logseqPath")
                }
                if (obsidianEnabled && obsidianPath.isNotBlank()) {
                    val files = MarkdownGenerator.generateObsidian(merged)
                    val dir = File(obsidianPath).also { it.mkdirs() }
                    for ((name, content) in files) File(dir, name).writeText(content)
                    addLog("Obsidian files written to $obsidianPath")
                }

                config.lastSyncTimestamp = System.currentTimeMillis() / 1000
                config.eventCount = merged.size
                val msg = "${Strings.get("done", lang)} — ${merged.size} events"
                config.lastSyncMessage = msg
                statusText = msg
                addLog(Strings.get("done", lang))
            } catch (e: Exception) {
                errorDialogMsg = e.message ?: "Unknown error"
                statusText = "${Strings.get("error", lang)}: ${e.message}"
                addLog("${Strings.get("error", lang)}: ${e.message}")
            } finally {
                isWorking = false
            }
        }
    }

    DueNestTheme(themeMode = themeMode) {
    val cs = MaterialTheme.colorScheme
    var sidebarExpanded by remember { mutableStateOf(true) }
    var selectedSection by remember { mutableStateOf(1) }

    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(if (sidebarExpanded) 240.dp else 48.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NavItem(
                    if (sidebarExpanded) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "Collapse", false, sidebarExpanded,
                ) { sidebarExpanded = !sidebarExpanded }
                HorizontalDivider()
                NavItem(Icons.Default.CheckCircle, Strings.get("tasks", lang), selectedSection == 1, sidebarExpanded) { selectedSection = 1 }
                HorizontalDivider()
                NavItem(Icons.Default.Build, Strings.get("connection", lang), selectedSection == 0, sidebarExpanded) { selectedSection = 0 }
                HorizontalDivider()
                NavItem(Icons.Default.Settings, Strings.get("settings", lang), selectedSection == 2, sidebarExpanded) { selectedSection = 2 }
                HorizontalDivider()
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight()) {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.get("app_title", lang), style = MaterialTheme.typography.headlineMedium)
                        Text(Strings.get("subtitle", lang), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { showSyncDialog = true }) {
                        Icon(AppIcons.Qr, contentDescription = Strings.get("sync", lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.height(100.dp),
            )
            when (selectedSection) {
                0 -> ConnectionTabContent(
                    url = url, onUrlChange = { url = it },
                    username = username, onUsernameChange = { username = it },
                    password = password, onPasswordChange = { password = it },
                    passwordVisible = passwordVisible, onPasswordVisibleChange = { passwordVisible = it },
                    tz = tz, onTzChange = { tz = it },
                    tzExpanded = tzExpanded, onTzExpandedChange = { tzExpanded = it },
                    savePw = savePw, onSavePwChange = { savePw = it },
                    isWorking = isWorking, statusText = statusText,
                    logLines = logLines.toList(),
                    lang = lang,
                    onFetch = { doSync(password) },
                )
                1 -> Box(Modifier.fillMaxSize()) {
                    val hasCompleted by remember {
                        derivedStateOf { taskCompletionMap.entries.any { it.value } }
                    }
                    TaskTabContent(
                        events = getMergedEvents(), completionMap = taskCompletionMap,
                        expandedId = expandedTaskId, onExpandedChange = { expandedTaskId = it },
                        onToggleCompletion = { id ->
                            val m: Map<String, Boolean> = taskCompletionMap
                            val cur = m.getOrElse(id) { false }
                            taskCompletionMap = HashMap(m).also { it[id] = !cur }
                            saveCompletionMap()
                            persistMergedEvents()
                        },
                        onClearCompleted = { showClearCompletedConfirm = true },
                        onEditTask = { openTaskDialog(it) },
                        onDeleteTask = { showDeleteConfirm = it },
                        isWorking = isWorking, lang = lang, use24h = use24h,
                        showClearCompleted = hasCompleted,
                        showIntro = config.lastSyncTimestamp == 0L && fetchedEvents.isEmpty(),
                    )
                    FloatingActionButton(
                        onClick = { openTaskDialog() },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = Strings.get("add_task", lang))
                    }
                }
                2 -> DesktopSettingsPage(
                    config = config,
                    themeMode = themeMode, onThemeModeChange = { themeMode = it },
                    selectedLang = selectedLang, onSelectedLangChange = { selectedLang = it },
                    use24h = use24h, onUse24hChange = { use24h = it },
                    logseqEnabled = logseqEnabled, onLogseqEnabledChange = { logseqEnabled = it },
                    logseqPath = logseqPath, onLogseqPathChange = { logseqPath = it },
                    obsidianEnabled = obsidianEnabled, onObsidianEnabledChange = { obsidianEnabled = it },
                    obsidianPath = obsidianPath, onObsidianPathChange = { obsidianPath = it },
                    tasksEnabled = tasksEnabled, onTasksEnabledChange = { tasksEnabled = it },
                    tasksOutputPath = tasksOutputPath, onTasksOutputPathChange = { tasksOutputPath = it },
                    daysBackText = daysBackText, onDaysBackTextChange = { daysBackText = it },
                    limitText = limitText, onLimitTextChange = { limitText = it },
                    onClearCredsClick = { showClearCredsConfirm = true },
                    lang = lang,
                )
            }
        }
    }

        // Dialogs
        if (showDeleteConfirm != null) {
            AlertDialog(onDismissRequest = { showDeleteConfirm = null },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("delete_task", lang)) },
                text = { Text(Strings.get("delete_task_confirm", lang)) },
                confirmButton = {
                    TextButton(onClick = { deleteManualEvent(showDeleteConfirm!!); showDeleteConfirm = null }) {
                        Text(Strings.get("delete_task", lang), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(Strings.get("cancel", lang)) } })
        }

        if (showTaskDialog) {
            val calendar = remember { java.util.Calendar.getInstance() }
            AlertDialog(
                onDismissRequest = { showTaskDialog = false; editingTask = null },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(if (editingTask != null) Strings.get("edit_task", lang) else Strings.get("add_task", lang)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()).width(520.dp)) {
                        OutlinedTextField(value = dialogName, onValueChange = { dialogName = it },
                            label = { Text(Strings.get("task_name", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = dialogDescription, onValueChange = { dialogDescription = it },
                            label = { Text(Strings.get("task_description", lang)) }, singleLine = false, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Spacer(Modifier.height(8.dp))
                        var courseExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = it }) {
                            OutlinedTextField(value = dialogCourse, onValueChange = {}, readOnly = true,
                                label = { Text(Strings.get("task_course", lang)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                                singleLine = true, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                            ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                                val opts: List<String> = courseOptions
                                opts.forEach { c ->
                                    DropdownMenuItem(text = { Text(c) }, onClick = { dialogCourse = c; courseExpanded = false })
                                }
                                if (courseOptions.isNotEmpty()) HorizontalDivider()
                                DropdownMenuItem(text = { Text("+ ${Strings.get("add_course", lang)}", color = MaterialTheme.colorScheme.primary) }, onClick = {
                                    courseExpanded = false; newCourseName = ""; showNewCourseDialog = true
                                })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        DatePickerRow(dateStr = dialogDate, onDateChange = { dialogDate = it }, lang = lang)
                        Spacer(Modifier.height(8.dp))
                        TimePickerRow(timeStr = dialogTime, onTimeChange = { dialogTime = it }, use24h = use24h)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = dialogDuration, onValueChange = { dialogDuration = it.filter { c -> c.isDigit() } },
                            label = { Text(Strings.get("task_duration", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = dialogUrl, onValueChange = { dialogUrl = it },
                            label = { Text(Strings.get("task_url", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { TextButton(onClick = { saveTaskDialog() }) { Text(Strings.get("save", lang)) } },
                dismissButton = { TextButton(onClick = { showTaskDialog = false; editingTask = null }) { Text(Strings.get("cancel", lang)) } })
        }

        if (showNewCourseDialog) {
            AlertDialog(onDismissRequest = { showNewCourseDialog = false },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("task_course", lang)) },
                text = {
                    OutlinedTextField(value = newCourseName, onValueChange = { newCourseName = it },
                        label = { Text(Strings.get("task_course", lang)) }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = { if (newCourseName.isNotBlank()) dialogCourse = newCourseName; showNewCourseDialog = false }) {
                        Text(Strings.get("save", lang))
                    }
                },
                dismissButton = { TextButton(onClick = { showNewCourseDialog = false }) { Text(Strings.get("cancel", lang)) } })
        }

        if (showClearCompletedConfirm) {
            AlertDialog(onDismissRequest = { showClearCompletedConfirm = false },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("clear_completed", lang)) },
                text = { Text(Strings.get("clear_completed_confirm", lang)) },
                confirmButton = {
                    TextButton(onClick = {
                        val m: Map<String, Boolean> = taskCompletionMap
                        val ids: List<String> = m.entries.filter { it.value == true }.map { it.key }
                        val events: List<Event> = manualEvents
                        for (id in ids) {
                            if (events.any { e -> e.id == id }) deleteManualEvent(id)
                        }
                        taskCompletionMap = emptyMap<String, Boolean>()
                        saveCompletionMap()
                        persistMergedEvents()
                        expandedTaskId = null
                        showClearCompletedConfirm = false
                    }) { Text(Strings.get("clear_completed", lang)) }
                },
                dismissButton = { TextButton(onClick = { showClearCompletedConfirm = false }) { Text(Strings.get("cancel", lang)) } })
        }

        if (showClearCredsConfirm) {
            AlertDialog(onDismissRequest = { showClearCredsConfirm = false },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("clear_creds", lang)) },
                text = { Text(Strings.get("clear_creds_confirm", lang)) },
                confirmButton = {
                    TextButton(onClick = {
                        config.clear()
                        url = ""; username = ""; password = ""
                        themeMode = "system"; selectedLang = "en"
                        tz = "UTC"; passwordVisible = false
                        logseqEnabled = false; obsidianEnabled = false
                        logseqPath = "logseq/"; obsidianPath = "obsidian/"
                        daysBackText = "7"; limitText = "100"
                        savePw = false; use24h = true
                        statusText = ""; logLines.clear()
                        fetchedEvents = emptyList(); manualEvents = emptyList()
                        taskCompletionMap = emptyMap(); deletedEventIds = emptySet()
                        tasksEnabled = true; tasksOutputPath = ""
                        expandedTaskId = null; isWorking = false
                        showClearCredsConfirm = false
                    }) { Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showClearCredsConfirm = false }) { Text(Strings.get("cancel", lang)) } })
        }

        if (errorDialogMsg != null) {
            AlertDialog(onDismissRequest = { errorDialogMsg = null },
                containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
                title = { Text(Strings.get("error", lang)) },
                text = { Text(errorDialogMsg ?: "") },
                confirmButton = { TextButton(onClick = { errorDialogMsg = null }) { Text("OK") } })
        }

        val syncChannel = remember { Channel<MergeResult>(Channel.CONFLATED) }

        val localIp = remember {
            try {
                NetworkInterface.getNetworkInterfaces().toList().asSequence()
                    .filter { it.isUp && !it.isLoopback && !it.isVirtual && it.name != "docker0" }
                    .filter { it.name.startsWith("eth") || it.name.startsWith("wlan") || it.name.startsWith("en") || it.name.startsWith("wl") || it.name.startsWith("wlp") }
                    .flatMap { it.inetAddresses.toList().asSequence() }
                    .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                    ?.hostAddress
                    ?: run {
                        NetworkInterface.getNetworkInterfaces().toList().asSequence()
                            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                            .flatMap { it.inetAddresses.toList().asSequence() }
                            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                            ?.hostAddress
                    }
            } catch (e: Exception) { com.moodlebridge.Log.w("Network", "Failed to detect local IP", e); null }
        }

        LaunchedEffect(Unit) {
            val server = try {
                val s = com.sun.net.httpserver.HttpServer.create(InetSocketAddress(SYNC_PORT), 0)
                s.createContext("/sync") { exchange ->
                    if (exchange.requestMethod == "POST") {
                        try {
                            val body = exchange.requestBody.readBytes().decodeToString()
                            val remote = SyncManager.deserializePayload(body)
                            if (remote != null) {
                                val le = try { Json.decodeFromString<List<Event>>(config.manualEventCache) } catch (e: Exception) { com.moodlebridge.Log.w("Sync", "Failed to parse manualEventCache", e); emptyList() }
                                val lc = try { Json.decodeFromString<Map<String, Boolean>>(config.taskCompletionState) } catch (e: Exception) { com.moodlebridge.Log.w("Sync", "Failed to parse taskCompletionState", e); emptyMap() }
                                val ld = try { Json.decodeFromString<Set<String>>(config.deletedEventIds) } catch (e: Exception) { com.moodlebridge.Log.w("Sync", "Failed to parse deletedEventIds", e); emptySet() }
                                val local = SyncManager.generatePayload(le, lc, ld, config.deviceId)
                                val merged = SyncManager.mergePayload(local, remote)
                                val response = SyncManager.serializePayload(
                                    SyncManager.generatePayload(merged.manualEvents, merged.taskCompletion, merged.deletedEventIds, config.deviceId)
                                )
                                exchange.responseHeaders.add("Content-Type", "text/plain")
                                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                                exchange.responseBody.write(response.toByteArray())
                                exchange.close()
                                syncChannel.trySend(merged)
                            } else {
                                exchange.sendResponseHeaders(400, 0)
                                exchange.close()
                            }
                        } catch (e: Exception) {
                            try { exchange.sendResponseHeaders(500, 0); exchange.close() } catch (_: Exception) {}
                        }
                    } else {
                        exchange.sendResponseHeaders(405, 0)
                        exchange.close()
                    }
                }
                s.executor = Executors.newSingleThreadExecutor()
                s.start()
                s
            } catch (e: Exception) {
                null
            }

            try {
                while (true) {
                    val merged = syncChannel.receive()
                    manualEvents = merged.manualEvents
                    taskCompletionMap = merged.taskCompletion
                    deletedEventIds = merged.deletedEventIds
                    config.manualEventCache = Json.encodeToString(manualEvents)
                    config.taskCompletionState = Json.encodeToString(taskCompletionMap)
                    config.deletedEventIds = Json.encodeToString(deletedEventIds)
                    persistMergedEvents()
                    kotlinx.coroutines.delay(1500)
                    showSyncDialog = false
                }
                awaitCancellation()
            } finally {
                server?.stop(0)
            }
        }

        if (showSyncDialog) {
            SyncDialog(
                onDismiss = { showSyncDialog = false },
                config = config,
                manualEvents = manualEvents,
                taskCompletionMap = taskCompletionMap,
                deletedEventIds = deletedEventIds,
                onStateUpdate = { me, tc, de ->
                    manualEvents = me; taskCompletionMap = tc; deletedEventIds = de
                },
                persistMergedEvents = { persistMergedEvents() },
                localIp = localIp,
                lang = lang,
                scope = scope,
            )
        }


    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
             else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TaskTabContent(
    events: List<Event>, completionMap: Map<String, Boolean>,
    expandedId: String?, onExpandedChange: (String?) -> Unit,
    onToggleCompletion: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onEditTask: (Event) -> Unit,
    onDeleteTask: (String) -> Unit,
    isWorking: Boolean, lang: String, use24h: Boolean,
    showClearCompleted: Boolean = false,
    showIntro: Boolean = false,
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(bottom = 72.dp)) {
            if (events.isEmpty() && showIntro) {
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
                val activeEvents = events.filter { completionMap[it.id] != true }
                val completedEvents = events.filter { completionMap[it.id] == true }

                val activeGrouped = activeEvents.groupBy { it.course.ifBlank { "General" } }.toSortedMap()
                activeGrouped.forEach { (course, courseEvents) ->
                    Text(course, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    courseEvents.forEach { ev ->
                        val isExpanded = expandedId == ev.id
                        TaskItem(
                            event = ev, isCompleted = false,
                            isExpanded = isExpanded,
                            onToggleCompletion = { onToggleCompletion(ev.id) },
                            onExpand = { onExpandedChange(if (isExpanded) null else ev.id) },
                            onEdit = if (ev.isManual) {{ onEditTask(ev) }} else {{ }},
                            onDelete = if (ev.isManual) {{ onDeleteTask(ev.id) }} else {{ }},
                            lang = lang, use24h = use24h,
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
                                TaskItem(
                                    event = ev, isCompleted = true,
                                    isExpanded = isExpanded,
                                    onToggleCompletion = { onToggleCompletion(ev.id) },
                                    onExpand = { onExpandedChange(if (isExpanded) null else ev.id) },
                                    onEdit = if (ev.isManual) {{ onEditTask(ev) }} else {{ }},
                                    onDelete = if (ev.isManual) {{ onDeleteTask(ev.id) }} else {{ }},
                                    lang = lang, use24h = use24h,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    HorizontalDivider()
                    if (showClearCompleted) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onClearCompleted,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(Strings.get("clear_completed", lang), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (events.isEmpty()) {
                    Spacer(Modifier.height(48.dp))
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).then(Modifier.padding(bottom = 16.dp)).align(Alignment.CenterHorizontally),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Text(Strings.get("no_tasks", lang), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    event: Event, isCompleted: Boolean, isExpanded: Boolean,
    onToggleCompletion: () -> Unit, onExpand: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit,
    lang: String, use24h: Boolean,
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
    val timeStr = Strings.formatTimestamp(event.timestart, use24h)
    val relativeDate = if (isCompleted) {
        Strings.get("delivered", lang)
    } else when {
        isOverdue -> Strings.get("tasks_overdue", "en")
        diffDays == 0 -> "${Strings.get("tasks_today", "en")} $timeStr"
        diffDays == 1 -> "${Strings.get("tasks_tomorrow", "en")} $timeStr"
        diffDays <= 7 -> Strings.get("tasks_in_days", "en").replace("{n}", diffDays.toString())
        else -> {
            val locale = if (lang == "es") java.util.Locale("es") else java.util.Locale.ENGLISH
            val sdf = java.text.SimpleDateFormat("MMMM dd", locale)
            sdf.format(java.util.Date(event.timestart * 1000))
        }
    }
    val overdueColor = MaterialTheme.colorScheme.error
    val dateColor = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else when {
        isOverdue -> overdueColor
        diffDays <= 2 -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF22C55E)
    }
    val dateIcon = if (isCompleted) Icons.Default.CheckCircle else when {
        isOverdue -> Icons.Default.Warning
        else -> Icons.Default.DateRange
    }
    val dateTint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else when {
        isOverdue -> overdueColor
        diffDays <= 2 -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF22C55E)
    }

    val cardBg by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
    )
    val cardAlpha = if (isCompleted && !isExpanded) 0.65f else 1f

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .alpha(cardAlpha)
            .animateContentSize(tween(250))
            .clickable { onExpand() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isCompleted, onCheckedChange = { onToggleCompletion() },
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
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isCompleted && !isExpanded) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
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
                    contentDescription = if (isExpanded) Strings.get("collapse", "en") else "Expand",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 42.dp, end = 12.dp, bottom = 10.dp)) {
                    if (event.isManual) {
                        Text(Strings.get("manual", "en"), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (event.description.isNotBlank()) {
                        Text(event.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (event.isManual) {
                            TextButton(onClick = onEdit) {
                                Text(Strings.get("edit_task", "en"), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = onDelete) {
                                Text(Strings.get("delete_task", "en"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (event.url.isNotBlank()) {
                            TextButton(onClick = {
                                try { Desktop.getDesktop().browse(URI(event.url)) } catch (_: Exception) {}
                            }) {
                                Text(Strings.get("open_in_browser", "en"), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = onExpand) {
                            Text(Strings.get("collapse", "en"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionTabContent(
    url: String, onUrlChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onPasswordVisibleChange: (Boolean) -> Unit,
    tz: String, onTzChange: (String) -> Unit,
    tzExpanded: Boolean, onTzExpandedChange: (Boolean) -> Unit,
    savePw: Boolean, onSavePwChange: (Boolean) -> Unit,
    isWorking: Boolean, statusText: String,
    logLines: List<String>,
    lang: String,
    onFetch: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Strings.get("log_in", lang), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = url, onValueChange = onUrlChange,
                label = { Text(Strings.get("moodle_url", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = username, onValueChange = onUsernameChange,
                label = { Text(Strings.get("username", lang)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = onPasswordChange,
                label = { Text(Strings.get("password", lang)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = savePw, onCheckedChange = onSavePwChange)
                Spacer(Modifier.width(4.dp))
                Text(Strings.get("store_pw", lang), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onFetch,
                    enabled = url.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !isWorking,
                    modifier = Modifier.widthIn(min = 200.dp)) {
                    Text(if (isWorking) Strings.get("working", lang) else Strings.get("fetch", lang))
                }
            }
            Text(Strings.get("log", lang), style = MaterialTheme.typography.labelMedium)
            Card(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    logLines.forEach { line -> Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                }
            }
        }
    }
}

private fun browseDirectory(current: String): String? {
    val chooser = JFileChooser(current.ifBlank { "." }).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select Directory"
        isAcceptAllFileFilterUsed = false
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val path = chooser.selectedFile.absolutePath
        File(path).mkdirs()
        path
    } else null
}

private fun browseFile(current: String): String? {
    val defaultFile = if (current.isNotBlank()) File(current)
                      else File(System.getProperty("user.home"), "tasks.md")
    defaultFile.parentFile?.mkdirs()
    val created = !defaultFile.exists() && defaultFile.writeText("").let { true }
    val chooser = JFileChooser(defaultFile.parentFile).apply {
        dialogTitle = "Select Tasks Output File"
        selectedFile = defaultFile
    }
    val result = chooser.showSaveDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        val path = chooser.selectedFile.absolutePath
        File(path).parentFile?.mkdirs()
        if (!File(path).exists()) File(path).writeText("")
        path
    } else {
        if (created) defaultFile.delete()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).widthIn(max = 140.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun DatePickerRow(
    dateStr: String,
    onDateChange: (String) -> Unit,
    lang: String,
) {
    val parts = dateStr.split("-")
    val curYear = parts.getOrNull(0) ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
    val curMonth = parts.getOrNull(1) ?: "01"
    val curDay = parts.getOrNull(2) ?: "01"
    val cal = java.util.Calendar.getInstance()
    val years = (cal.get(java.util.Calendar.YEAR) - 2..cal.get(java.util.Calendar.YEAR) + 5).map { it.toString() }
    val days = (1..31).map { String.format("%02d", it) }
    val locale = if (lang == "es") java.util.Locale("es") else java.util.Locale.ENGLISH
    val monthNames = remember { java.text.DateFormatSymbols.getInstance(locale).shortMonths.take(12) }
    val curMonthDisplay = remember(curMonth) { monthNames[curMonth.toInt() - 1] }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SimpleDropdown("Year", curYear, years, { y -> onDateChange("$y-$curMonth-$curDay") },
            modifier = Modifier.weight(1f))
        SimpleDropdown("Month", curMonthDisplay, monthNames, { m ->
            val num = String.format("%02d", monthNames.indexOf(m) + 1)
            onDateChange("$curYear-$num-$curDay")
        }, modifier = Modifier.weight(1f))
        SimpleDropdown("Day", curDay, days, { d -> onDateChange("$curYear-$curMonth-$d") },
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TimePickerRow(
    timeStr: String,
    onTimeChange: (String) -> Unit,
    use24h: Boolean,
) {
    val parts = timeStr.split(":")
    val curHour24 = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val curMin = parts.getOrNull(1)?.let { String.format("%02d", it.toIntOrNull() ?: 0) } ?: "00"
    val minutes = (0..59).map { String.format("%02d", it) }

    if (use24h) {
        val curHour = String.format("%02d", curHour24)
        val hours = (0..23).map { String.format("%02d", it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SimpleDropdown("Hour", curHour, hours, { h -> onTimeChange("$h:$curMin") },
                modifier = Modifier.weight(1f))
            SimpleDropdown("Min", curMin, minutes, { m -> onTimeChange("$curHour:$m") },
                modifier = Modifier.weight(1f))
        }
    } else {
        val isPm = curHour24 >= 12
        val displayHour = when {
            curHour24 == 0 -> 12
            curHour24 > 12 -> curHour24 - 12
            else -> curHour24
        }
        val curDisplay = String.format("%02d", displayHour)
        val hours12 = (1..12).map { String.format("%02d", it) }
        val ampmOptions = listOf("AM", "PM")
        val curAmpm = if (isPm) "PM" else "AM"

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SimpleDropdown("Hour", curDisplay, hours12, { h ->
                val hVal = h.toInt()
                val h24 = if (isPm) (if (hVal == 12) 12 else hVal + 12) else (if (hVal == 12) 0 else hVal)
                onTimeChange(String.format("%02d:%s", h24, curMin))
            }, modifier = Modifier.weight(1f))
            SimpleDropdown("Min", curMin, minutes, { m ->
                onTimeChange(String.format("%02d:%s", curHour24, m))
            }, modifier = Modifier.weight(1f))
            SimpleDropdown("AM/PM", curAmpm, ampmOptions, { a ->
                val isNowPm = a == "PM"
                val h24 = if (isNowPm) (if (displayHour == 12) 12 else displayHour + 12) else (if (displayHour == 12) 0 else displayHour)
                onTimeChange(String.format("%02d:%s", h24, curMin))
            }, modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopSettingsPage(
    config: DesktopConfigStore,
    themeMode: String, onThemeModeChange: (String) -> Unit,
    selectedLang: String, onSelectedLangChange: (String) -> Unit,
    use24h: Boolean, onUse24hChange: (Boolean) -> Unit,
    logseqEnabled: Boolean, onLogseqEnabledChange: (Boolean) -> Unit,
    logseqPath: String, onLogseqPathChange: (String) -> Unit,
    obsidianEnabled: Boolean, onObsidianEnabledChange: (Boolean) -> Unit,
    obsidianPath: String, onObsidianPathChange: (String) -> Unit,
    tasksEnabled: Boolean, onTasksEnabledChange: (Boolean) -> Unit,
    tasksOutputPath: String, onTasksOutputPathChange: (String) -> Unit,
    daysBackText: String, onDaysBackTextChange: (String) -> Unit,
    limitText: String, onLimitTextChange: (String) -> Unit,
    onClearCredsClick: () -> Unit,
    lang: String,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopStart) {
        Column(Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(Strings.get("settings", lang), style = MaterialTheme.typography.headlineMedium)

            Text(Strings.get("general", lang), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Language, null, modifier = Modifier.size(20.dp),
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
                            DropdownMenuItem(text = { Text(name) }, onClick = { onSelectedLangChange(code); config.language = code; langExpanded = false })
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val themeIcon = when (themeMode) {
                    "light" -> AppIcons.Sun
                    "solarized_light" -> AppIcons.SolarizedLight
                    "high_contrast" -> AppIcons.HighContrast
                    "amoled_dark" -> AppIcons.MoonAmoled
                    "sakura" -> AppIcons.Sakura
                    "solarized_dark" -> AppIcons.SolarizedDark
                    "nord" -> AppIcons.Nord
                    "dracula" -> AppIcons.Dracula
                    "catppuccin" -> AppIcons.Catppuccin
                    else -> AppIcons.MoonDark
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
                            val icon = when (value) {
                                "light" -> AppIcons.Sun
                                "solarized_light" -> AppIcons.SolarizedLight
                                "high_contrast" -> AppIcons.HighContrast
                                "amoled_dark" -> AppIcons.MoonAmoled
                                "sakura" -> AppIcons.Sakura
                                "solarized_dark" -> AppIcons.SolarizedDark
                                "nord" -> AppIcons.Nord
                                "dracula" -> AppIcons.Dracula
                                "catppuccin" -> AppIcons.Catppuccin
                                else -> AppIcons.MoonDark
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
                                onClick = { onThemeModeChange(value); config.themeMode = value; themeExpanded = false }
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(Strings.get("time_format", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                SimpleDropdown(
                    label = Strings.get("time_format", lang),
                    selected = if (use24h) Strings.get("time_24h", lang) else Strings.get("time_12h", lang),
                    options = listOf(Strings.get("time_12h", lang), Strings.get("time_24h", lang)),
                    onSelect = { v -> onUse24hChange(v == Strings.get("time_24h", lang)); config.use24h = use24h },
                    modifier = Modifier.widthIn(max = 120.dp),
                )
            }

            HorizontalDivider()
            Text(Strings.get("output", lang), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Logseq, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(Strings.get("logseq_label", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = logseqEnabled, onCheckedChange = { onLogseqEnabledChange(it); config.logseqEnabled = it })
            }
            if (logseqEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = logseqPath, onValueChange = { onLogseqPathChange(it); config.logseqPath = it },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { browseDirectory(logseqPath)?.let { onLogseqPathChange(it); config.logseqPath = it } }) {
                        Text(Strings.get("browse", lang))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Obsidian, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(Strings.get("obsidian_label", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = obsidianEnabled, onCheckedChange = { onObsidianEnabledChange(it); config.obsidianEnabled = it })
            }
            if (obsidianEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = obsidianPath, onValueChange = { onObsidianPathChange(it); config.obsidianPath = it },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { browseDirectory(obsidianPath)?.let { onObsidianPathChange(it); config.obsidianPath = it } }) {
                        Text(Strings.get("browse", lang))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(Strings.get("tasks_path", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = tasksEnabled, onCheckedChange = { onTasksEnabledChange(it); config.tasksEnabled = it })
            }
            if (tasksEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = tasksOutputPath, onValueChange = { onTasksOutputPathChange(it); config.tasksOutputPath = it },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { browseFile(tasksOutputPath)?.let { onTasksOutputPathChange(it); config.tasksOutputPath = it } }) {
                        Text(Strings.get("browse", lang))
                    }
                }
            }

            HorizontalDivider()
            Text(Strings.get("advanced", lang), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.get("fetch_days", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                OutlinedTextField(value = daysBackText, onValueChange = { onDaysBackTextChange(it); config.fetchDaysBack = it.toIntOrNull() ?: 7 },
                    singleLine = true, modifier = Modifier.widthIn(max = 100.dp))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.get("fetch_limit", lang), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                OutlinedTextField(value = limitText, onValueChange = { onLimitTextChange(it); config.fetchLimit = it.toIntOrNull() ?: 100 },
                    singleLine = true, modifier = Modifier.widthIn(max = 100.dp))
            }

            HorizontalDivider()
            TextButton(onClick = onClearCredsClick) {
                Text(Strings.get("clear_creds", lang), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun applySyncMerge(
    merged: MergeResult,
    config: DesktopConfigStore,
    onStateUpdate: (List<Event>, Map<String, Boolean>, Set<String>) -> Unit,
    persistMergedEvents: () -> Unit,
) {
    onStateUpdate(merged.manualEvents, merged.taskCompletion, merged.deletedEventIds)
    config.manualEventCache = Json.encodeToString(merged.manualEvents)
    config.taskCompletionState = Json.encodeToString(merged.taskCompletion)
    config.deletedEventIds = Json.encodeToString(merged.deletedEventIds)
    persistMergedEvents()
}

@Composable
private fun SyncDialog(
    onDismiss: () -> Unit,
    config: DesktopConfigStore,
    manualEvents: List<Event>,
    taskCompletionMap: Map<String, Boolean>,
    deletedEventIds: Set<String>,
    onStateUpdate: (List<Event>, Map<String, Boolean>, Set<String>) -> Unit,
    persistMergedEvents: () -> Unit,
    localIp: String?,
    lang: String,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var syncMergeMsg by remember { mutableStateOf<String?>(null) }
    val syncPayload = remember(manualEvents, taskCompletionMap, deletedEventIds) {
        SyncManager.generatePayload(manualEvents, taskCompletionMap, deletedEventIds, config.deviceId)
    }
    val cs = MaterialTheme.colorScheme

    val qrContent = remember(syncPayload, localIp) {
        if (localIp != null) SyncManager.encodeQrContent("http://$localIp:$SYNC_PORT", syncPayload)
        else SyncManager.serializePayload(syncPayload)
    }
    val qrImage: ImageBitmap? = remember(qrContent) {
        try {
            val writer = QRCodeWriter()
            val matrix: BitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 400, 400)
            val buffered = BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB)
            for (x in 0 until 400) for (y in 0 until 400)
                buffered.setRGB(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            buffered.toComposeImageBitmap()
        } catch (_: Exception) { null }
    }

    val applyAndMerge: (MergeResult) -> Unit = { merged ->
        applySyncMerge(merged, config, onStateUpdate, persistMergedEvents)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surface, titleContentColor = cs.onSurface, textContentColor = cs.onSurface,
        title = { Text(Strings.get("sync_qr_title", lang)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(440.dp)) {
                if (qrImage != null) {
                    Image(qrImage, contentDescription = "QR Code",
                        modifier = Modifier.size(300.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Text(Strings.get("sync_error", lang), color = cs.error)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (localIp != null) "http://$localIp:$SYNC_PORT" else Strings.get("sync_no_network", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (localIp != null) cs.onSurface.copy(alpha = 0.5f) else cs.error
                )
                if (syncMergeMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(syncMergeMsg!!, color = cs.primary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try {
                            val fc = JFileChooser()
                            fc.dialogTitle = Strings.get("sync_export", lang)
                            fc.fileFilter = FileNameExtensionFilter("JSON files", "json")
                            fc.selectedFile = File("duenest-sync.json")
                            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                var file = fc.selectedFile
                                if (!file.name.endsWith(".json")) file = File(file.absolutePath + ".json")
                                file.writeText(SyncManager.serializePayload(syncPayload))
                                onDismiss()
                            }
                        } catch (_: Exception) {}
                    }, modifier = Modifier.weight(1f)) {
                        Text(Strings.get("sync_export", lang))
                    }
                    OutlinedButton(onClick = {
                        try {
                            val fc = JFileChooser()
                            fc.dialogTitle = Strings.get("sync_import", lang)
                            fc.fileFilter = FileNameExtensionFilter("JSON files", "json")
                            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val remote = SyncManager.deserializePayload(fc.selectedFile.readText())
                                if (remote != null) {
                                    applyAndMerge(SyncManager.mergePayload(syncPayload, remote))
                                    syncMergeMsg = Strings.get("sync_merged", lang)
                                } else {
                                    syncMergeMsg = Strings.get("sync_no_data", lang)
                                }
                            }
                        } catch (_: Exception) {}
                    }, modifier = Modifier.weight(1f)) {
                        Text(Strings.get("sync_import", lang))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        try {
                            val fc = JFileChooser()
                            fc.dialogTitle = Strings.get("sync_import_qr", lang)
                            fc.fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp")
                            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val image = ImageIO.read(fc.selectedFile) ?: run {
                                    syncMergeMsg = Strings.get("sync_no_data", lang)
                                    return@OutlinedButton
                                }
                                val qrResult = QRCodeReader().decode(
                                    BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image)))
                                )
                                val (url, payload) = SyncManager.decodeQrContent(qrResult.text)
                                if (payload != null) {
                                    syncMergeMsg = Strings.get("sync_syncing", lang)
                                    if (url != null) {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val client = OkHttpClient.Builder()
                                                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                                    .build()
                                                val reqBody = SyncManager.serializePayload(syncPayload)
                                                    .toRequestBody("text/plain".toMediaType())
                                                val resp = client.newCall(
                                                    Request.Builder().url("$url/sync").post(reqBody).build()
                                                ).execute()
                                                val respBody = resp.body?.string()
                                                val remote = if (resp.isSuccessful && respBody != null)
                                                    SyncManager.deserializePayload(respBody) else null
                                                withContext(Dispatchers.Main) {
                                                    val base = SyncManager.mergePayload(syncPayload, payload)
                                                    val final = if (remote != null)
                                                        SyncManager.mergePayload(
                                                            SyncManager.generatePayload(base.manualEvents, base.taskCompletion, base.deletedEventIds, config.deviceId),
                                                            remote
                                                        ) else base
                                                    applyAndMerge(final)
                                                    syncMergeMsg = Strings.get("sync_merged", lang)
                                                }
                                            } catch (_: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    applyAndMerge(SyncManager.mergePayload(syncPayload, payload))
                                                    syncMergeMsg = Strings.get("sync_merged", lang)
                                                }
                                            }
                                        }
                                    } else {
                                        applyAndMerge(SyncManager.mergePayload(syncPayload, payload))
                                        syncMergeMsg = Strings.get("sync_merged", lang)
                                    }
                                } else {
                                    syncMergeMsg = Strings.get("sync_no_data", lang)
                                }
                            }
                        } catch (_: Exception) { syncMergeMsg = Strings.get("sync_no_data", lang) }
                    }, modifier = Modifier.weight(1f)) {
                        Text(Strings.get("sync_import_qr", lang))
                    }
                    OutlinedButton(onClick = {
                        try {
                            val fc = JFileChooser()
                            fc.dialogTitle = Strings.get("sync_export_qr", lang)
                            fc.fileFilter = FileNameExtensionFilter("PNG images", "png")
                            fc.selectedFile = File("duenest-qr.png")
                            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                var file = fc.selectedFile
                                if (!file.name.endsWith(".png")) file = File(file.absolutePath + ".png")
                                val writer = QRCodeWriter()
                                val matrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 400, 400)
                                val img = BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB)
                                for (x in 0 until 400) for (y in 0 until 400)
                                    img.setRGB(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                                ImageIO.write(img, "png", file)
                                onDismiss()
                            }
                        } catch (_: Exception) {}
                    }, modifier = Modifier.weight(1f)) {
                        Text(Strings.get("sync_export_qr", lang))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) } })
}
