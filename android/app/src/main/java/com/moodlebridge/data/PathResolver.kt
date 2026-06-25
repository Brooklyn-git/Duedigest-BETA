package com.moodlebridge.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

object PathResolver {

    fun writeIcs(context: Context, path: String, content: String) {
        if (path.startsWith("content://")) {
            try {
                val uri = Uri.parse(path)
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    ?: throw IOException("Cannot open $path")
            } catch (e: SecurityException) {
                Log.w("PathResolver", "Permission denied for $path, falling back to cache", e)
                File(context.cacheDir, "ics/calendar.ics").also { it.parentFile?.mkdirs() }.writeText(content)
            }
        } else {
            File(path).also { it.parentFile?.mkdirs() }.writeText(content)
        }
    }

    fun writeMarkdownFiles(context: Context, path: String, fallbackSubdir: String, files: List<Pair<String, String>>) {
        if (path.startsWith("content://")) {
            try {
                val treeDoc = DocumentFile.fromTreeUri(context, Uri.parse(path))
                treeDoc?.let { doc ->
                    for ((name, content) in files) {
                        val existing = doc.findFile(name)
                        val file = existing ?: doc.createFile("text/plain", name.removeSuffix(".md"))
                        file?.let { f ->
                            context.contentResolver.openOutputStream(f.uri)?.use { out ->
                                out.write(content.toByteArray())
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w("PathResolver", "Permission denied for $path, falling back to cache/$fallbackSubdir", e)
                File(context.cacheDir, fallbackSubdir).also { it.mkdirs() }.let { dir ->
                    for ((name, content) in files) {
                        File(dir, name).writeText(content)
                    }
                }
            }
        } else {
            File(path).also { it.mkdirs() }.let { dir ->
                for ((name, content) in files) {
                    File(dir, name).writeText(content)
                }
            }
        }
    }
}
