package com.opp.googleoperation.data.model

data class NotificationEvent(
    val id: String = "",
    val deviceId: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val subText: String = "",
    val postTime: Long = System.currentTimeMillis(),
    val category: String = ""
)
