package com.opp.googleoperation.util

object Constants {
    const val PREFS_NAME = "tactical_telemetry_prefs"
    const val KEY_DEVICE_ID = "pref_device_id"
    const val KEY_SERVER_URL = "pref_server_url"
    const val KEY_SERVICE_ENABLED = "pref_service_enabled"
    const val KEY_LAST_SYNC = "pref_last_sync_timestamp"

    // default local/deployed endpoint - can be changed in settings UI
    const val DEFAULT_WORKER_URL = "https://YOUR_WORKER_SUBDOMAIN.workers.dev"

    // Backblaze B2 configuration
    const val B2_KEY_ID = "YOUR_B2_KEY_ID"
    const val B2_APPLICATION_KEY = "YOUR_B2_APPLICATION_KEY"
    const val B2_BUCKET_NAME = "YOUR_B2_BUCKET_NAME"
    const val B2_KEY_NAME = "YOUR_B2_KEY_NAME"

    const val ACTION_START_SERVICE = "com.opp.googleoperation.action.START_SERVICE"
    const val ACTION_STOP_SERVICE = "com.opp.googleoperation.action.STOP_SERVICE"
    const val ACTION_FORCE_SYNC = "com.opp.googleoperation.action.FORCE_SYNC"

    const val NOTIFICATION_ID = 1001
}
