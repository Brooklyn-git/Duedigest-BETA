package com.moodlebridge.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SyncPayload(
    val manualEvents: List<Event>,
    val taskCompletion: Map<String, Boolean>,
    val deletedEventIds: Set<String>,
    val deviceId: String,
    val timestamp: Long,
)

data class MergeResult(
    val manualEvents: List<Event>,
    val taskCompletion: Map<String, Boolean>,
    val deletedEventIds: Set<String>,
)

object SyncManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun generatePayload(
        manualEvents: List<Event>,
        taskCompletion: Map<String, Boolean>,
        deletedEventIds: Set<String>,
        deviceId: String,
    ): SyncPayload = SyncPayload(
        manualEvents = manualEvents,
        taskCompletion = taskCompletion,
        deletedEventIds = deletedEventIds,
        deviceId = deviceId,
        timestamp = System.currentTimeMillis(),
    )

    fun mergePayload(local: SyncPayload, remote: SyncPayload): MergeResult {
        val mergedDeleted = local.deletedEventIds + remote.deletedEventIds

        val mergedEvents = mutableMapOf<String, Event>()
        for (e in local.manualEvents) mergedEvents[e.id] = e
        for (e in remote.manualEvents) {
            val existing = mergedEvents[e.id]
            if (existing == null || remote.timestamp > local.timestamp) {
                mergedEvents[e.id] = e
            }
        }

        for (id in mergedDeleted) {
            mergedEvents.remove(id)
        }

        val mergedCompletion = mutableMapOf<String, Boolean>()
        val allKeys = local.taskCompletion.keys.toMutableSet().apply { addAll(remote.taskCompletion.keys) }
        for (key in allKeys) {
            if (key in mergedDeleted) continue
            val localVal = local.taskCompletion[key]
            val remoteVal = remote.taskCompletion[key]
            mergedCompletion[key] = when {
                localVal == null -> remoteVal ?: false
                remoteVal == null -> localVal
                else -> localVal || remoteVal
            }
        }

        return MergeResult(
            manualEvents = mergedEvents.values.sortedBy { it.timestart },
            taskCompletion = mergedCompletion,
            deletedEventIds = mergedDeleted,
        )
    }

    fun serializePayload(payload: SyncPayload): String {
        val jsonBytes = json.encodeToString(SyncPayload.serializer(), payload).encodeToByteArray()
        val compressed = gzipCompress(jsonBytes)
        return base64Encode(compressed)
    }

    fun deserializePayload(data: String): SyncPayload? {
        return try {
            val compressed = base64Decode(data)
            val jsonBytes = gzipDecompress(compressed)
            json.decodeFromString<SyncPayload>(jsonBytes.decodeToString())
        } catch (_: Exception) {
            null
        }
    }
}

expect fun gzipCompress(input: ByteArray): ByteArray
expect fun gzipDecompress(input: ByteArray): ByteArray
expect fun base64Encode(input: ByteArray): String
expect fun base64Decode(input: String): ByteArray
