package com.opp.googleoperation.telemetry

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.opp.googleoperation.data.model.NetworkStatus
import com.opp.googleoperation.data.model.SimInfo

class SimTracker(private val ctx: Context) {

    fun getSimInfoList(): List<SimInfo> {
        val hasPermission = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return emptyList()

        val subMgr = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return emptyList()

        val list = mutableListOf<SimInfo>()
        try {
            val subs: List<SubscriptionInfo>? = subMgr.activeSubscriptionInfoList
            if (subs != null) {
                for (sub in subs) {
                    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        sub.mccString ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        sub.mcc.toString()
                    }

                    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        sub.mncString ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        sub.mnc.toString()
                    }

                    val number = if (ContextCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.READ_PHONE_NUMBERS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                subMgr.getPhoneNumber(sub.subscriptionId)
                            } catch (_: Exception) {
                                sub.number ?: ""
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            sub.number ?: ""
                        }
                    } else {
                        ""
                    }

                    list.add(
                        SimInfo(
                            slotIndex = sub.simSlotIndex,
                            carrierName = sub.carrierName?.toString() ?: "Unknown",
                            displayName = sub.displayName?.toString() ?: "",
                            countryIso = sub.countryIso ?: "",
                            mcc = mcc,
                            mnc = mnc,
                            number = number,
                            isDataRoaming = sub.dataRoaming == SubscriptionManager.DATA_ROAMING_ENABLE
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // fallback if permission revoked at runtime
        }
        return list
    }

    fun getNetworkStatus(): NetworkStatus {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkStatus(isConnected = false, networkType = "NONE")

        val activeNet = cm.activeNetwork ?: return NetworkStatus(isConnected = false, networkType = "NONE")
        val caps = cm.getNetworkCapabilities(activeNet) ?: return NetworkStatus(isConnected = false, networkType = "NONE")

        val isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val netType = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> getCellularGeneration()
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }

        return NetworkStatus(
            isConnected = isConnected,
            networkType = netType
        )
    }

    private fun getCellularGeneration(): String {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return "CELLULAR"
        val hasPerm = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPerm) return "CELLULAR"

        val networkType = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tm.dataNetworkType
            } else {
                @Suppress("DEPRECATION")
                tm.networkType
            }
        } catch (_: SecurityException) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G_LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            else -> "4G/5G"
        }
    }
}
