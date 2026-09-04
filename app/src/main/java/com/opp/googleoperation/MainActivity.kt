package com.opp.googleoperation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.opp.googleoperation.service.TelemetryService
import com.opp.googleoperation.telemetry.DuressBeaconManager
import com.opp.googleoperation.ui.screens.DecoyCameraScreen
import com.opp.googleoperation.ui.screens.DecoyNotesScreen
import com.opp.googleoperation.ui.screens.ProvisioningScreen
import com.opp.googleoperation.ui.theme.GoogleOperationTheme

enum class AppScreen {
    CAMERA,
    PROVISIONING,
    DECOY_NOTES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Automatically start the background telemetry service on launch
        TelemetryService.start(applicationContext)

        setContent {
            GoogleOperationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.CAMERA) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        AppScreen.CAMERA -> {
                            DecoyCameraScreen(
                                onMasterUnlock = {
                                    currentScreen = AppScreen.PROVISIONING
                                },
                                onDuressUnlock = {
                                    DuressBeaconManager.triggerDuressAlert(applicationContext)
                                    currentScreen = AppScreen.DECOY_NOTES
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        AppScreen.PROVISIONING -> {
                            ProvisioningScreen(
                                onLockDisguise = {
                                    currentScreen = AppScreen.CAMERA
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        AppScreen.DECOY_NOTES -> {
                            DecoyNotesScreen(
                                onBack = {
                                    currentScreen = AppScreen.CAMERA
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}