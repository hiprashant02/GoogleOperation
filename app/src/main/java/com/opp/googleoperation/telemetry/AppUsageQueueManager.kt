package com.opp.googleoperation.telemetry

import android.content.Context
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.opp.googleoperation.data.model.AppUsageEvent
import com.opp.googleoperation.network.ApiClient
import java.io.File

class AppUsageQueueManager(private val ctx: Context) {

    companion object {
        private const val TAG = "AppUsageQueue"
        private const val QUEUE_FILE_NAME = "pending_app_usage_queue.json"
        private val LOCK = Any()
    }

    private val queueFile: File
        get() = File(ctx.filesDir, QUEUE_FILE_NAME)

    fun enqueue(events: List<AppUsageEvent>) {
        if (events.isEmpty()) return
        synchronized(LOCK) {
            try {
                val existing = getQueuedEventsInternal().toMutableList()
                val map = existing.associateBy { "${it.deviceId}_${it.packageName}" }.toMutableMap()

                for (event in events) {
                    val key = "${event.deviceId}_${event.packageName}"
                    val prev = map[key]
                    if (prev == null || event.lastTimeUsedMs >= prev.lastTimeUsedMs) {
                        map[key] = event
                    }
                }

                val merged = map.values.toList()
                saveQueueInternal(merged)
                Log.d(TAG, "enqueued ${events.size} items, total queued on disk: ${merged.size}")
            } catch (e: Exception) {
                Log.w(TAG, "failed to enqueue app usage items", e)
            }
        }
    }

    fun getQueuedEvents(): List<AppUsageEvent> {
        synchronized(LOCK) {
            return getQueuedEventsInternal()
        }
    }

    fun clearQueue() {
        synchronized(LOCK) {
            try {
                if (queueFile.exists()) {
                    val deleted = queueFile.delete()
                    Log.d(TAG, "app usage queue successfully purged from disk: $deleted")
                }
            } catch (e: Exception) {
                Log.w(TAG, "failed to clear app usage queue file", e)
            }
        }
    }

    private fun getQueuedEventsInternal(): List<AppUsageEvent> {
        if (!queueFile.exists()) return emptyList()
        return try {
            val json = queueFile.readText(Charsets.UTF_8)
            if (json.isBlank()) return emptyList()
            val type = object : TypeToken<List<AppUsageEvent>>() {}.type
            ApiClient.gson.fromJson<List<AppUsageEvent>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "error reading app usage queue file", e)
            emptyList()
        }
    }

    private fun saveQueueInternal(events: List<AppUsageEvent>) {
        try {
            val json = ApiClient.gson.toJson(events)
            queueFile.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "error writing app usage queue file", e)
        }
    }
}
