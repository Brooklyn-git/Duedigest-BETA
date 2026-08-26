package com.moodlebridge.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object MoodleWebScraper {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .cookieJar(object : CookieJar {
                private val store = mutableMapOf<String, MutableList<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val key = url.host
                    store.getOrPut(key) { mutableListOf() }.let { existing ->
                        for (cookie in cookies) {
                            existing.removeAll { it.name == cookie.name && it.path == cookie.path }
                            existing.add(cookie)
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> =
                    store[url.host].orEmpty()
            })
            .build()
    }

    fun scrape(moodleUrl: String, username: String, password: String): List<Event> {
        val base = moodleUrl.trimEnd('/')
        login(base, username, password)
        val courses = collectCourses(base)
        val events = mutableListOf<Event>()
        for (course in courses) {
            events.addAll(scrapeAssignments(base, course))
        }
        return events.sortedBy { it.timestart }
    }

    private fun login(base: String, username: String, password: String) {
        val url = "$base/login/index.php"
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()
        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw IOException("Empty login response")
        if (response.code >= 400) throw IOException("Login failed (HTTP ${response.code})")
        val doc = Jsoup.parse(html)
        if (doc.select("input[name=\"username\"]").isNotEmpty() &&
            doc.select("#loginerrormessagewrap, .loginerror, #login").isNotEmpty()
        ) {
            throw IOException("Login failed — check your credentials")
        }
    }

    private fun collectCourses(base: String): List<CourseInfo> {
        val url = "$base/my/"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw IOException("Empty dashboard response")
        val doc = Jsoup.parse(html)
        val courses = mutableListOf<CourseInfo>()
        val seen = mutableSetOf<String>()
        for (link in doc.select("a[href*=\"/course/view.php?id=\"]")) {
            val href = link.attr("abs:href").ifEmpty { link.attr("href") }
            val idMatch = Regex("id=(\\d+)").find(href) ?: continue
            val id = idMatch.groupValues[1]
            if (id in seen) continue
            seen.add(id)
            val name = link.text().trim().replace(Regex("\\s+"), " ")
            if (name.isNotBlank()) {
                courses.add(CourseInfo(id, name))
            }
        }
        return courses
    }

    private fun scrapeAssignments(base: String, course: CourseInfo): List<Event> {
        val url = "$base/mod/assign/index.php?id=${course.id}"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return emptyList()
        val doc = Jsoup.parse(html)
        val rows = doc.select("table.generaltable tbody tr")
        val now = System.currentTimeMillis() / 1000
        val result = mutableListOf<Event>()
        for (row in rows) {
            val link = row.select("a[href*=\"/mod/assign/view.php?id=\"]").firstOrNull() ?: continue
            val href = link.attr("abs:href").ifEmpty { link.attr("href") }
            val title = link.text().trim()
            if (title.isBlank()) continue
            val cells = row.select("td, th")
            val dueDateStr = cells.getOrNull(2)?.text()?.trim().orEmpty()
            val submissionStatus = cells.getOrNull(3)?.text()?.trim().orEmpty()
            if (isSubmitted(submissionStatus)) continue
            val duedate = parseDate(dueDateStr)
            if (duedate != null && duedate <= now) continue
            result.add(
                Event(
                    id = "scrape_${course.id}_${href.hashCode().toUInt()}",
                    name = title,
                    description = "",
                    timestart = duedate ?: 0L,
                    timeduration = 0,
                    eventtype = "assign",
                    url = href,
                    course = course.name,
                    modname = "assign",
                )
            )
        }
        return result
    }

    private fun isSubmitted(status: String): Boolean {
        val lower = status.lowercase()
        return "submitted" in lower || "entregado" in lower ||
            "enviado" in lower || "calificad" in lower
    }

    private fun parseDate(text: String): Long? {
        if (text.isBlank() || text.equals("Sin fecha", ignoreCase = true)) return null
        val locales = listOf(
            Locale.of("es", "MX"), Locale.of("es"), Locale.of("en", "US"), Locale.US
        )
        val patterns = listOf(
            "d 'de' MMMM 'de' yyyy, h:mm a",
            "d 'de' MMMM 'de' yyyy, HH:mm",
            "d MMMM yyyy, h:mm a",
            "d MMMM yyyy, HH:mm",
            "d MMM yyyy, h:mm a",
            "d MMM yyyy, HH:mm",
            "MMMM d, yyyy, h:mm a",
            "MMMM d, yyyy, HH:mm",
            "MMM d, yyyy, h:mm a",
            "MMM d, yyyy, HH:mm",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy h:mm a",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd h:mm a",
        )
        for (locale in locales) {
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, locale)
                    sdf.isLenient = false
                    val date = sdf.parse(text) ?: continue
                    return date.time / 1000
                } catch (_: Exception) {
                    continue
                }
            }
        }
        return null
    }

    private data class CourseInfo(val id: String, val name: String)
}
