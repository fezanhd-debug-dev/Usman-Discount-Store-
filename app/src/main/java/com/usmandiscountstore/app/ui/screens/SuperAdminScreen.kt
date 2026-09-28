package com.usmandiscountstore.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.usmandiscountstore.app.util.AdminApiHelper
import com.usmandiscountstore.app.util.SuperAdminHelper
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(onLogout: () -> Unit) {

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("License", "Devices", "Security", "Info")

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

            if (SuperAdminHelper.isDefaultPassword(context)) {
                Surface(color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFB45309))
                        Spacer(Modifier.width(8.dp))
                        Text("Default password active — change in Security tab.",
                            fontSize = 12.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                    }
                }
            }

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

            when (selectedTab) {
                0 -> LicenseDashboardTab()
                1 -> DevicesListTab()
                2 -> SecurityTab()
                3 -> InfoTab()
            }
        }
    }
}

// =========================================================
// TAB 1: LICENSE DASHBOARD
// =========================================================
@Composable
private fun LicenseDashboardTab() {
    var totalDevices by remember { mutableIntStateOf(0) }
    var activeDevices by remember { mutableIntStateOf(0) }
    var expiredDevices by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val devices = AdminApiHelper.getAllDevices()
        if (devices != null) {
            totalDevices = devices.length()
            for (i in 0 until devices.length()) {
                val status = devices.getJSONObject(i).optString("status")
                if (status == "active") activeDevices++ else expiredDevices++
            }
        }
        isLoading = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        
        SectionHeader(Icons.Default.VerifiedUser, "LICENSE DASHBOARD", "Overview of registered devices")
        
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("Total", totalDevices.toString(), Color(0xFF2563EB), Modifier.weight(1f))
                DashboardCard("Active", activeDevices.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                DashboardCard("Expired", expiredDevices.toString(), Color(0xFFDC2626), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("License Management", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Devices ko activate, block, add ya remove karne ke liye 'Devices' tab par jayein.",
                        fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// =========================================================
// TAB 2: DEVICES LIST
// =========================================================
@Composable
private fun DevicesListTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var devices by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDevice by remember { mutableStateOf<JSONObject?>(null) }
    var showActivateDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    suspend fun loadDevices() {
        isLoading = true
        val jsonArray = AdminApiHelper.getAllDevices()
        val list = mutableListOf<JSONObject>()
        if (jsonArray != null) {
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i))
            }
        }
        devices = list
        isLoading = false
    }

    LaunchedEffect(Unit) { loadDevices() }

    // ========== ADD DEVICE DIALOG ==========
    if (showAddDialog) {
        var newHardwareId by remember { mutableStateOf("") }
        var newStoreName by remember { mutableStateOf("") }
        var newDuration by remember { mutableStateOf<Int?>(7) }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("➕ Add New Device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Manually naya device add karein.", fontSize = 12.sp, color = Color(0xFF6B7280))
                    
                    OutlinedTextField(
                        value = newHardwareId,
                        onValueChange = { newHardwareId = it },
                        label = { Text("Hardware ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newStoreName,
                        onValueChange = { newStoreName = it },
                        label = { Text("Store Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Duration:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7 to "7 Days", 3 to "3 Mo", 6 to "6 Mo", 12 to "12 Mo").forEach { (m, label) ->
                            FilterChip(
                                selected = newDuration == m,
                                onClick = { newDuration = m },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (newHardwareId.isBlank() || newStoreName.isBlank()) {
                                Toast.makeText(context, "❌ Fields khali hain", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val months = if (newDuration == 7) null else newDuration
                            val success = AdminApiHelper.addDeviceManually(
                                newHardwareId.trim(), newStoreName.trim(), months
                            )
                            if (success) {
                                Toast.makeText(context, "✅ Device added", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                loadDevices()
                            } else {
                                Toast.makeText(context, "❌ Failed (device already exists?)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) { Text("Add", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ========== ACTIVATE DIALOG ==========
    if (showActivateDialog && selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showActivateDialog = false },
            title = { Text("Activate License") },
            text = {
                Column {
                    Text("Device: ${selectedDevice?.optString("store_name")}")
                    Spacer(Modifier.height(16.dp))
                    Text("Select Duration:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(3, 6, 12).forEach { months ->
                        Button(
                            onClick = {
                                scope.launch {
                                    val success = AdminApiHelper.activateDevice(
                                        selectedDevice?.optString("hardware_id") ?: "", months)
                                    if (success) {
                                        Toast.makeText(context, "✅ Activated for $months months", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "❌ Failed to activate", Toast.LENGTH_SHORT).show()
                                    }
                                    showActivateDialog = false
                                    loadDevices()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) { Text("$months Months") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActivateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ========== BLOCK DIALOG ==========
    if (showBlockDialog && selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block Device?") },
            text = { Text("Kya aap waqai is device ko block karna chahte hain? Ye device foran lock ho jayegi.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = AdminApiHelper.blockDevice(selectedDevice?.optString("hardware_id") ?: "")
                            if (success) Toast.makeText(context, "🚫 Device Blocked", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, "❌ Failed", Toast.LENGTH_SHORT).show()
                            showBlockDialog = false
                            loadDevices()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Block", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ========== DELETE DIALOG ==========
    if (showDeleteDialog && selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("🗑️ Remove Device?") },
            text = { 
                Column {
                    Text("Kya aap waqai is device ko server se permanently remove karna chahte hain?")
                    Spacer(Modifier.height(8.dp))
                    Text("Store: ${selectedDevice?.optString("store_name")}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Ye action undo nahi ho sakta!", color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = AdminApiHelper.deleteDevice(selectedDevice?.optString("hardware_id") ?: "")
                            if (success) Toast.makeText(context, "🗑️ Device Removed", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, "❌ Failed to remove", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                            loadDevices()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Remove", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Registered Devices (${devices.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row {
                // ➕ ADD DEVICE BUTTON
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add Device", tint = Color(0xFF16A34A))
                }
                // 🔄 REFRESH BUTTON
                IconButton(onClick = { 
                    scope.launch { loadDevices() }
                }) { Icon(Icons.Default.Refresh, "Refresh", tint = Color(0xFF0F172A)) }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No devices registered yet.", color = Color(0xFF6B7280))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add First Device")
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(devices) { device ->
                    val status = device.optString("status")
                    val licenseType = device.optString("license_type")
                    
                    val expiry = if (device.isNull("license_end") || device.optString("license_end").isEmpty()) {
                        "Trial ends: " + device.optString("trial_end").take(10)
                    } else {
                        "Expires: " + device.optString("license_end").take(10)
                    }
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(device.optString("store_name"), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    modifier = Modifier.weight(1f))
                                Text(
                                    status.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == "active") Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("ID: ${device.optString("hardware_id").take(25)}...", fontSize = 10.sp, color = Color(0xFF6B7280))
                            Text("Type: $licenseType | $expiry", fontSize = 11.sp, color = Color(0xFF374151))
                            Spacer(Modifier.height(10.dp))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        selectedDevice = device
                                        showActivateDialog = true
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Activate", fontSize = 10.sp) }
                                
                                Button(
                                    onClick = {
                                        selectedDevice = device
                                        showBlockDialog = true
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Block", fontSize = 10.sp) }
                                
                                // 🗑️ DELETE BUTTON
                                IconButton(
                                    onClick = {
                                        selectedDevice = device
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Remove",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// TAB 3: SECURITY
// =========================================================
@Composable
private fun SecurityTab() {
    SectionHeader(Icons.Default.Security, "SUPER ADMIN SECURITY", "Master password change")
    val context = LocalContext.current
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Change Master Password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(value = newPwd, onValueChange = { newPwd = it; msg = null },
                    label = { Text("New Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = confirmPwd, onValueChange = { confirmPwd = it; msg = null },
                    label = { Text("Confirm Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (msg != null) {
                    Text(msg!!, color = if (msg!!.startsWith("✅")) Color(0xFF16A34A) else Color(0xFFDC2626), fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        when {
                            newPwd.length < 8 -> msg = "❌ Min 8 characters required"
                            newPwd != confirmPwd -> msg = "❌ Passwords do not match"
                            else -> {
                                SuperAdminHelper.setPassword(context, newPwd)
                                newPwd = ""; confirmPwd = ""
                                msg = "✅ Password updated successfully"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Update Password", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// =========================================================
// TAB 4: INFO
// =========================================================
@Composable
private fun InfoTab() {
    SectionHeader(Icons.Default.Info, "SYSTEM INFO", "Build & runtime details")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("App", "Usman Discount Store")
                InfoRow("Version", "1.0.0")
                InfoRow("Owner", "Muhammad Usman Nawaz")
                InfoRow("Developer", "Mr.DHooM 4K")
                InfoRow("Server", "staffmanagestore.duckdns.org")
                Divider(Modifier.padding(vertical = 6.dp))
                Text("© 2026 Mr.DHooM 4K — All Rights Reserved", fontSize = 10.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}

// =========================================================
// REUSABLE COMPONENTS
// =========================================================
@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0F172A).copy(alpha = 0.08f),
            modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF0F172A), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF6B7280))
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
