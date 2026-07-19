package com.moodlebridge.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        EncryptedSharedPreferences.create(
            "moodle_bridge",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    var moodleUrl: String
        get() = prefs.getString(KEY_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var savePassword: Boolean
        get() = prefs.getBoolean(KEY_SAVE_PW, false)
        set(value) = prefs.edit().putBoolean(KEY_SAVE_PW, value).apply()

    var timezone: String
        get() = prefs.getString(KEY_TZ, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TZ, value).apply()

    var icsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ICS, true)
        set(value) = prefs.edit().putBoolean(KEY_ICS, value).apply()

    var logseqEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOGSEQ, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGSEQ, value).apply()

    var obsidianEnabled: Boolean
        get() = prefs.getBoolean(KEY_OBSIDIAN, false)
        set(value) = prefs.edit().putBoolean(KEY_OBSIDIAN, value).apply()

    var icsPath: String
        get() = prefs.getString(KEY_ICS_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ICS_PATH, value).apply()

    var logseqPath: String
        get() = prefs.getString(KEY_LOGSEQ_PATH, "logseq/") ?: "logseq/"
        set(value) = prefs.edit().putString(KEY_LOGSEQ_PATH, value).apply()

    var obsidianPath: String
        get() = prefs.getString(KEY_OBSIDIAN_PATH, "obsidian/") ?: "obsidian/"
        set(value) = prefs.edit().putString(KEY_OBSIDIAN_PATH, value).apply()

    var fetchDaysBack: Int
        get() = prefs.getInt(KEY_DAYS_BACK, 7)
        set(value) = prefs.edit().putInt(KEY_DAYS_BACK, value).apply()

    var fetchLimit: Int
        get() = prefs.getInt(KEY_FETCH_LIMIT, 100)
        set(value) = prefs.edit().putInt(KEY_FETCH_LIMIT, value).apply()

    var language: String
        get() = (prefs.getString(KEY_LANG, "en") ?: "").takeIf { it.isNotBlank() } ?: "en"
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var lastSyncMessage: String
        get() = prefs.getString(KEY_SYNC_MSG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_MSG, value).apply()

    var eventCount: Int
        get() = prefs.getInt(KEY_EVENT_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_EVENT_COUNT, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply()

    var notificationScheduleType: String
        get() = prefs.getString(KEY_NOTIF_SCHEDULE, "daily") ?: "daily"
        set(value) = prefs.edit().putString(KEY_NOTIF_SCHEDULE, value).apply()

    var notificationCustomDays: String
        get() = prefs.getString(KEY_NOTIF_CUSTOM_DAYS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NOTIF_CUSTOM_DAYS, value).apply()

    var notificationCustomHours: String
        get() = prefs.getString(KEY_NOTIF_CUSTOM_HOURS, "9:00") ?: "9:00"
        set(value) = prefs.edit().putString(KEY_NOTIF_CUSTOM_HOURS, value).apply()

    var notification24hFormat: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_24H, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_24H, value).apply()

    var taskRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_TASK_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TASK_REMINDER_ENABLED, value).apply()

    var taskReminderScheduleType: String
        get() = prefs.getString(KEY_TASK_REMINDER_SCHEDULE, "daily") ?: "daily"
        set(value) = prefs.edit().putString(KEY_TASK_REMINDER_SCHEDULE, value).apply()

    var taskReminderCustomDays: String
        get() = prefs.getString(KEY_TASK_REMINDER_CUSTOM_DAYS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TASK_REMINDER_CUSTOM_DAYS, value).apply()

    var taskReminderCustomHours: String
        get() = prefs.getString(KEY_TASK_REMINDER_CUSTOM_HOURS, "9:00") ?: "9:00"
        set(value) = prefs.edit().putString(KEY_TASK_REMINDER_CUSTOM_HOURS, value).apply()

    var widgetOpacity: Float
        get() = prefs.getFloat(KEY_WIDGET_OPACITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_WIDGET_OPACITY, value).apply()

    fun getWidgetOpacity(widgetId: String): Float =
        prefs.getFloat("${KEY_WIDGET_OPACITY}_$widgetId", lastConfiguredOpacity)

    fun setWidgetOpacity(widgetId: String, value: Float) {
        prefs.edit().putFloat("${KEY_WIDGET_OPACITY}_$widgetId", value).commit()
    }

    var tasksEnabled: Boolean
        get() = prefs.getBoolean(KEY_TASKS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TASKS_ENABLED, value).apply()

    var tasksOutputPath: String
        get() = prefs.getString(KEY_TASKS_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TASKS_PATH, value).apply()

    var taskCompletionState: String
        get() = prefs.getString(KEY_TASK_COMPLETION, "{}") ?: "{}"
        set(value) = prefs.edit().putString(KEY_TASK_COMPLETION, value).apply()

    var taskEventCache: String
        get() = prefs.getString(KEY_TASK_EVENT_CACHE, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_TASK_EVENT_CACHE, value).apply()

    var manualEventCache: String
        get() = prefs.getString(KEY_MANUAL_EVENT_CACHE, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_MANUAL_EVENT_CACHE, value).apply()

    var deletedEventIds: String
        get() = prefs.getString(KEY_DELETED_EVENT_IDS, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_DELETED_EVENT_IDS, value).apply()

    var deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, "") ?: ""
            if (existing.isNotBlank()) return existing
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            return newId
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var lastConfiguredOpacity: Float
        get() = prefs.getFloat(KEY_LAST_OPACITY, 1.0f)
        set(value) { prefs.edit().putFloat(KEY_LAST_OPACITY, value).commit() }

    val isConfigured: Boolean
        get() = moodleUrl.isNotBlank() && (token.isNotBlank() || (username.isNotBlank() && password.isNotBlank()))

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_URL = "moodle_url"
        const val KEY_TOKEN = "token"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_SAVE_PW = "save_password"
        const val KEY_TZ = "timezone"
        const val KEY_ICS = "ics_enabled"
        const val KEY_LOGSEQ = "logseq_enabled"
        const val KEY_OBSIDIAN = "obsidian_enabled"
        const val KEY_ICS_PATH = "ics_path"
        const val KEY_LOGSEQ_PATH = "logseq_path"
        const val KEY_OBSIDIAN_PATH = "obsidian_path"
        const val KEY_DAYS_BACK = "fetch_days_back"
        const val KEY_FETCH_LIMIT = "fetch_limit"
        const val KEY_LANG = "language"
        const val KEY_THEME = "theme_mode"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_SYNC_MSG = "sync_msg"
        const val KEY_EVENT_COUNT = "event_count"
        const val KEY_WIDGET_OPACITY = "widget_opacity"
        const val KEY_LAST_OPACITY = "last_configured_opacity"
        const val KEY_NOTIF_ENABLED = "notifications_enabled"
        const val KEY_NOTIF_SCHEDULE = "notif_schedule_type"
        const val KEY_NOTIF_CUSTOM_DAYS = "notif_custom_days"
        const val KEY_NOTIF_CUSTOM_HOURS = "notif_custom_hours"
        const val KEY_NOTIF_24H = "notif_24h_format"
        const val KEY_TASK_REMINDER_ENABLED = "task_reminder_enabled"
        const val KEY_TASK_REMINDER_SCHEDULE = "task_reminder_schedule_type"
        const val KEY_TASK_REMINDER_CUSTOM_DAYS = "task_reminder_custom_days"
        const val KEY_TASK_REMINDER_CUSTOM_HOURS = "task_reminder_custom_hours"
        const val KEY_TASKS_ENABLED = "tasks_enabled"
        const val KEY_TASKS_PATH = "tasks_path"
        const val KEY_TASK_COMPLETION = "task_completion"
        const val KEY_TASK_EVENT_CACHE = "task_event_cache"
        const val KEY_MANUAL_EVENT_CACHE = "manual_event_cache"
        const val KEY_DELETED_EVENT_IDS = "deleted_event_ids"
        const val KEY_DEVICE_ID = "device_id"
    }
}
