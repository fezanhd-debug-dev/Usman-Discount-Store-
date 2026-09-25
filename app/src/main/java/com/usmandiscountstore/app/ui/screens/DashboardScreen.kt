package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.SecurityPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit) {

    val context = LocalContext.current
    val prefs = SecurityPreferences(context)
    val userName = prefs.getUserName()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Usman Discount Store",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Text(
                            "Vehari Road, Old Hasilpur",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        prefs.clearSession()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Welcome Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Khush Amdeed 👋",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Text(
                        userName.ifEmpty { "Admin" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            // Geo-fence Status
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = BrandGreen)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Hasilpur Geofence Active",
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            fontSize = 14.sp
                        )
                        Text(
                            "30m radius — Terminal UDS-HSL-01",
                            fontSize = 11.sp,
                            color = BrandGreenDark
                        )
                    }
                }
            }

            // Quick Stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Hazir Aaj", "0 / 0", BrandGreen, Modifier.weight(1f))
                StatCard("Late Aaj", "0", BrandOrange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Advance", "Rs. 0", Color(0xFFDC2626), Modifier.weight(1f))
                StatCard("Total Staff", "0", Color(0xFF2563EB), Modifier.weight(1f))
            }

            Text(
                "Store Modules",
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )

            ActionCard("Camera Biometric Hazri", "Face detection se hazri lagao", Icons.Default.CameraAlt, BrandGreen) {}
            ActionCard("Mulazimeen Record", "Staff ki tafseelat", Icons.Default.People, Color(0xFF2563EB)) {}
            ActionCard("Advance & Peshgi Khata", "Udhaar aur cash peshgi", Icons.Default.AccountBalanceWallet, BrandOrange) {}
            ActionCard("Salary Slips", "Mahana tankhwah", Icons.Default.ReceiptLong, Color(0xFF7C3AED)) {}
            ActionCard("Admin Settings", "Shift aur geofence", Icons.Default.Tune, Color(0xFF4B5563)) {}

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = TextGray)
            Spacer(Modifier.height(6.dp))
            Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = TextGray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextGray)
        }
    }
}
