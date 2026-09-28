package com.usmandiscountstore.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.usmandiscountstore.app.navigation.AppNavGraph
import com.usmandiscountstore.app.ui.screens.LicenseLockScreen
import com.usmandiscountstore.app.util.LicenseManager

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Location Permission Check
        val needs = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            needs.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needs.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (needs.isNotEmpty()) permLauncher.launch(needs.toTypedArray())

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    var isLicenseChecked by remember { mutableStateOf(false) }
                    var isLicenseValid by remember { mutableStateOf(false) }
                    var retryTrigger by remember { mutableIntStateOf(0) }

                    LaunchedEffect(retryTrigger) {
                        val context = applicationContext
                        isLicenseChecked = false

                        // 1. Pehle LOCAL cache check karein (agar pehle se valid hai to foran app khul jaye)
                        val localValid = LicenseManager.isLicenseLocallyValid(context)

                        if (localValid) {
                            // Agar local cache valid hai, to foran app dikhayein
                            isLicenseValid = true
                            isLicenseChecked = true
                            
                            // Background mein server se verify karein (agar block/expire ho gaya ho)
                            val serverValid = LicenseManager.verifyLicense(context)
                            if (!serverValid) {
                                // Agar server ne block/expire kar diya hai, to lock screen dikhayein
                                isLicenseValid = false
                            }
                        } else {
                            // 2. Agar local cache nahi hai, to server se register + verify karein
                            val registered = LicenseManager.registerDevice(context, "Usman Discount Store")
                            if (registered) {
                                isLicenseValid = LicenseManager.verifyLicense(context)
                            } else {
                                isLicenseValid = false
                            }
                            isLicenseChecked = true
                        }
                    }

                    if (!isLicenseChecked) {
                        // Loading Screen
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (!isLicenseValid) {
                        // Lock Screen
                        LicenseLockScreen(onRetry = {
                            isLicenseChecked = false
                            retryTrigger++
                        })
                    } else {
                        // ✅ License Valid → Normal App
                        val navController = rememberNavController()
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
