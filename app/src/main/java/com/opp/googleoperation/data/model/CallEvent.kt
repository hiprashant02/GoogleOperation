package com.opp.googleoperation.data.model

data class CallEvent(
    val id: String = "",
    val deviceId: String,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: String, // "INCOMING", "OUTGOING", "MISSED", "REJECTED", "BLOCKED", "VOICEMAIL"
    val durationSeconds: Long = 0,
    val simSlot: Int = 1, // 1 for SIM 1, 2 for SIM 2
    val carrierName: String? = null,
    val geocodedLocation: String? = null,
    val callTimestamp: Long = System.currentTimeMillis(),
    val callState: String = "COMPLETED", // "RINGING", "OFFHOOK", "COMPLETED", "MISSED"
    var recordingUrl: String? = null
)
