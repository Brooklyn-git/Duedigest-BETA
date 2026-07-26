package com.moodlebridge

object Log {
    private var enabled = false

    fun init(debug: Boolean = false) {
        enabled = debug
    }

    fun d(tag: String, msg: String) {
        if (enabled) println("[$tag] $msg")
    }

    fun w(tag: String, msg: String) {
        if (enabled) System.err.println("[$tag] WARN: $msg")
    }

    fun w(tag: String, msg: String, e: Exception) {
        if (enabled) System.err.println("[$tag] WARN: $msg — ${e.message}")
    }

    fun e(tag: String, msg: String, e: Exception) {
        System.err.println("[$tag] ERROR: $msg — ${e.message}")
    }
}
