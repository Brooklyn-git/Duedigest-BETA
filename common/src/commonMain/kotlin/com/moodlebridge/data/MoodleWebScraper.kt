package com.moodlebridge.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
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
                private val store = java.util.concurrent.ConcurrentHashMap<String, MutableList<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val key = url.host
                    store.getOrPut(key) { java.util.concurrent.CopyOnWriteArrayList() }.let { existing ->
                        for (cookie in cookies) {
                            existing.removeAll { it.name == cookie.name && it.path == cookie.path }
                            existing.add(cookie)
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> =
                    store[url.host].orEmpty().toList()
            })
            .build()
    }

    fun scrape(moodleUrl: String, username: String, password: String): List<Event> {
        val base = moodleUrl.trimEnd('/')
        println("DueNest Scraper: Starting scrape for $base")
        login(base, username, password)
        println("DueNest Scraper: Login OK, collecting courses...")
        val courses = collectCourses(base)
        println("DueNest Scraper: Found ${courses.size} courses: ${courses.map { it.name }}")
        val events = mutableListOf<Event>()
        for (course in courses) {
            val courseEvents = scrapeAssignments(base, course)
            println("DueNest Scraper: Course '${course.name}' (id=${course.id}) -> ${courseEvents.size} assignments")
            events.addAll(courseEvents)
        }
        println("DueNest Scraper: Total events = ${events.size}")
        return events.sortedBy { it.timestart }
    }

    private fun login(base: String, username: String, password: String) {
        val loginUrl = "$base/login/index.php"
        val pageResp = client.newCall(Request.Builder().url(loginUrl).get().build()).execute()
        val pageHtml = pageResp.body?.string() ?: throw IOException("Could not load login page")
        val doc = Jsoup.parse(pageHtml)
        val loginToken = doc.select("input[name=\"logintoken\"]").attr("value").orEmpty()

        val bodyBuilder = FormBody.Builder()
            .add("username", username)
            .add("password", password)
        if (loginToken.isNotBlank()) {
            bodyBuilder.add("logintoken", loginToken)
        }

        val response = client.newCall(
            Request.Builder().url(loginUrl).post(bodyBuilder.build()).build()
        ).execute()
        val responseUrl = response.request.url.toString()
        if (responseUrl.contains("/login")) {
            throw IOException("Login failed — check your credentials")
        }
    }

    private fun collectCourses(base: String): List<CourseInfo> {
        val url = "$base/my/"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val finalUrl = response.request.url.toString()
        println("DueNest Scraper: Dashboard URL after redirects: $finalUrl")
        if (finalUrl.contains("/login")) {
            throw IOException("Session expired — could not load dashboard")
        }
        val html = response.body?.string() ?: throw IOException("Empty dashboard response")
        println("DueNest Scraper: Dashboard HTML length = ${html.length}")
        val doc = Jsoup.parse(html)
        val allLinks = doc.select("a[href*=\"/course/view.php?id=\"]")
        println("DueNest Scraper: Found ${allLinks.size} course links in HTML")
        val courses = mutableListOf<CourseInfo>()
        val seen = mutableSetOf<String>()
        for (link in allLinks) {
            val href = link.attr("abs:href").ifEmpty { link.attr("href") }
            val idMatch = Regex("id=(\\d+)").find(href) ?: continue
            val id = idMatch.groupValues[1]
            if (id in seen) continue
            seen.add(id)
            val name = link.text().trim().replace(Regex("\\s+"), " ")
            println("DueNest Scraper:   Course link: id=$id, name='$name', href=$href")
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
        println("DueNest Scraper:   ${rows.size} rows in assignment table for '${course.name}'")
        val result = mutableListOf<Event>()
        for (row in rows) {
            val link = row.select("a[href*=\"/mod/assign/view.php?id=\"]").firstOrNull() ?: continue
            val href = link.attr("abs:href").ifEmpty { link.attr("href") }
            val title = link.text().trim()
            if (title.isBlank()) continue
            val assignId = Regex("id=(\\d+)").find(href)?.groupValues?.get(1) ?: continue
            val cells = row.select("td, th").map { it.text().trim() }
            val (dueDateStr, submissionStatus) = extractDateAndStatus(cells)
            if (isSubmitted(submissionStatus)) {
                println("DueNest Scraper:     SKIP '$title' (submitted: '$submissionStatus')")
                continue
            }
            val duedate = parseDate(dueDateStr)
            if (duedate != null && duedate <= now) {
                println("DueNest Scraper:     SKIP '$title' (past due: $dueDateStr -> $duedate, now=$now)")
                continue
            }
            println("DueNest Scraper:     INCLUDE '$title' (assignId=$assignId, duedate=$duedate, dueStr='$dueDateStr')")
            val description = scrapeDescription(base, href)
            result.add(
                Event(
                    id = "assign_$assignId",
                    name = title,
                    description = description,
                    timestart = duedate ?: Long.MAX_VALUE,
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

    private fun scrapeDescription(base: String, assignmentUrl: String): String {
        return try {
            val request = Request.Builder().url(assignmentUrl).get().build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return ""
            val doc = Jsoup.parse(html)
            val intro = doc.select("#intro").firstOrNull()
            if (intro != null) {
                intro.text().trim().replace(Regex("\\s+"), " ")
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractDateAndStatus(cells: List<String>): Pair<String, String> {
        if (cells.size >= 5) {
            return Pair(cells[2], cells[3])
        }
        if (cells.size == 4) {
            return Pair(cells[1], cells[2])
        }
        var date = ""
        var status = ""
        for (cell in cells) {
            val lower = cell.lowercase()
            if (date.isEmpty() && looksLikeDate(cell)) {
                date = cell
            } else if (status.isEmpty() && looksLikeStatus(lower)) {
                status = cell
            }
        }
        return Pair(date, status)
    }

    private fun looksLikeDate(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        if ("sin fecha" in lower || "no due date" in lower) return false
        return parseDate(text) != null || Regex("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}").containsMatchIn(text)
    }

    private fun looksLikeStatus(lower: String): Boolean {
        return "entregad" in lower || "submitted" in lower || "enviad" in lower ||
            "no entregad" in lower || "not submitted" in lower || "abierto" in lower ||
            "open" in lower || "calificad" in lower || "graded" in lower ||
            "cerrad" in lower || "closed" in lower
    }

    private fun isSubmitted(status: String): Boolean {
        val lower = status.lowercase()
        return ("submitted" in lower && "not" !in lower) ||
            ("entregad" in lower && "no" !in lower) ||
            ("enviad" in lower && "no" !in lower) ||
            "calificad" in lower || "graded" in lower
    }

    private fun parseDate(text: String): Long? {
        if (text.isBlank()) return null
        val lower = text.lowercase().trim()
        if ("sin fecha" in lower || "no due date" in lower) return null
        val locales = listOf(
            Locale("es", "MX"), Locale("es"), Locale("en", "US"), Locale.US
        )
        val patterns = listOf(
            "d 'de' MMMM 'de' yyyy, h:mm a",
            "d 'de' MMMM 'de' yyyy, HH:mm",
            "d 'de' MMMM 'de' yyyy",
            "d MMMM yyyy, h:mm a",
            "d MMMM yyyy, HH:mm",
            "d MMMM yyyy",
            "d MMM yyyy, h:mm a",
            "d MMM yyyy, HH:mm",
            "d MMM yyyy",
            "MMMM d, yyyy, h:mm a",
            "MMMM d, yyyy, HH:mm",
            "MMMM d, yyyy",
            "MMM d, yyyy, h:mm a",
            "MMM d, yyyy, HH:mm",
            "MMM d, yyyy",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy h:mm a",
            "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd h:mm a",
            "yyyy-MM-dd",
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
