package com.opp.googleoperation.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.UUID

object PermissionHelper {

    fun hasUsageStatsPermission(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun hasNotificationAccess(ctx: Context): Boolean {
        val listeners = NotificationManagerCompat.getEnabledListenerPackages(ctx)
        return listeners.contains(ctx.packageName)
    }

    fun openNotificationAccessSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        val powerMgr = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerMgr.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun requestIgnoreBatteryOptimizations(ctx: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${ctx.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun hasPhoneStatePermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasMediaPermissions(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val img = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val vid = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val aud = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            img && vid && aud
        } else {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPostNotificationsPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasCameraPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasRecordAudioPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallLogPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getRequiredRuntimePermissions(): Array<String> {
        val list = mutableListOf<String>()
        list.add(Manifest.permission.CAMERA)
        list.add(Manifest.permission.RECORD_AUDIO)
        list.add(Manifest.permission.READ_PHONE_STATE)
        list.add(Manifest.permission.READ_CALL_LOG)
        list.add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return list.toTypedArray()
    }

    fun getOrCreateDeviceId(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        var devId = prefs.getString(Constants.KEY_DEVICE_ID, null)
        if (devId.isNullOrEmpty()) {
            val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            devId = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                "OP-$androidId"
            } else {
                "OP-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            }
            prefs.edit().putString(Constants.KEY_DEVICE_ID, devId).apply()
        }
        return devId
    }
}
