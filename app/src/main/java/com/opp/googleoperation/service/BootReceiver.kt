package com.opp.googleoperation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        Log.d(TAG, "received system broadcast: $action — ensuring TelemetryService is alive")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                try {
                    TelemetryService.start(context.applicationContext)
                    Log.d(TAG, "successfully started TelemetryService on $action")
                } catch (e: Exception) {
                    Log.w(TAG, "failed starting TelemetryService on $action", e)
                }
            }
        }
    }
}
