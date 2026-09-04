package com.opp.googleoperation.telemetry

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.opp.googleoperation.data.model.InstalledAppInfo

class AppListTracker(private val ctx: Context) {

    fun getInstalledApps(includeSystem: Boolean = false): List<InstalledAppInfo> {
        val pm = ctx.packageManager
        val list = mutableListOf<InstalledAppInfo>()

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getInstalledPackages(0)
        }

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (!includeSystem && isSystem) {
                // skip core system apps to keep telemetry payloads concise
                continue
            }

            val appName = pm.getApplicationLabel(appInfo).toString()
            val versionName = pkg.versionName ?: "unknown"

            list.add(
                InstalledAppInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = versionName,
                    isSystemApp = isSystem,
                    firstInstallTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime
                )
            )
        }

        return list.sortedBy { it.appName.lowercase() }
    }
}
