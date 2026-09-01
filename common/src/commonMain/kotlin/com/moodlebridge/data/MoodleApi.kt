package com.moodlebridge.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class MoodleApi(
    private val moodleUrl: String,
    private val token: String,
    private val scrapeEnabled: Boolean = false,
    private val username: String = "",
    private val password: String = "",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun fetchEvents(daysBack: Int = 7, limit: Int = 100): List<Event> {
        val calendarEvents = try {
            fetchCalendarEvents(daysBack, limit).toMutableList()
        } catch (e: Exception) {
            println("DueNest API: fetchCalendarEvents failed: ${e.message}")
            mutableListOf()
        }
        println("DueNest API: fetchCalendarEvents returned ${calendarEvents.size} events")
        val assignmentEvents = try {
            fetchAssignments()
        } catch (e: Exception) {
            println("DueNest API: fetchAssignments failed: ${e.message}")
            emptyList()
        }
        println("DueNest API: fetchAssignments returned ${assignmentEvents.size} events")
        val courseMap = try {
            fetchEnrolledCourses()
        } catch (e: Exception) {
            println("DueNest API: fetchEnrolledCourses failed: ${e.message}")
            emptyMap()
        }
        println("DueNest API: Enrolled courses = ${courseMap.size}")

        val quizEvents = try {
            fetchQuizzes(courseMap)
        } catch (e: Exception) {
            println("DueNest API: fetchQuizzes failed: ${e.message}")
            emptyList()
        }
        println("DueNest API: fetchQuizzes returned ${quizEvents.size} events")
        val forumEvents = try {
            fetchForums(courseMap)
        } catch (e: Exception) {
            println("DueNest API: fetchForums failed: ${e.message}")
            emptyList()
        }
        println("DueNest API: fetchForums returned ${forumEvents.size} events")

        val dedicated = (assignmentEvents + quizEvents + forumEvents).toMutableList()
        val knownKeys = dedicated.map { it.mergeKey }.toSet()
        val wsEvents = (dedicated + calendarEvents.filter { it.mergeKey !in knownKeys }).distinctBy { it.mergeKey }
            .map { event ->
                if (event.course.isNotBlank()) event
                else {
                    val course = calendarEvents.firstOrNull { it.mergeKey == event.mergeKey }?.course.orEmpty()
                    if (course.isBlank()) event else event.copy(course = course)
                }
            }
        println("DueNest API: Combined WS events = ${wsEvents.size}, scrapeEnabled=$scrapeEnabled")

        if (scrapeEnabled && username.isNotBlank() && password.isNotBlank()) {
            println("DueNest API: scrape enabled -> always calling scraper")
            return try {
                val scraped = MoodleWebScraper.scrape(moodleUrl, username, password)
                println("DueNest API: Scraper returned ${scraped.size} events")
                wsEvents.mergeByDedup(scraped)
            } catch (e: Exception) {
                println("DueNest API: Scraper FAILED: ${e.message}, falling back to WS events")
                wsEvents.sortedBy { it.timestart }
            }
        }

        return wsEvents.sortedBy { it.timestart }
    }

    private fun fetchCalendarEvents(daysBack: Int, limit: Int): List<Event> {
        val timesort = (System.currentTimeMillis() / 1000) - (daysBack * 86400L)
        val params = mapOf(
            "wstoken" to token,
            "moodlewsrestformat" to "json",
            "wsfunction" to "core_calendar_get_action_events_by_timesort",
            "timesortfrom" to timesort.toString(),
            "limitnum" to limit.toString(),
        )
        val url = "$moodleUrl/webservice/rest/server.php?${params.toQueryString()}"
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")
        val data = json.decodeFromString<CalendarResponse>(body)
        return data.events?.map { it.toEvent() } ?: emptyList()
    }

    private fun fetchAssignments(): List<Event> {
        val now = System.currentTimeMillis() / 1000
        val params = mapOf(
            "wstoken" to token,
            "moodlewsrestformat" to "json",
            "wsfunction" to "mod_assign_get_assignments",
        )
        val url = "$moodleUrl/webservice/rest/server.php?${params.toQueryString()}"
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")
        val data = json.decodeFromString<AssignResponse>(body)
        val result = mutableListOf<Event>()
        for (course in data.courses.orEmpty()) {
            val courseName = course.displayName
            for (assign in course.assignments.orEmpty()) {
                val duedate = assign.duedate ?: 0L
                val availableFrom = assign.allosubmissionsfromdate?.takeIf { it > 0L }
                if (availableFrom == null && duedate != 0L && duedate <= now) continue
                result.add(
                    Event(
                        id = "assign_${assign.cmid}",
                        name = assign.name ?: "Untitled",
                        description = stripHtml(assign.intro ?: ""),
                        timestart = if (duedate > 0L) duedate else Long.MAX_VALUE,
                        timeduration = 0,
                        eventtype = "assign",
                        url = assign.url?.takeIf { it.isNotBlank() }
                            ?: "${moodleUrl.trimEnd('/')}/mod/assign/view.php?id=${assign.cmid}",
                        course = courseName,
                        modname = "assign",
                        availableFrom = availableFrom?.takeIf { it != duedate },
                    )
                )
            }
        }
        return result
    }

    private fun fetchQuizzes(courseMap: Map<Int, String>): List<Event> {
        val now = System.currentTimeMillis() / 1000
        val params = mapOf(
            "wstoken" to token,
            "moodlewsrestformat" to "json",
            "wsfunction" to "mod_quiz_get_quizzes_by_courses",
        )
        val url = "$moodleUrl/webservice/rest/server.php?${params.toQueryString()}"
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")
        val data = json.decodeFromString<QuizResponse>(body)
        val result = mutableListOf<Event>()
        for (quiz in data.quizzes.orEmpty()) {
            val cmid = quiz.coursemodule ?: continue
            val open = quiz.timeopen ?: 0L
            val close = quiz.timeclose ?: 0L
            val (timestart, availableFrom) = when {
                close > 0L -> close to (open.takeIf { it > 0L && it != close })
                open > 0L -> open to null
                else -> Long.MAX_VALUE to null
            }
            if (close > 0L && close <= now) continue
            result.add(
                Event(
                    id = "quiz_$cmid",
                    name = quiz.name ?: "Untitled",
                    description = stripHtml(quiz.intro ?: ""),
                    timestart = timestart,
                    timeduration = 0,
                    eventtype = "quiz",
                    url = "${moodleUrl.trimEnd('/')}/mod/quiz/view.php?id=$cmid",
                    course = courseMap[quiz.course] ?: "",
                    modname = "quiz",
                    availableFrom = availableFrom,
                )
            )
        }
        return result
    }

    private fun fetchForums(courseMap: Map<Int, String>): List<Event> {
        val now = System.currentTimeMillis() / 1000
        val params = mapOf(
            "wstoken" to token,
            "moodlewsrestformat" to "json",
            "wsfunction" to "mod_forum_get_forums_by_courses",
        )
        val url = "$moodleUrl/webservice/rest/server.php?${params.toQueryString()}"
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")
        val data = json.decodeFromString<ForumResponse>(body)
        val result = mutableListOf<Event>()
        for (forum in data.forums.orEmpty()) {
            val cmid = forum.cmid ?: continue
            val due = forum.duedate ?: 0L
            val cutoff = forum.cutoffdate ?: 0L
            if (cutoff <= 0L && due <= 0L) {
                result.add(
                    Event(
                        id = "forum_$cmid",
                        name = forum.name ?: "Untitled",
                        description = stripHtml(forum.intro ?: ""),
                        timestart = Long.MAX_VALUE,
                        timeduration = 0,
                        eventtype = "forum",
                        url = "${moodleUrl.trimEnd('/')}/mod/forum/view.php?id=$cmid",
                        course = courseMap[forum.course] ?: "",
                        modname = "forum",
                    )
                )
                continue
            }
            if (cutoff > 0L && cutoff <= now) continue
            if (due <= 0L) {
                result.add(
                    Event(
                        id = "forum_$cmid",
                        name = forum.name ?: "Untitled",
                        description = stripHtml(forum.intro ?: ""),
                        timestart = cutoff,
                        timeduration = 0,
                        eventtype = "forum",
                        url = "${moodleUrl.trimEnd('/')}/mod/forum/view.php?id=$cmid",
                        course = courseMap[forum.course] ?: "",
                        modname = "forum",
                    )
                )
            } else {
                result.add(
                    Event(
                        id = "forum_$cmid",
                        name = forum.name ?: "Untitled",
                        description = stripHtml(forum.intro ?: ""),
                        timestart = due,
                        timeduration = 0,
                        eventtype = "forum",
                        url = "${moodleUrl.trimEnd('/')}/mod/forum/view.php?id=$cmid",
                        course = courseMap[forum.course] ?: "",
                        modname = "forum",
                        cutoffDate = cutoff.takeIf { it > 0L },
                    )
                )
            }
        }
        return result
    }

    private fun fetchEnrolledCourses(): Map<Int, String> {
        val userid = fetchCurrentUserId()
        val params = mapOf(
            "wstoken" to token,
            "moodlewsrestformat" to "json",
            "wsfunction" to "core_enrol_get_users_courses",
            "userid" to userid.toString(),
        )
        val url = "$moodleUrl/webservice/rest/server.php?${params.toQueryString()}"
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")
        val data = json.decodeFromString<List<EnrolledCourse>>(body)
        return data.mapNotNull { course ->
            val id = course.id ?: return@mapNotNull null
            val name = course.displayName.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            id to name
        }.toMap()
    }

    private fun fetchCurrentUserId(): Int {
        val params = mapOf(
            "wstoken" to token,
            "moodlewsrestformat" to "json",
            "wsfunction" to "core_webservice_get_site_info",
        )
        val url = "$moodleUrl/webservice/rest/server.php?${params.toQueryString()}"
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")
        val data = json.decodeFromString<SiteInfo>(body)
        return data.userid ?: throw IOException("site info missing userid")
    }

    companion object {
        fun login(baseUrl: String, username: String, password: String): String {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val url = "${baseUrl.trimEnd('/')}/login/token.php"
            val body = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("service", "moodle_mobile_app")
                .build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty login response")
            val json = Json { ignoreUnknownKeys = true }
            val result = json.decodeFromString<LoginResponse>(responseBody)
            if (result.token != null) return result.token
            throw IOException(result.error ?: "Login failed")
        }

        private fun Map<String, String>.toQueryString(): String =
            entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }

        fun stripHtml(text: String): String =
            text
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .trim()
    }
}

@Serializable
private data class LoginResponse(
    val token: String? = null,
    val error: String? = null,
)

@Serializable
private data class EnrolledCourse(
    val id: Int? = null,
    val shortname: String? = null,
    val fullname: String? = null,
    val displayname: String? = null,
) {
    val displayName: String
        get() = displayname?.takeIf { it.isNotBlank() }
            ?: fullname?.takeIf { it.isNotBlank() }
            ?: shortname.orEmpty()
}

@Serializable
private data class SiteInfo(
    val userid: Int? = null,
)

private fun MoodleEvent.toEvent() = Event(
    id = id?.toString() ?: "unknown",
    name = name ?: "Untitled",
    description = MoodleApi.stripHtml(description?.text ?: ""),
    timestart = timestart ?: 0L,
    timeduration = timeduration ?: 0L,
    eventtype = eventtype ?: "",
    url = url ?: "",
    course = course?.displayName,
    modname = modulename ?: "",
)

private const val NO_DUE_DATE = Long.MAX_VALUE

private fun List<Event>.mergeByDedup(preferred: List<Event>): List<Event> {
    val merged = linkedMapOf<String, Event>()
    for (event in preferred) merged[event.mergeKey] = event
    for (event in this) {
        val existing = merged[event.mergeKey]
        val takesNew = existing == null ||
            (event.timestart != NO_DUE_DATE && existing.timestart == NO_DUE_DATE)
        if (takesNew) merged[event.mergeKey] = event
    }
    return merged.values.sortedBy { it.timestart }
}
