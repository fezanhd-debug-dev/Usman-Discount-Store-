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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.util.SuperAdminHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(onLogout: () -> Unit) {

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "License",
        "Devices",
        "Security",
        "Info"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SUPER ADMIN PANEL", fontWeight = FontWeight.Black,
                            color = Color.White, fontSize = 15.sp, letterSpacing = 2.sp)
                        Text("Mr.DHooM 4K — Restricted", fontSize = 10.sp,
                            color = Color(0xFFFBBF24), letterSpacing = 1.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFFF8F9FF)
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Warning banner if default password
            if (SuperAdminHelper.isDefaultPassword(context)) {
                Surface(color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFB45309))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Default password active — please change it in Security tab.",
                            fontSize = 12.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Content
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> LicenseTab()
                    1 -> DevicesTab()
                    2 -> SecurityTab()
                    3 -> InfoTab()
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun LicenseTab() {
    SectionHeader(
        icon = Icons.Default.VerifiedUser,
        title = "LICENSE MANAGEMENT",
        subtitle = "App activation & expiry"
    )

    // Placeholder card
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF2563EB))
                Spacer(Modifier.width(10.dp))
                Text("Coming in Next Phase",
                    fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Yahan license generate / activate / deactivate ka feature aayega. " +
                        "Abhi ke liye panel tayyar hai — agli baar license code add karenge.",
                fontSize = 12.sp, color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun DevicesTab() {
    SectionHeader(
        icon = Icons.Default.Devices,
        title = "REGISTERED DEVICES",
        subtitle = "Devices linked to this license"
    )

    EmptyCard("No devices registered yet. License system ke saath aayega.")
}

@Composable
private fun SecurityTab() {
    SectionHeader(
        icon = Icons.Default.Security,
        title = "SUPER ADMIN SECURITY",
        subtitle = "Master password change"
    )

    val context = LocalContext.current
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Change Master Password",
                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = newPwd, onValueChange = { newPwd = it; msg = null },
                label = { Text("New Password") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPwd, onValueChange = { confirmPwd = it; msg = null },
                label = { Text("Confirm Password") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (msg != null) {
                Text(msg!!,
                    color = if (msg!!.startsWith("✅")) Color(0xFF16A34A) else Color(0xFFDC2626),
                    fontSize = 12.sp)
            }

            Button(
                onClick = {
                    when {
                        newPwd.length < 8 -> msg = "❌ Password must be at least 8 characters"
                        newPwd != confirmPwd -> msg = "❌ Passwords do not match"
                        else -> {
                            SuperAdminHelper.setPassword(context, newPwd)
                            newPwd = ""; confirmPwd = ""
                            msg = "✅ Password updated successfully"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Update Password", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoTab() {
    SectionHeader(
        icon = Icons.Default.Info,
        title = "SYSTEM INFO",
        subtitle = "Build & runtime details"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow("App", "Usman Discount Store")
            InfoRow("Version", "1.0.0")
            InfoRow("Build", "Debug")
            InfoRow("Owner", "Muhammad Usman Nawaz")
            InfoRow("Developer", "Mr.DHooM 4K")
            InfoRow("Store", "Vehari Road, Old Hasilpur")
            Divider(Modifier.padding(vertical = 6.dp))
            Text("© 2026 Mr.DHooM 4K — All Rights Reserved",
                fontSize = 10.sp, color = Color(0xFF9CA3AF))
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.08f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF0F172A), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1F2937),
                letterSpacing = 1.sp)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = Color(0xFF6B7280), modifier = Modifier.width(90.dp))
        Text(value, fontSize = 12.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
    }
}
