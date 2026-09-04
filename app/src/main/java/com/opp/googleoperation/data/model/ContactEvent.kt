package com.opp.googleoperation.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ContactEvent(
    @SerializedName("id")
    val id: String,

    @SerializedName("deviceId")
    val deviceId: String = "",

    @SerializedName("contactId")
    val contactId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("phoneNumber")
    val phoneNumber: String,

    @SerializedName("phoneType")
    val phoneType: String = "Mobile",

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("isStarred")
    val isStarred: Boolean = false,

    @SerializedName("lastUpdatedMs")
    val lastUpdatedMs: Long,

    @SerializedName("lookupKey")
    val lookupKey: String? = null,

    @SerializedName("isNewIntercept")
    val isNewIntercept: Boolean = false,

    @SerializedName("syncType")
    val syncType: String = "INITIAL_SYNC",

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
