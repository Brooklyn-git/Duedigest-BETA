package com.moodlebridge.data

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual fun gzipCompress(input: ByteArray): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).use { it.write(input) }
    return bos.toByteArray()
}

actual fun gzipDecompress(input: ByteArray): ByteArray {
    return GZIPInputStream(input.inputStream()).use { it.readBytes() }
}

actual fun base64Encode(input: ByteArray): String = Base64.getEncoder().encodeToString(input)

actual fun base64Decode(input: String): ByteArray = Base64.getDecoder().decode(input)
