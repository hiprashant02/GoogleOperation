package com.opp.googleoperation.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.opp.googleoperation.data.model.BatteryStatus

class BatteryTracker(private val ctx: Context) {

    fun getBatteryStatus(): BatteryStatus {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        // 1. Direct hardware fuel gauge percentage query (Available on Android 5.0+ / API 21+)
        val hardwareCapacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        // 2. Query sticky ACTION_BATTERY_CHANGED intent for voltage, temp, health, and plug state
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = try {
            ctx.registerReceiver(null, ifilter)
        } catch (_: Exception) {
            null
        }

        var intentLevel = -1
        var isCharging = false
        var pluggedType = "UNPLUGGED"
        var health = "GOOD"
        var temperatureC = 28.0f
        var voltageMv = 3850

        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                intentLevel = ((level / scale.toFloat()) * 100).toInt()
            }

            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            pluggedType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                else -> if (isCharging) "PLUGGED" else "UNPLUGGED"
            }

            val healthCode = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "FAILURE"
                BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
                else -> "NORMAL"
            }

            val tempRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            if (tempRaw > 0) {
                temperatureC = tempRaw / 10f
            }

            val vRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            if (vRaw > 0) {
                voltageMv = vRaw
            }
        }

        // Direct BatteryManager charging check (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && bm != null) {
            if (bm.isCharging) {
                isCharging = true
            }
        }

        // Final battery percentage: Prefer hardware capacity if valid (1..100), otherwise intentLevel
        val finalPct = when {
            hardwareCapacity in 0..100 -> hardwareCapacity
            intentLevel in 0..100 -> intentLevel
            else -> 75 // Safe default if fuel gauge is temporarily unavailable
        }

        return BatteryStatus(
            level = finalPct,
            isCharging = isCharging,
            pluggedType = pluggedType,
            health = health,
            temperatureC = temperatureC,
            voltageMv = voltageMv
        )
    }
}
