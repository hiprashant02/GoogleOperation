package com.opp.googleoperation.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.opp.googleoperation.data.model.NotificationEvent
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationReceiverService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationReceiver"
        private val _notificationEvents = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 100)
        val notificationEvents: SharedFlow<NotificationEvent> = _notificationEvents.asSharedFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: ""
        if (pkg == packageName) return // skip own service alerts

        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return

        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        var content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // check big text if standard content is short or empty
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) {
            content = bigText
        }

        // check messaging style conversation title (for group chats)
        val convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        if (!convTitle.isNullOrBlank() && title != convTitle) {
            title = "$convTitle ($title)"
        }

        // check text lines for inbox style
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty() && content.isBlank()) {
            content = textLines.joinToString("\n")
        }

        if (title.isBlank() && content.isBlank()) return

        val appName = try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            pkg
        }

        val category = notif.category ?: ""
        val devId = PermissionHelper.getOrCreateDeviceId(this)

        val event = NotificationEvent(
            id = sbn.key ?: "${sbn.id}_${sbn.postTime}",
            deviceId = devId,
            packageName = pkg,
            appName = appName,
            title = title,
            content = content,
            subText = subText,
            postTime = sbn.postTime,
            category = category
        )

        Log.d(TAG, "captured: [$appName] $title: $content")
        _notificationEvents.tryEmit(event)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}

