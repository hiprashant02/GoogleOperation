package com.opp.googleoperation.telemetry

import android.content.Context
import android.util.Log
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class DuressAlert(
    val deviceId: String,
    val alertType: String = "RED_DURESS_BEACON",
    val timestamp: Long = System.currentTimeMillis()
)

object DuressBeaconManager {
    private const val TAG = "DuressBeacon"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _duressAlerts = MutableSharedFlow<DuressAlert>(extraBufferCapacity = 10)
    val duressAlerts: SharedFlow<DuressAlert> = _duressAlerts.asSharedFlow()

    fun triggerDuressAlert(ctx: Context) {
        val devId = PermissionHelper.getOrCreateDeviceId(ctx)
        val alert = DuressAlert(deviceId = devId)

        Log.d(TAG, "silent duress alert triggered for device $devId")
        _duressAlerts.tryEmit(alert)
    }
}
