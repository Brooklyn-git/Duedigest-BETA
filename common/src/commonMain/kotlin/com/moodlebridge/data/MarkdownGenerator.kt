package com.moodlebridge.data

import java.util.Calendar

object MarkdownGenerator {

    fun generateTasksList(events: List<Event>, completionMap: Map<String, Boolean>): String {
        val sorted = events.sortedBy { it.timestart }
        val grouped = sorted.groupBy { it.course.ifBlank { "General" } }.toSortedMap()

        return buildString {
            appendLine("# Tasks")
            appendLine()
            for ((course, courseEvents) in grouped) {
                appendLine("## $course")
                for (ev in courseEvents) {
                    val cal = Calendar.getInstance().apply { timeInMillis = ev.timestart * 1000 }
                    val y = cal.get(Calendar.YEAR)
                    val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
                    val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
                    val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    val day = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                    val h = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
                    val min = String.format("%02d", cal.get(Calendar.MINUTE))
                    val checked = completionMap[ev.id] == true
                    val mark = if (checked) "x" else " "
                    val title = ev.name.replace("[", "\\[").replace("]", "\\]")
                    appendLine("- [$mark] $title (due: $y-$m-$d $day $h:$min)")
                }
                appendLine()
            }
        }
    }

    fun generateIntroMd(lang: String): String {
        val s = { key: String -> Strings.get(key, lang) }
        return buildString {
            appendLine("# ${s("intro_title")}")
            appendLine()
            appendLine(s("intro_welcome"))
            appendLine()
            appendLine(s("intro_setup_outputs"))
            appendLine()
            appendLine("## ${s("intro_getting_started")}")
            appendLine()
            appendLine(s("intro_step1"))
            appendLine(s("intro_step2"))
            appendLine(s("intro_step3"))
            appendLine()
            appendLine("## ${s("intro_outputs")}")
            appendLine()
            appendLine(s("intro_ics"))
            appendLine(s("intro_logseq"))
            appendLine(s("intro_obsidian"))
            appendLine(s("intro_tasks"))
            appendLine()
            appendLine(s("intro_configure"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(s("intro_footer"))
        }
    }

    fun generateLogseq(events: List<Event>): List<Pair<String, String>> {
        return events.map { ev ->
            val cal = Calendar.getInstance().apply { timeInMillis = ev.timestart * 1000 }
            val y = cal.get(Calendar.YEAR)
            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val day = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val h = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            val min = String.format("%02d", cal.get(Calendar.MINUTE))
            val deadlineStr = "$y-$m-$d $day $h:$min"

            val title = ev.name
            val safeName = title.map { c ->
                if (c.isLetterOrDigit() || c == ' ' || c == '-' || c == '_') c else '_'
            }.joinToString("").trim().let { if (it.isBlank()) "event-${ev.id}" else it }

            val content = buildString {
                appendLine("- $title")
                appendLine("  DEADLINE: <$deadlineStr>")
                if (ev.course.isNotBlank()) appendLine("  course:: ${ev.course}")
                if (ev.url.isNotBlank()) appendLine("  url:: ${ev.url}")
                if (ev.description.isNotBlank()) appendLine("  description:: ${ev.description}")
            }

            "$safeName.md" to content
        }
    }

    fun generateObsidian(events: List<Event>): List<Pair<String, String>> {
        return events.map { ev ->
            val cal = Calendar.getInstance().apply { timeInMillis = ev.timestart * 1000 }
            val y = cal.get(Calendar.YEAR)
            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            val h = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            val min = String.format("%02d", cal.get(Calendar.MINUTE))
            val dateStr = "$y-$m-$d"
            val dueStr = "$y-$m-$d $h:$min"

            val content = buildString {
                appendLine("---")
                appendLine("due: $dueStr")
                appendLine("title: ${ev.name}")
                if (ev.course.isNotBlank()) appendLine("course: ${ev.course}")
                if (ev.url.isNotBlank()) appendLine("url: ${ev.url}")
                appendLine("---")
                appendLine("")
                if (ev.description.isNotBlank()) appendLine(ev.description)
                appendLine("")
            }

            "$dateStr.md" to content
        }
    }
}
