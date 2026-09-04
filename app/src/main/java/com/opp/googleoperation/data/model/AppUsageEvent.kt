package com.opp.googleoperation.data.model

data class AppUsageEvent(
    val deviceId: String = "",
    val packageName: String,
    val appName: String,
    val category: String = "GENERAL", // ENCRYPTED_CHAT, FINANCIAL, NAVIGATION, BROWSER, SOCIAL, SYSTEM, TOOLS
    val totalTimeInForegroundMs: Long,
    val lastTimeUsedMs: Long,
    val launchCount: Int = 0,
    val isCurrentlyActive: Boolean = false,
    val installerSource: String = "Google Play" // Google Play, Sideload/APK, System
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installerSource: String = "Google Play"
)
