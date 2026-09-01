package com.moodlebridge.data

import java.io.File
import java.util.prefs.Preferences

class DesktopConfigStore {
    private val prefs = Preferences.userNodeForPackage(DesktopConfigStore::class.java)

    private val configDir = File(System.getProperty("user.home"), ".moodlebridge").also { it.mkdirs() }

    var moodleUrl: String
        get() = prefs.get(KEY_URL, "")
        set(value) = prefs.put(KEY_URL, value)

    var token: String
        get() = prefs.get(KEY_TOKEN, "")
        set(value) = prefs.put(KEY_TOKEN, value)

    var username: String
        get() = prefs.get(KEY_USERNAME, "")
        set(value) = prefs.put(KEY_USERNAME, value)

    var password: String
        get() = prefs.get(KEY_PASSWORD, "")
        set(value) = prefs.put(KEY_PASSWORD, value)

    var savePassword: Boolean
        get() = prefs.getBoolean(KEY_SAVE_PW, false)
        set(value) = prefs.putBoolean(KEY_SAVE_PW, value)

    var logseqEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOGSEQ, false)
        set(value) = prefs.putBoolean(KEY_LOGSEQ, value)

    var obsidianEnabled: Boolean
        get() = prefs.getBoolean(KEY_OBSIDIAN, false)
        set(value) = prefs.putBoolean(KEY_OBSIDIAN, value)

    var logseqPath: String
        get() = prefs.get(KEY_LOGSEQ_PATH, "logseq/")
        set(value) = prefs.put(KEY_LOGSEQ_PATH, value)

    var obsidianPath: String
        get() = prefs.get(KEY_OBSIDIAN_PATH, "obsidian/")
        set(value) = prefs.put(KEY_OBSIDIAN_PATH, value)

    var fetchDaysBack: Int
        get() = prefs.getInt(KEY_DAYS_BACK, 7)
        set(value) = prefs.putInt(KEY_DAYS_BACK, value)

    var fetchLimit: Int
        get() = prefs.getInt(KEY_FETCH_LIMIT, 100)
        set(value) = prefs.putInt(KEY_FETCH_LIMIT, value)

    var language: String
        get() = prefs.get(KEY_LANG, "en").takeIf { it.isNotBlank() } ?: "en"
        set(value) = prefs.put(KEY_LANG, value)

    var themeMode: String
        get() = prefs.get(KEY_THEME, "system") ?: "system"
        set(value) = prefs.put(KEY_THEME, value)

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.putLong(KEY_LAST_SYNC, value)

    var lastSyncMessage: String
        get() = prefs.get(KEY_SYNC_MSG, "")
        set(value) = prefs.put(KEY_SYNC_MSG, value)

    var eventCount: Int
        get() = prefs.getInt(KEY_EVENT_COUNT, 0)
        set(value) = prefs.putInt(KEY_EVENT_COUNT, value)

    var use24h: Boolean
        get() = prefs.getBoolean(KEY_USE24H, true)
        set(value) = prefs.putBoolean(KEY_USE24H, value)

    var tasksEnabled: Boolean
        get() = prefs.getBoolean(KEY_TASKS_ENABLED, true)
        set(value) = prefs.putBoolean(KEY_TASKS_ENABLED, value)

    var tasksOutputPath: String
        get() = prefs.get(KEY_TASKS_PATH, "")
        set(value) = prefs.put(KEY_TASKS_PATH, value)

    var taskCompletionState: String
        get() = readFile(KEY_TASK_COMPLETION, "{}")
        set(value) = writeFile(KEY_TASK_COMPLETION, value)

    var taskEventCache: String
        get() = readFile(KEY_TASK_EVENT_CACHE, "[]")
        set(value) = writeFile(KEY_TASK_EVENT_CACHE, value)

    var manualEventCache: String
        get() = readFile(KEY_MANUAL_EVENT_CACHE, "[]")
        set(value) = writeFile(KEY_MANUAL_EVENT_CACHE, value)

    var deletedEventIds: String
        get() = readFile(KEY_DELETED_EVENT_IDS, "[]")
        set(value) = writeFile(KEY_DELETED_EVENT_IDS, value)

    var deviceId: String
        get() {
            val existing = prefs.get(KEY_DEVICE_ID, "")
            if (existing.isNotBlank()) return existing
            val newId = java.util.UUID.randomUUID().toString()
            prefs.put(KEY_DEVICE_ID, newId)
            return newId
        }
        set(value) = prefs.put(KEY_DEVICE_ID, value)

    var lastSyncUrl: String
        get() = prefs.get(KEY_LAST_SYNC_URL, "")
        set(value) = prefs.put(KEY_LAST_SYNC_URL, value)

    var syncIntervalSeconds: Int
        get() = prefs.getInt(KEY_SYNC_INTERVAL, 30)
        set(value) = prefs.putInt(KEY_SYNC_INTERVAL, value)

    var lastNetworkSignature: String
        get() = prefs.get(KEY_LAST_NETWORK_SIG, "")
        set(value) = prefs.put(KEY_LAST_NETWORK_SIG, value)

    var scrapeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCRAPE_ENABLED, false)
        set(value) = prefs.putBoolean(KEY_SCRAPE_ENABLED, value)

    var skipFinishedTasks: Boolean
        get() = prefs.getBoolean(KEY_SKIP_FINISHED, true)
        set(value) = prefs.putBoolean(KEY_SKIP_FINISHED, value)

    val isConfigured: Boolean
        get() = moodleUrl.isNotBlank() && (token.isNotBlank() || (username.isNotBlank() && password.isNotBlank()))

    private fun readFile(key: String, default: String): String {
        val file = File(configDir, key)
        return if (file.exists()) {
            try { file.readText() } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to read $key", e); default }
        } else {
            val legacy = prefs.get(key, default)
            if (legacy != default) writeFile(key, legacy)
            legacy
        }
    }

    private fun writeFile(key: String, value: String) {
        try { File(configDir, key).writeText(value) } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to write $key", e) }
    }

    fun clear() {
        try { prefs.clear() } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to clear preferences", e) }
        listOf(KEY_TASK_COMPLETION, KEY_TASK_EVENT_CACHE, KEY_MANUAL_EVENT_CACHE, KEY_DELETED_EVENT_IDS).forEach {
            try { File(configDir, it).delete() } catch (e: Exception) { com.moodlebridge.Log.w("Config", "Failed to delete $it", e) }
        }
    }

    private companion object {
        const val KEY_URL = "moodle_url"
        const val KEY_TOKEN = "token"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_SAVE_PW = "save_password"
        const val KEY_LOGSEQ = "logseq_enabled"
        const val KEY_OBSIDIAN = "obsidian_enabled"
        const val KEY_LOGSEQ_PATH = "logseq_path"
        const val KEY_OBSIDIAN_PATH = "obsidian_path"
        const val KEY_DAYS_BACK = "fetch_days_back"
        const val KEY_FETCH_LIMIT = "fetch_limit"
        const val KEY_LANG = "language"
        const val KEY_THEME = "theme_mode"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_SYNC_MSG = "sync_msg"
        const val KEY_EVENT_COUNT = "event_count"
        const val KEY_USE24H = "use_24h"
        const val KEY_TASKS_ENABLED = "tasks_enabled"
        const val KEY_TASKS_PATH = "tasks_path"
        const val KEY_TASK_COMPLETION = "task_completion"
        const val KEY_TASK_EVENT_CACHE = "task_event_cache"
        const val KEY_MANUAL_EVENT_CACHE = "manual_event_cache"
        const val KEY_DELETED_EVENT_IDS = "deleted_event_ids"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LAST_SYNC_URL = "last_sync_url"
        const val KEY_SYNC_INTERVAL = "sync_interval_seconds"
        const val KEY_LAST_NETWORK_SIG = "last_network_signature"
        const val KEY_SCRAPE_ENABLED = "scrape_enabled"
        const val KEY_SKIP_FINISHED = "skip_finished_tasks"
    }
}
