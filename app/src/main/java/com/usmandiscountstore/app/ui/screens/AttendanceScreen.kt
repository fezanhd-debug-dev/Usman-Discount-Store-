package com.usmandiscountstore.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.LocationHelper
import com.usmandiscountstore.app.util.SecurityPreferences
import com.usmandiscountstore.app.util.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.get(context)
    val staffRepo = db.staffDao()
    val attendanceDao = db.attendanceDao()
    val settingsDao = db.storeSettingsDao()

    val prefs = SecurityPreferences(context)
    val markerName = prefs.getUserName().ifEmpty { "Admin" }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    var staffList by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var todayAttendance by remember { mutableStateOf<List<AttendanceEntity>>(emptyList()) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    // Which staff is being camera-captured for PRESENT
    var cameraForStaff by remember { mutableStateOf<StaffEntity?>(null) }

    var hasLocationPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> hasLocationPerm = r[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    LaunchedEffect(Unit) {
        if (!hasLocationPerm) permLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        staffRepo.getAllActive().collectLatest { staffList = it }
        attendanceDao.getByDate(today).collectLatest { todayAttendance = it }
    }

    fun getStatusFor(staffId: Long) = todayAttendance.firstOrNull { it.staffId == staffId }

    fun markAttendance(staff: StaffEntity, status: String, selfiePath: String = "") {
        scope.launch {
            isChecking = true
            statusMsg = null

            if (status == "PRESENT" || status == "HALF_DAY") {
                if (!hasLocationPerm) {
                    statusMsg = "❌ Location permission chahiye"
                    isChecking = false; return@launch
                }
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc == null) {
                    statusMsg = "❌ GPS on karo"
                    isChecking = false; return@launch
                }
                val s = settingsDao.get()
                val storeLat = s?.latitude ?: 29.6974
                val storeLon = s?.longitude ?: 72.5518
                val radius = s?.geofenceRadiusMeters ?: 30f
                val dist = LocationHelper.distanceTo(loc.latitude, loc.longitude, storeLat, storeLon)
                if (dist > radius) {
                    statusMsg = "❌ Aap dukan se ${dist.toInt()}m door hain"
                    isChecking = false; return@launch
                }
            }

            val existing = attendanceDao.getByStaffAndDate(staff.id, today)
            val entity = existing?.copy(
                status = status,
                checkInTime = if (status == "PRESENT" || status == "HALF_DAY") todayTime else existing.checkInTime,
                selfiePath = selfiePath.ifEmpty { existing.selfiePath },
                markedBy = markerName
            ) ?: AttendanceEntity(
                staffId = staff.id,
                staffName = staff.name,
                date = today,
                status = status,
                checkInTime = if (status == "PRESENT" || status == "HALF_DAY") todayTime else "",
                selfiePath = selfiePath,
                markedBy = markerName
            )
            attendanceDao.insert(entity)
            statusMsg = "✅ ${staff.name} ki hazri darj"

            val s = settingsDao.get()
            if (s?.alertEnabled == true && s.adminWhatsapp.isNotBlank()) {
                val statusText = when (status) {
                    "PRESENT" -> "Hazir"; "LEAVE" -> "Chutti"
                    "ABSENT" -> "Gair Hazir"; "HALF_DAY" -> "Half Day"
                    else -> status
                }
                val msg = "🏪 Usman Discount Store\n\n📋 Hazri Alert\n👤 ${staff.name}\n✅ $statusText\n⏰ $todayTime\n📅 $today\n✍️ $markerName\n\n© Mr.DHooM 4K"
                WhatsAppHelper.sendAlert(context, s.adminWhatsapp, msg)
            }
            isChecking = false
        }
    }

    // If camera is open for a staff — show camera screen
    cameraForStaff?.let { staff ->
        CameraScreen(
            title = "Hazri — ${staff.name}",
            onPhotoCaptured = { file ->
                val path = file.absolutePath
                cameraForStaff = null
                markAttendance(staff, "PRESENT", path)
            },
            onBack = { cameraForStaff = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hazri", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(today, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = BackgroundLight
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            if (isChecking) LinearProgressIndicator(Modifier.fillMaxWidth())
            statusMsg?.let {
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("✅")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(it, Modifier.padding(12.dp), fontSize = 13.sp,
                        color = if (it.startsWith("✅")) Color(0xFF166534) else Color(0xFF991B1B))
                }
            }

            if (staffList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Koi staff nahi hai", color = TextGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(staffList, key = { it.id }) { staff ->
                        val att = getStatusFor(staff.id)
                        AttendanceCard(
                            staff = staff,
                            attendance = att,
                            onMarkPresent = { cameraForStaff = staff },   // camera open
                            onMarkOther = { status -> markAttendance(staff, status) }
                        )
                    }
                    item { CopyrightFooter() }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(
    staff: StaffEntity,
    attendance: AttendanceEntity?,
    onMarkPresent: () -> Unit,
    onMarkOther: (String) -> Unit
) {
    val (statusColor, statusText) = when (attendance?.status) {
        "PRESENT" -> BrandGreen to "Hazir"
        "HALF_DAY" -> Color(0xFF7C3AED) to "Half Day"
        "LEAVE" -> BrandOrange to "Chutti"
        "ABSENT" -> Color(0xFFDC2626) to "Gair"
        null -> TextGray to "Nahi Lagi"
        else -> TextGray to attendance.status
    }

    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text(staff.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(staff.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(staff.designation.ifEmpty { staff.role }, fontSize = 11.sp, color = TextGray)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            if (attendance?.checkInTime?.isNotEmpty() == true) {
                Spacer(Modifier.height(6.dp))
                Text("⏰ ${attendance.checkInTime} • ✍️ ${attendance.markedBy}",
                    fontSize = 11.sp, color = TextGray)
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (attendance?.status != "PRESENT") {
                    Button(
                        onClick = onMarkPresent,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Hazir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (attendance?.status != "HALF_DAY") {
                    Button(
                        onClick = { onMarkOther("HALF_DAY") },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) { Text("Half", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                if (attendance?.status != "LEAVE") {
                    OutlinedButton(
                        onClick = { onMarkOther("LEAVE") },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) { Text("Chutti", fontSize = 10.sp, color = BrandOrange) }
                }
                if (attendance?.status != "ABSENT") {
                    OutlinedButton(
                        onClick = { onMarkOther("ABSENT") },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) { Text("Gair", fontSize = 10.sp, color = Color(0xFFDC2626)) }
                }
            }
        }
    }
}
