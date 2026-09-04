package com.opp.googleoperation.data.model

data class AudioThreatEvent(
    val id: String = "",
    val deviceId: String,
    val threatType: String, // "DISTRESS_SCREAM", "SUDDEN_SPIKE", "DURESS_TRIGGER", "AMBIENT_SAMPLE"
    val confidenceScore: Float,
    val compressedAudioPath: String,
    val fileSizeBytes: Long,
    val durationSec: Int,
    val timestamp: Long = System.currentTimeMillis(),
    var b2Url: String? = null
)
