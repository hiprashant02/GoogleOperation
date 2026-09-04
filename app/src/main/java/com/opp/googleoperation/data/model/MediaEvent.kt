package com.opp.googleoperation.data.model

data class MediaEvent(
    val mediaId: Long,
    val deviceId: String,
    val fileName: String,
    val mimeType: String,
    val mediaType: String, // "image", "video", "audio"
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val contentUri: String,
    val relativePath: String = "",
    var b2Url: String? = null,
    var uploadStatus: String = "pending"
)
