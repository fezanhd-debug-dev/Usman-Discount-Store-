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

        // Location Permission Check (Pehle jaisa hi)
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
                    
                    // ==========================================
                    // LICENSE CHECK LOGIC (NEW)
                    // ==========================================
                    var isLicenseChecked by remember { mutableStateOf(false) }
                    var isLicenseValid by remember { mutableStateOf(false) }
                    var retryTrigger by remember { mutableIntStateOf(0) }

                    LaunchedEffect(retryTrigger) {
                        val context = applicationContext
                        isLicenseChecked = false

                        // 1. Pehle server se verify karein
                        isLicenseValid = LicenseManager.verifyLicense(context)

                        // 2. Agar valid nahi hai, to check karein ke device register hai ya nahi
                        // Agar pehli baar app khuli hai (trial activate nahi hua), to register karein
                        if (!isLicenseValid && !LicenseManager.isLicenseLocallyValid(context)) {
                            LicenseManager.registerDevice(context, "Usman Discount Store")
                            isLicenseValid = LicenseManager.verifyLicense(context)
                        }
                        isLicenseChecked = true
                    }

                    if (!isLicenseChecked) {
                        // Loading Screen (Jab tak server se jawab aa raha hai)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (!isLicenseValid) {
                        // License Lock Screen (Agar license expire/blocked hai)
                        LicenseLockScreen(onRetry = {
                            isLicenseChecked = false
                            retryTrigger++ // Dobara server se check karne ke liye
                        })
                    } else {
                        // ✅ License Valid → Normal App Flow
                        val navController = rememberNavController()
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
