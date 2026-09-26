package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.LeaveRequestEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.SecurityPreferences
import com.usmandiscountstore.app.util.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    isAdminView: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.get(context)
    val staffDao = db.staffDao()
    val leaveDao = db.leaveDao()
    val attendanceDao = db.attendanceDao()
    val settingsDao = db.storeSettingsDao()

    val prefs = SecurityPreferences(context)
    val currentUserName = prefs.getUserName().ifEmpty { "Admin" }

    var staffList by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var allRequests by remember { mutableStateOf<List<LeaveRequestEntity>>(emptyList()) }
    var pendingCount by remember { mutableStateOf(0) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var showNewRequest by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableIntStateOf(0) }   // 0=Pending, 1=Approved, 2=Rejected

    LaunchedEffect(Unit) {
        staffDao.getAllActive().collectLatest { staffList = it }
        leaveDao.getAll().collectLatest { allRequests = it }
        leaveDao.getPendingCount().collectLatest { pendingCount = it }
    }

    val filtered = when (tabIndex) {
        0 -> allRequests.filter { it.status == "PENDING" }
        1 -> allRequests.filter { it.status == "APPROVED" }
        else -> allRequests.filter { it.status == "REJECTED" }
    }

    // Approve handler: mark all days as LEAVE in attendance
    fun approve(req: LeaveRequestEntity) {
        scope.launch {
            try {
                leaveDao.updateStatus(req.id, "APPROVED", currentUserName,
                    System.currentTimeMillis(), req.adminNote)

                // Loop dates from fromDate to toDate
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                var date = sdf.parse(req.fromDate)
                val end = sdf.parse(req.toDate)
                var count = 0
                while (date != null && !date.after(end)) {
                    val dStr = sdf.format(date)
                    val existing = attendanceDao.getByStaffAndDate(req.staffId, dStr)
                    val entity = existing?.copy(status = "LEAVE",
                        note = "Leave: ${req.reason}", markedBy = currentUserName)
                        ?: AttendanceEntity(
                            staffId = req.staffId, staffName = req.staffName,
                            date = dStr, status = "LEAVE",
                            note = "Leave: ${req.reason}", markedBy = currentUserName
                        )
                    attendanceDao.insert(entity)
                    count++
                    val cal = Calendar.getInstance().apply { time = date }
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    date = cal.time
                }
                statusMsg = "✅ Approved — $count din Chutti mark ki gayi"

                val settings = settingsDao.get()
                if (settings?.alertEnabled == true && settings.adminWhatsapp.isNotBlank()) {
                    val msg = "🏪 Usman Discount Store\n\n✅ Leave Approved\n👤 ${req.staffName}\n" +
                            "📅 ${req.fromDate} → ${req.toDate}\n" +
                            "👥 By: $currentUserName\n\n© Mr.DHooM 4K"
                    WhatsAppHelper.sendAlert(context, settings.adminWhatsapp, msg)
                }
            } catch (e: Exception) {
                statusMsg = "❌ ${e.message}"
            }
        }
    }

    fun reject(req: LeaveRequestEntity) {
        scope.launch {
            leaveDao.updateStatus(req.id, "REJECTED", currentUserName,
                System.currentTimeMillis(), req.adminNote)
            statusMsg = "❌ Rejected — ${req.staffName}"

            val settings = settingsDao.get()
            if (settings?.alertEnabled == true && settings.adminWhatsapp.isNotBlank()) {
                val msg = "🏪 Usman Discount Store\n\n❌ Leave Rejected\n👤 ${req.staffName}\n" +
                        "📅 ${req.fromDate} → ${req.toDate}\n" +
                        "👥 By: $currentUserName\n\n© Mr.DHooM 4K"
                WhatsAppHelper.sendAlert(context, settings.adminWhatsapp, msg)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Leave Requests", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (pendingCount > 0) "$pendingCount pending" else "All caught up",
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isAdminView) {
                        IconButton(onClick = { showNewRequest = true }) {
                            Icon(Icons.Default.Add, "New", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = BackgroundLight
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {

            Spacer(Modifier.height(10.dp))

            // Tabs
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.White,
                contentColor = BrandGreen
            ) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 },
                    text = { Text("Pending ($pendingCount)") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 },
                    text = { Text("Approved") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 },
                    text = { Text("Rejected") })
            }

            statusMsg?.let {
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("✅")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(10.dp), fontSize = 12.sp,
                        color = if (it.startsWith("✅")) Color(0xFF166534) else Color(0xFF991B1B))
                }
            }

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventAvailable, null, tint = TextGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when (tabIndex) {
                                0 -> "Koi pending request nahi"
                                1 -> "Koi approved request nahi"
                                else -> "Koi rejected request nahi"
                            },
                            color = TextGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { req ->
                        LeaveRequestCard(
                            req = req,
                            isAdminView = isAdminView,
                            onApprove = { approve(req) },
                            onReject = { reject(req) }
                        )
                    }
                    item { CopyrightFooter() }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showNewRequest) {
        NewLeaveDialog(
            staffList = staffList,
            onDismiss = { showNewRequest = false },
            onSave = { staff, from, to, reason ->
                scope.launch {
                    leaveDao.insert(
                        LeaveRequestEntity(
                            staffId = staff.id,
                            staffName = staff.name,
                            fromDate = from,
                            toDate = to,
                            reason = reason
                        )
                    )
                    showNewRequest = false
                    statusMsg = "✅ Request bhej di gayi"
                }
            }
        )
    }
}

@Composable
private fun LeaveRequestCard(
    req: LeaveRequestEntity,
    isAdminView: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val (statusColor, statusText) = when (req.status) {
        "APPROVED" -> BrandGreen to "Approved"
        "REJECTED" -> Color(0xFFDC2626) to "Rejected"
        else -> BrandOrange to "Pending"
    }

    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text(req.staffName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(req.staffName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Requested: ${formatDate(req.requestedAt)}", fontSize = 10.sp, color = TextGray)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("${req.fromDate}  →  ${req.toDate}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            if (req.reason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("📝 ${req.reason}", fontSize = 11.sp, color = TextGray)
            }

            if (req.status == "PENDING" && isAdminView) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            } else if (req.status != "PENDING" && req.reviewedBy.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Reviewed by ${req.reviewedBy}", fontSize = 10.sp, color = TextGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewLeaveDialog(
    staffList: List<StaffEntity>,
    onDismiss: () -> Unit,
    onSave: (StaffEntity, String, String, String) -> Unit
) {
    var selected by remember { mutableStateOf<StaffEntity?>(null) }
    var staffExpanded by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var toDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Leave Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = staffExpanded,
                    onExpandedChange = { staffExpanded = !staffExpanded }
                ) {
                    OutlinedTextField(
                        value = selected?.name ?: "Staff select karo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Staff") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(staffExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = staffExpanded,
                        onDismissRequest = { staffExpanded = false }) {
                        staffList.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { selected = s; staffExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = fromDate, onValueChange = { fromDate = it },
                    label = { Text("From Date (yyyy-MM-dd)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = toDate, onValueChange = { toDate = it },
                    label = { Text("To Date (yyyy-MM-dd)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = reason, onValueChange = { reason = it },
                    label = { Text("Reason") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selected != null && fromDate.isNotBlank() && toDate.isNotBlank()) {
                        onSave(selected!!, fromDate.trim(), toDate.trim(), reason.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd-MMM hh:mm a", Locale.US).format(Date(millis))
