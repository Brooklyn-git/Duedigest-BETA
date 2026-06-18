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
    val url: String? = null,
)

@Serializable
data class AssignResponse(val courses: List<AssignCourse>? = null)

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
)
