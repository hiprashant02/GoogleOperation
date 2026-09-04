package com.opp.googleoperation.data.model

data class SimInfo(
    val slotIndex: Int,
    val carrierName: String,
    val displayName: String,
    val countryIso: String,
    val mcc: String,
    val mnc: String,
    val number: String = "",
    val isDataRoaming: Boolean = false
)
