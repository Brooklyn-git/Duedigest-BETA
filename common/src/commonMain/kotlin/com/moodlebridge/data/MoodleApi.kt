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

        val wsEvents = (calendarEvents + assignmentEvents).distinctBy { it.mergeKey }
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
            val shortname = course.shortname ?: ""
            for (assign in course.assignments.orEmpty()) {
                val duedate = assign.duedate ?: 0L
                if (duedate != 0L && duedate <= now) continue
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
                        course = shortname,
                        modname = "assign",
                    )
                )
            }
        }
        return result
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

private fun MoodleEvent.toEvent() = Event(
    id = id?.toString() ?: "unknown",
    name = name ?: "Untitled",
    description = MoodleApi.stripHtml(description?.text ?: ""),
    timestart = timestart ?: 0L,
    timeduration = timeduration ?: 0L,
    eventtype = eventtype ?: "",
    url = url ?: "",
    course = course?.shortname ?: "",
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
