package com.opp.googleoperation.telemetry

import android.content.Context
import android.os.Build
import com.opp.googleoperation.data.model.DeviceStatus
import com.opp.googleoperation.data.model.TelemetryPayload
import com.opp.googleoperation.util.PermissionHelper

class TelemetryAggregator(private val ctx: Context) {

    private val batteryTracker = BatteryTracker(ctx)
    private val simTracker = SimTracker(ctx)
    private val appListTracker = AppListTracker(ctx)
    private val usageStatsTracker = UsageStatsTracker(ctx)

    fun getDeviceStatus(): DeviceStatus {
        val deviceId = PermissionHelper.getOrCreateDeviceId(ctx)
        val battery = batteryTracker.getBatteryStatus()
        val network = simTracker.getNetworkStatus()
        val sims = simTracker.getSimInfoList()
        val activeApp = usageStatsTracker.getCurrentlyActiveApp() ?: "Home / Idle"

        return DeviceStatus(
            deviceId = deviceId,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            battery = battery,
            network = network,
            sims = sims,
            activeApp = activeApp,
            timestamp = System.currentTimeMillis()
        )
    }

    fun getFullTelemetryPayload(): TelemetryPayload {
        val status = getDeviceStatus()
        val recentApps = usageStatsTracker.getRecentAppUsage()

        return TelemetryPayload(
            deviceId = status.deviceId,
            deviceModel = "${status.manufacturer} ${status.model}",
            timestamp = status.timestamp,
            battery = status.battery,
            network = status.network,
            sims = status.sims,
            activeApp = status.activeApp,
            recentApps = recentApps
        )
    }

    fun getInstalledApps(includeSystem: Boolean = false) = appListTracker.getInstalledApps(includeSystem)
}
