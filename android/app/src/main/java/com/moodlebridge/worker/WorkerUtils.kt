package com.moodlebridge.worker

import com.moodlebridge.data.ConfigStore
import java.util.Calendar

internal fun parseIntList(value: String): List<Int> =
    value.split(",").mapNotNull {
        val t = it.trim()
        if (t.contains(":")) t.substringBefore(":").toIntOrNull() else t.toIntOrNull()
    }

internal fun parseTimeList(value: String): List<Pair<Int, Int>> =
    value.split(",").mapNotNull {
        val t = it.trim()
        val parts = t.split(":")
        val h = parts[0].toIntOrNull() ?: return@mapNotNull null
        val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
        h to m
    }

internal fun calculateDelay(
    scheduleType: String,
    customHours: String,
    customDays: String,
): Long {
    val now = Calendar.getInstance()
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val currentDay = now.get(Calendar.DAY_OF_WEEK) - 1

    return when (scheduleType) {
        "daily" -> 24 * 60L
        "weekly" -> 7 * 24 * 60L
        "custom" -> {
            val times = parseTimeList(customHours)
            val days = parseIntList(customDays).toSet()
            val allDays = days.isEmpty()
            if (times.isEmpty()) return 60L

            val sortedTimes = times.map { (h, m) -> h * 60 + m }.sorted()

            for (t in sortedTimes) {
                if (t > currentMinutes) return (t - currentMinutes).toLong()
            }

            for (offset in 1..7) {
                val d = (currentDay + offset) % 7
                if (allDays || d in days) {
                    val t = sortedTimes.first()
                    return (offset * 24 * 60L + t - currentMinutes).coerceAtLeast(1)
                }
            }
            7 * 24 * 60L
        }
        else -> 24 * 60L
    }
}
