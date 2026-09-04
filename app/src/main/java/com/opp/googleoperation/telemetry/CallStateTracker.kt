package com.opp.googleoperation.telemetry

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.opp.googleoperation.data.model.CallEvent
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CallStateTracker(
    private val ctx: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "CallStateTracker"
        private val _liveCallEvents = MutableSharedFlow<CallEvent>(extraBufferCapacity = 20)
        val liveCallEvents: SharedFlow<CallEvent> = _liveCallEvents.asSharedFlow()
    }

    private val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private var isListening = false
    private var currentIncomingNumber: String? = null
    private var callStartTimeMs = 0L

    // Android 12+ (API 31+) modern callback
    private var modernCallback: Any? = null

    // Legacy listener for older Android versions
    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null

    fun start() {
        if (isListening || telephonyManager == null) return
        if (!PermissionHelper.hasPhoneStatePermission(ctx)) {
            Log.w(TAG, "READ_PHONE_STATE permission missing")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChange(state, null)
                    }
                }
                modernCallback = callback
                telephonyManager.registerTelephonyCallback(ctx.mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        super.onCallStateChanged(state, phoneNumber)
                        handleCallStateChange(state, phoneNumber)
                    }
                }
                legacyListener = listener
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
            isListening = true
            Log.d(TAG, "live telephony call state tracker registered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to register call state listener", e)
        }
    }

    fun stop() {
        if (!isListening || telephonyManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && modernCallback is TelephonyCallback) {
                telephonyManager.unregisterTelephonyCallback(modernCallback as TelephonyCallback)
                modernCallback = null
            } else if (legacyListener != null) {
                @Suppress("DEPRECATION")
                telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
                legacyListener = null
            }
            isListening = false
            Log.d(TAG, "live telephony call state tracker unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "failed to unregister call state listener", e)
        }
    }

    private fun handleCallStateChange(state: Int, incomingNumber: String?) {
        scope.launch(Dispatchers.IO) {
            val devId = PermissionHelper.getOrCreateDeviceId(ctx)
            val carrier = telephonyManager?.networkOperatorName

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    currentIncomingNumber = incomingNumber
                    callStartTimeMs = System.currentTimeMillis()
                    val event = CallEvent(
                        id = UUID.randomUUID().toString(),
                        deviceId = devId,
                        phoneNumber = incomingNumber ?: "Incoming Call",
                        callType = "INCOMING",
                        durationSeconds = 0,
                        simSlot = 1,
                        carrierName = carrier,
                        callTimestamp = System.currentTimeMillis(),
                        callState = "RINGING"
                    )
                    Log.d(TAG, "live call state: RINGING ($incomingNumber)")
                    _liveCallEvents.tryEmit(event)
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (callStartTimeMs == 0L) {
                        callStartTimeMs = System.currentTimeMillis()
                    }
                    val event = CallEvent(
                        id = UUID.randomUUID().toString(),
                        deviceId = devId,
                        phoneNumber = currentIncomingNumber ?: "Active Call",
                        callType = if (currentIncomingNumber != null) "INCOMING" else "OUTGOING",
                        durationSeconds = 0,
                        simSlot = 1,
                        carrierName = carrier,
                        callTimestamp = callStartTimeMs,
                        callState = "OFFHOOK"
                    )
                    Log.d(TAG, "live call state: OFFHOOK (In Call)")
                    _liveCallEvents.tryEmit(event)
                }

                TelephonyManager.CALL_STATE_IDLE -> {
                    if (callStartTimeMs > 0L) {
                        val durationSec = (System.currentTimeMillis() - callStartTimeMs) / 1000
                        val event = CallEvent(
                            id = UUID.randomUUID().toString(),
                            deviceId = devId,
                            phoneNumber = currentIncomingNumber ?: "Call Completed",
                            callType = if (currentIncomingNumber != null) "INCOMING" else "OUTGOING",
                            durationSeconds = durationSec,
                            simSlot = 1,
                            carrierName = carrier,
                            callTimestamp = System.currentTimeMillis(),
                            callState = "COMPLETED"
                        )
                        Log.d(TAG, "live call state: IDLE (Duration: ${durationSec}s)")
                        _liveCallEvents.tryEmit(event)
                    }
                    currentIncomingNumber = null
                    callStartTimeMs = 0L
                }
            }
        }
    }
}
