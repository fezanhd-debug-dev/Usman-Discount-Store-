package com.usmandiscountstore.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import com.usmandiscountstore.app.util.FaceEmbeddingHelper
import com.usmandiscountstore.app.util.Lang
import com.usmandiscountstore.app.util.LocationHelper
import com.usmandiscountstore.app.util.SecurityPreferences
import com.usmandiscountstore.app.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var cameraForStaff by remember { mutableStateOf<StaffEntity?>(null) }
    var confirmAllPresent by remember { mutableStateOf(false) }
    var hasLocationPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> hasLocationPerm = r[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    LaunchedEffect(Unit) {
        if (!hasLocationPerm) permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        staffRepo.getAllActive().collectLatest { staffList = it }
        attendanceDao.getByDate(today).collectLatest { todayAttendance = it }
    }

    fun getStatusFor(staffId: Long) = todayAttendance.firstOrNull { it.staffId == staffId }

    fun markOther(staff: StaffEntity, status: String) {
        scope.launch {
            isChecking = true; statusMsg = null
            val existing = attendanceDao.getByStaffAndDate(staff.id, today)
            val entity = existing?.copy(status = status, markedBy = markerName)
                ?: AttendanceEntity(staffId = staff.id, staffName = staff.name, date = today,
                    status = status, markedBy = markerName)
            attendanceDao.insert(entity)
            statusMsg = "✅ ${staff.name} — ${status.lowercase().replace("_", " ")}"
            isChecking = false
        }
    }

    fun markAllPresent() {
        scope.launch {
            isChecking = true; statusMsg = null
            var count = 0
            for (s in staffList) {
                val existing = attendanceDao.getByStaffAndDate(s.id, today)
                if (existing == null || existing.status != "PRESENT") {
                    val entity = existing?.copy(status = "PRESENT", checkInTime = todayTime, markedBy = markerName)
                        ?: AttendanceEntity(staffId = s.id, staffName = s.name, date = today,
                            status = "PRESENT", checkInTime = todayTime, markedBy = markerName)
                    attendanceDao.insert(entity); count++
                }
            }
            statusMsg = "✅ $count ${Lang.t("marked_count")}"
            isChecking = false
        }
    }

    suspend fun processPunch(staff: StaffEntity, selfiePath: String) {
        if (!hasLocationPerm) { statusMsg = "❌ ${Lang.t("perm_error")}"; return }
        val loc = LocationHelper.getCurrentLocation(context)
        if (loc == null) { statusMsg = "❌ ${Lang.t("gps_error")}"; return }
        val settings = settingsDao.get()
        val storeLat = settings?.latitude ?: 29.6974
        val storeLon = settings?.longitude ?: 72.5518
        val radius = settings?.geofenceRadiusMeters ?: 30f
        val dist = LocationHelper.distanceTo(loc.latitude, loc.longitude, storeLat, storeLon)
        if (dist > radius) {
            statusMsg = "❌ ${dist.toInt()}m away. ${radius.toInt()}m radius."
            return
        }
        val storedEmb = FaceEmbeddingHelper.stringToEmbed(staff.faceEmbedding)
        if (storedEmb == null) { statusMsg = "❌ ${Lang.t("face_required")}"; return }
        val bmp = BitmapFactory.decodeFile(selfiePath)
        val liveEmb = withContext(Dispatchers.Default) { FaceEmbeddingHelper.getEmbedding(bmp) }
        if (liveEmb == null) { statusMsg = "❌ Face detect failed"; return }
        val similarity = FaceEmbeddingHelper.cosineSimilarity(storedEmb, liveEmb)
        if (similarity < 0.75f) {
            statusMsg = "❌ ${Lang.t("face_match_fail")} (${(similarity * 100).toInt()}%)"
            File(selfiePath).delete(); return
        }
        val existing = attendanceDao.getByStaffAndDate(staff.id, today)
        val entity = existing?.copy(status = "PRESENT", checkInTime = todayTime,
            selfiePath = selfiePath, markedBy = markerName)
            ?: AttendanceEntity(staffId = staff.id, staffName = staff.name, date = today,
                status = "PRESENT", checkInTime = todayTime, selfiePath = selfiePath, markedBy = markerName)
        attendanceDao.insert(entity)
        statusMsg = "✅ ${staff.name} (${(similarity * 100).toInt()}%)"
        if (settings?.alertEnabled == true && settings.adminWhatsapp.isNotBlank()) {
            WhatsAppHelper.sendAlert(context, settings.adminWhatsapp,
                "🏪 Usman Discount Store\n\n📋 Hazri Verified\n👤 ${staff.name}\n" +
                        "✅ Face: ${(similarity * 100).toInt()}%\n⏰ $todayTime\n📅 $today\n✍️ $markerName")
        }
    }

    cameraForStaff?.let { staff ->
        CameraScreen(title = "${Lang.t("face_verify")} — ${staff.name}",
            onPhotoCaptured = { file ->
                cameraForStaff = null
                scope.launch {
                    isChecking = true; statusMsg = null
                    try { processPunch(staff, file.absolutePath) }
                    catch (e: Exception) { statusMsg = "❌ ${e.message}" }
                    finally { isChecking = false }
                }
            },
            onBack = { cameraForStaff = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Lang.t("hazri"), fontWeight = FontWeight.Bold, color = Color.White)
                        Text(today, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Lang.t("back"), tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { confirmAllPresent = true }) {
                        Icon(Icons.Default.DoneAll, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(Lang.t("all_present"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LanguagePickerInTopBar(tint = Color.White)
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
                        containerColor = if (it.startsWith("✅")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(it, Modifier.padding(12.dp), fontSize = 13.sp,
                        color = if (it.startsWith("✅")) Color(0xFF166534) else Color(0xFF991B1B))
                }
            }
            if (staffList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(Lang.t("no_staff_attendance"), color = TextGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(staffList, key = { it.id }) { staff ->
                        AttendanceCard(staff, getStatusFor(staff.id),
                            onMarkPresent = { cameraForStaff = staff },
                            onMarkOther = { status -> markOther(staff, status) })
                    }
                    item { CopyrightFooter() }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (confirmAllPresent) {
        AlertDialog(
            onDismissRequest = { confirmAllPresent = false },
            title = { Text(Lang.t("confirm_all_present"), fontWeight = FontWeight.Bold) },
            text = { Text(Lang.t("confirm_all_msg")) },
            confirmButton = {
                Button(onClick = { markAllPresent(); confirmAllPresent = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)) {
                    Text(Lang.t("yes"))
                }
            },
            dismissButton = { TextButton(onClick = { confirmAllPresent = false }) { Text(Lang.t("cancel")) } }
        )
    }
}

@Composable
private fun AttendanceCard(
    staff: StaffEntity, attendance: AttendanceEntity?,
    onMarkPresent: () -> Unit, onMarkOther: (String) -> Unit
) {
    val (color, label) = when (attendance?.status) {
        "PRESENT" -> BrandGreen to Lang.t("present")
        "HALF_DAY" -> Color(0xFF7C3AED) to Lang.t("half_day")
        "LEAVE" -> BrandOrange to Lang.t("leave")
        "ABSENT" -> Color(0xFFDC2626) to Lang.t("absent")
        null -> TextGray to Lang.t("not_marked")
        else -> TextGray to attendance.status
    }

    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text(staff.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(staff.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(staff.designation.ifEmpty { staff.role }, fontSize = 11.sp, color = TextGray)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color,
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
                    Button(onClick = onMarkPresent, modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Icon(Icons.Default.Face, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(Lang.t("present"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (attendance?.status != "HALF_DAY") {
                    Button(onClick = { onMarkOther("HALF_DAY") }, modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Text(Lang.t("half_day"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (attendance?.status != "LEAVE") {
                    OutlinedButton(onClick = { onMarkOther("LEAVE") }, modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Text(Lang.t("leave"), fontSize = 10.sp, color = BrandOrange)
                    }
                }
                if (attendance?.status != "ABSENT") {
                    OutlinedButton(onClick = { onMarkOther("ABSENT") }, modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Text(Lang.t("absent"), fontSize = 10.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}
