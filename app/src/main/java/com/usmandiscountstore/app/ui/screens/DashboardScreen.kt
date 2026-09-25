package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.SecurityPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateSalary: () -> Unit,
    onNavigateStaff: () -> Unit,
    onNavigateAttendance: () -> Unit,
    onNavigateAdvance: () -> Unit,
    onNavigateSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs = SecurityPreferences(context)
    val userName = prefs.getUserName().ifEmpty { "Admin" }
    val role = prefs.getRole().ifEmpty { "ADMIN" }
    val isAdmin = role == "ADMIN"
    val todayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogoSmall(size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Usman Discount Store", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text("Vehari Road, Old Hasilpur", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { prefs.clearSession(); onLogout() }) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(BrandGreenLight),
                        contentAlignment = Alignment.Center) {
                        Text(userName.firstOrNull()?.toString() ?: "U",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Khush Amdeed", fontSize = 12.sp, color = TextGray)
                        Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = if (isAdmin) BrandGreen else BrandOrange) {
                            Text(if (isAdmin) "ADMIN ACCESS" else "MODERATOR ACCESS",
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            Text(todayDate, fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = 4.dp))

            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Hasilpur Geofence Active", fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 13.sp)
                        Text("Vehari Road Counter • UDS-HSL-01", fontSize = 11.sp, color = BrandGreenDark)
                    }
                }
            }

            Text("Store Modules", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp))

            ModuleCard("Hazri Lagao", "GPS + Half Day option", Icons.Default.CheckCircle, BrandGreen, onNavigateAttendance)

            if (isAdmin) {
                ModuleCard("Mulazimeen Management", "Staff add / edit / delete", Icons.Default.People, Color(0xFF2563EB), onNavigateStaff)
            } else {
                ModuleCard("Mulazimeen List", "Staff dekho", Icons.Default.People, Color(0xFF2563EB), onNavigateStaff)
            }

            ModuleCard("Advance & Peshgi Khata", "Udhaar aur cash peshgi", Icons.Default.AccountBalanceWallet, BrandOrange, onNavigateAdvance)

            if (isAdmin) {
                ModuleCard("Salary Slips & Payroll", "Mahana tankhwah + PDF", Icons.Default.ReceiptLong, Color(0xFF7C3AED), onNavigateSalary)
                ModuleCard("Admin Settings", "WhatsApp + password change", Icons.Default.Tune, Color(0xFF4B5563), onNavigateSettings)
            }

            if (!isAdmin) {
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = BrandOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("Moderator: Salary aur Settings Admin ke paas hain.",
                            fontSize = 12.sp, color = Color(0xFFB45309))
                    }
                }
            }

            CopyrightFooter()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ModuleCard(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextGray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}
