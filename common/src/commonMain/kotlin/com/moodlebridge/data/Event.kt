package com.moodlebridge.data

import kotlinx.serialization.Serializable

@Serializable
data class MoodleEvent(
    val id: Int? = null,
    val name: String? = null,
    val description: Description? = null,
    val timestart: Long? = null,
    val timeduration: Long? = null,
    val eventtype: String? = null,
    val url: String? = null,
    val course: Course? = null,
    val modulename: String? = null,
)

@Serializable
data class Description(val text: String? = null)

@Serializable
data class Course(val shortname: String? = null)

@Serializable
data class CalendarResponse(val events: List<MoodleEvent>? = null)

@Serializable
data class AssignCourse(
    val shortname: String? = null,
    val assignments: List<Assignment>? = null,
)

@Serializable
data class Assignment(
    val name: String? = null,
    val cmid: Int? = null,
    val duedate: Long? = null,
    val intro: String? = null,
    val allosubmissionsfromdate: Long? = null,
    val url: String? = null,
)

@Serializable
data class AssignResponse(val courses: List<AssignCourse>? = null)

@Serializable
data class Quiz(
    val name: String? = null,
    val coursemodule: Int? = null,
    val intro: String? = null,
    val timeopen: Long? = null,
    val timeclose: Long? = null,
)

@Serializable
data class QuizResponse(val quizzes: List<Quiz>? = null)

@Serializable
data class Forum(
    val name: String? = null,
    val cmid: Int? = null,
    val intro: String? = null,
    val duedate: Long? = null,
    val cutoffdate: Long? = null,
)

@Serializable
data class ForumResponse(val forums: List<Forum>? = null)

@Serializable
data class Event(
    val id: String,
    val name: String,
    val description: String,
    val timestart: Long,
    val timeduration: Long,
    val eventtype: String,
    val url: String,
    val course: String,
    val modname: String,
    val source: String = "moodle",
    val availableFrom: Long? = null,
    val cutoffDate: Long? = null,
) {
    val isManual: Boolean get() = source == "manual"

    fun isLate(now: Long): Boolean =
        timestart != Long.MAX_VALUE && now >= timestart && cutoffDate != null && now < cutoffDate

    fun isOverdueStatus(now: Long): Boolean =
        timestart != Long.MAX_VALUE && now >= (cutoffDate ?: timestart)

    val mergeKey: String
        get() {
            val idParam = Regex("id=(\\d+)").find(url)?.groupValues?.get(1)
            if (idParam != null) return "activity:$idParam"
            val assignId = Regex("^assign_(\\d+)$").find(id)?.groupValues?.get(1)
            if (assignId != null) return "activity:$assignId"
            val normalized = url
                .trim()
                .substringBefore('#')
                .trimEnd('/')
                .lowercase()
            return if (normalized.isNotBlank()) normalized else "id:$id"
        }
}

fun mergeFetchedEvents(cached: List<Event>, fresh: List<Event>): List<Event> {
    val merged = linkedMapOf<String, Event>()
    val noDate = Long.MAX_VALUE
    for (e in cached) merged[e.mergeKey] = e
    for (e in fresh) {
        val existing = merged[e.mergeKey]
        val better = existing == null ||
            (e.timestart != noDate && existing.timestart == noDate) ||
            (e.url.isNotBlank() && existing.url.isBlank())
        if (better) merged[e.mergeKey] = e
    }
    return merged.values.sortedBy { it.timestart }
}
