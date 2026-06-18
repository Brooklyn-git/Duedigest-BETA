package com.moodlebridge.data

object IcsGenerator {

    fun generate(events: List<Event>): String {
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Moodle Calendar Bridge//EN",
            "X-WR-CALNAME:Moodle Calendar",
        )
        for (ev in events) {
            val uid = "${ev.id}@moodle-calendar-bridge"
            val dtstart = fmtIcsDt(ev.timestart)
            val durationSec = ev.timeduration
            val endTs = if (durationSec > 0) ev.timestart + durationSec else ev.timestart + 3600
            val dtend = fmtIcsDt(endTs)
            val summary = escapeIcs(ev.name)
            val desc = buildString {
                val d = escapeIcs(ev.description)
                if (d.isNotBlank()) append(d)
                if (ev.course.isNotBlank()) {
                    if (isNotEmpty()) append("\\n\\n")
                    append("Course: ${ev.course}")
                }
                if (ev.url.isNotBlank()) {
                    if (isNotEmpty()) append("\\n")
                    append("URL: ${ev.url}")
                }
            }
            lines.add("BEGIN:VEVENT")
            lines.add("UID:$uid")
            lines.add("DTSTART:$dtstart")
            lines.add("DTEND:$dtend")
            lines.add("SUMMARY:$summary")
            lines.add("DESCRIPTION:$desc")
            lines.add("END:VEVENT")
        }
        lines.add("END:VCALENDAR")
        return lines.joinToString("\r\n") + "\r\n"
    }

    private fun fmtIcsDt(timestamp: Long): String {
        val millis = timestamp * 1000
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
        val d = String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH))
        val h = String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
        val min = String.format("%02d", cal.get(java.util.Calendar.MINUTE))
        val s = String.format("%02d", cal.get(java.util.Calendar.SECOND))
        return "$y${m}${d}T${h}${min}${s}"
    }

    private fun escapeIcs(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
}
