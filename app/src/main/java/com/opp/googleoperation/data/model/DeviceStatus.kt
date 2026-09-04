package com.opp.googleoperation.data.model

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val pluggedType: String,
    val health: String,
    val temperatureC: Float,
    val voltageMv: Int
)

data class NetworkStatus(
    val isConnected: Boolean,
    val networkType: String, // "5G", "4G", "WIFI", "NONE"
    val signalStrengthDbm: Int = 0,
    val ipAddress: String = ""
)

data class DeviceStatus(
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val battery: BatteryStatus,
    val network: NetworkStatus,
    val sims: List<SimInfo>,
    val activeApp: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
