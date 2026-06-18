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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.moodlebridge.data.ConfigStore
import com.moodlebridge.data.IcsGenerator
import com.moodlebridge.data.MoodleApi
import com.moodlebridge.data.Strings
import com.moodlebridge.worker.SyncWorker
import java.io.File
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
        val ch = NotificationChannel(SyncWorker.CHANNEL_ID, "Moodle Sync", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}

@Composable
private fun MoodleBridgeTheme(themeMode: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (when (themeMode) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }) darkColorScheme() else lightColorScheme(),
        content = { Surface(Modifier.fillMaxSize()) { content() } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var errorDialogMsg by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf(config.lastSyncMessage) }
    val logLines = remember { mutableStateListOf<String>() }
    var tzExpanded by remember { mutableStateOf(false) }
    var widgetOpacity by remember { mutableFloatStateOf(config.widgetOpacity) }

    fun addLog(msg: String) { logLines.add(msg) }

    fun doSync(pw: String) {
        isWorking = true; logLines.clear(); statusText = ""
        scope.launch {
            doFetch(context, config, url, username, pw, tz, savePw,
                icsEnabled, logseqEnabled, obsidianEnabled, icsPath, logseqPath, obsidianPath,
                daysBackText, limitText, lang, { addLog(it) }, { statusText = it }, { errorDialogMsg = it },
                { isWorking = false })
        }
    }

    MoodleBridgeTheme(themeMode = themeMode) {
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
                // ── Tabs (bigger touch targets) ─────────────────
                TabRow(selectedTabIndex = selectedTab) {
                    listOf(Strings.get("connection", lang), Strings.get("output", lang)).forEachIndexed { i, t ->
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
                        1 -> OutputTabContent(
                            icsEnabled = icsEnabled, onIcsEnabledChange = { icsEnabled = it },
                            logseqEnabled = logseqEnabled, onLogseqEnabledChange = { logseqEnabled = it },
                            obsidianEnabled = obsidianEnabled, onObsidianEnabledChange = { obsidianEnabled = it },
                            icsPath = icsPath, onIcsPathChange = { icsPath = it },
                            logseqPath = logseqPath, onLogseqPathChange = { logseqPath = it },
                            obsidianPath = obsidianPath, onObsidianPathChange = { obsidianPath = it },
                            daysBackText = daysBackText, onDaysBackChange = { daysBackText = it },
                            limitText = limitText, onLimitChange = { limitText = it }, lang = lang)
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
                Text(Strings.get("log", lang), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                Card(Modifier.fillMaxWidth().weight(2f).padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                        items(logLines.toList()) { line -> Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                    }
                }

                // ── Status ──────────────────────────────────────
                Text(statusText.ifBlank { Strings.get("ready", lang) }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

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
                        Text("Widget opacity", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("%.0f".format(widgetOpacity * 100), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
                            Slider(value = widgetOpacity, onValueChange = { widgetOpacity = it; config.widgetOpacity = it }, valueRange = 0.1f..1.0f, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
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

private suspend fun doFetch(
    context: Context, config: ConfigStore,
    url: String, username: String, password: String, tz: String, savePw: Boolean,
    icsEnabled: Boolean, logseqEnabled: Boolean, obsidianEnabled: Boolean,
    icsPath: String, logseqPath: String, obsidianPath: String,
    daysBackText: String, limitText: String, lang: String,
    onLog: (String) -> Unit, onStatus: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit,
) {
    try {
        config.moodleUrl = url.trimEnd('/'); config.username = username.trim(); config.timezone = tz.trim()
        config.savePassword = savePw; if (savePw) config.password = password else config.password = ""
        config.icsEnabled = icsEnabled; config.logseqEnabled = logseqEnabled; config.obsidianEnabled = obsidianEnabled
        config.icsPath = icsPath; config.logseqPath = logseqPath; config.obsidianPath = obsidianPath
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
            withContext(Dispatchers.IO) { File(context.cacheDir, "ics").also { it.mkdirs() }; File(File(context.cacheDir, "ics"), "calendar.ics").writeText(IcsGenerator.generate(events)) }
            onLog("ICS -> ${context.cacheDir}/ics/calendar.ics")
        }

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
    daysBackText: String, onDaysBackChange: (String) -> Unit, limitText: String, onLimitChange: (String) -> Unit, lang: String,
) {
    val icsPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri -> uri?.let { onIcsPathChange(it.toString()) } }
    val logseqPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onLogseqPathChange(uri.toString()) } }
    val obsidianPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onObsidianPathChange(uri.toString()) } }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.get("formats", lang), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = icsEnabled, onCheckedChange = onIcsEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("ics_label", lang)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = logseqEnabled, onCheckedChange = onLogseqEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("logseq_label", lang)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = obsidianEnabled, onCheckedChange = onObsidianEnabledChange); Spacer(Modifier.width(4.dp)); Text(Strings.get("obsidian_label", lang)) }
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
