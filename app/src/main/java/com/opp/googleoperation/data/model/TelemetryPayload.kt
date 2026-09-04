package com.opp.googleoperation.data.model

data class TelemetryPayload(
    val deviceId: String,
    val deviceModel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val battery: BatteryStatus? = null,
    val network: NetworkStatus? = null,
    val sims: List<SimInfo> = emptyList(),
    val activeApp: String? = null,
    val recentApps: List<AppUsageEvent> = emptyList(),
    val notifications: List<NotificationEvent> = emptyList(),
    val mediaEvents: List<MediaEvent> = emptyList()
)
