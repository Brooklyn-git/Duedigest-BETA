package com.moodlebridge.data

import android.content.Context
import android.content.SharedPreferences

class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("moodle_bridge", Context.MODE_PRIVATE)

    var moodleUrl: String
        get() = prefs.getString(KEY_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var timezone: String
        get() = prefs.getString(KEY_TZ, "UTC") ?: "UTC"
        set(value) = prefs.edit().putString(KEY_TZ, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var lastSyncMessage: String
        get() = prefs.getString(KEY_SYNC_MSG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_MSG, value).apply()

    var eventCount: Int
        get() = prefs.getInt(KEY_EVENT_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_EVENT_COUNT, value).apply()

    val isConfigured: Boolean
        get() = moodleUrl.isNotBlank() && token.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_URL = "moodle_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_TZ = "timezone"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_SYNC_MSG = "sync_msg"
        private const val KEY_EVENT_COUNT = "event_count"
    }
}
