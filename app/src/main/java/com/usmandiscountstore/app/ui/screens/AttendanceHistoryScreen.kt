package com.usmandiscountstore.app.ui.screens

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
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    staffId: Long = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.get(context)
    val staffDao = db.staffDao()
    val attendanceDao = db.attendanceDao()

    var staffList by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var selectedStaff by remember { mutableStateOf<StaffEntity?>(null) }
    var staffExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var selectedMonth by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
    }
    var historyList by remember { mutableStateOf<List<AttendanceEntity>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf<AttendanceEntity?>(null) }

    val months = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        repeat(12) {
            list.add(SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    LaunchedEffect(Unit) {
        staffDao.getAllActive().collectLatest { list ->
            staffList = list
            if (selectedStaff == null && list.isNotEmpty()) {
                selectedStaff = if (staffId != 0L) list.find { it.id == staffId } ?: list.first() else list.first()
            }
        }
    }

    LaunchedEffect(selectedStaff, selectedMonth) {
        selectedStaff?.let { s ->
            historyList = attendanceDao.getStaffMonthly(s.id, "$selectedMonth%")
        }
    }

    // Summary calculations
    val presentCount = historyList.count { it.status == "PRESENT" }
    val halfCount = historyList.count { it.status == "HALF_DAY" }
    val leaveCount = historyList.count { it.status == "LEAVE" }
    val absentCount = historyList.count { it.status == "ABSENT" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance History", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Monthly report", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {

            // ===== STAFF SELECT =====
            ExposedDropdownMenuBox(
                expanded = staffExpanded,
                onExpandedChange = { staffExpanded = !staffExpanded }
            ) {
                OutlinedTextField(
                    value = selectedStaff?.name ?: "Staff select karo",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Staff") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = BrandGreen) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(staffExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = staffExpanded, onDismissRequest = { staffExpanded = false }) {
                    staffList.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = { selectedStaff = s; staffExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ===== MONTH SELECT =====
            ExposedDropdownMenuBox(
                expanded = monthExpanded,
                onExpandedChange = { monthExpanded = !monthExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Month") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = BrandGreen) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                    months.forEach { m ->
                        DropdownMenuItem(text = { Text(m) }, onClick = { selectedMonth = m; monthExpanded = false })
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== SUMMARY CARDS =====
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("MONTHLY SUMMARY", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("Hazir", "$presentCount", BrandGreen, Modifier.weight(1f))
                        SummaryCard("Half", "$halfCount", Color(0xFF7C3AED), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("Chutti", "$leaveCount", BrandOrange, Modifier.weight(1f))
                        SummaryCard("Gair", "$absentCount", Color(0xFFDC2626), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("DAILY RECORDS", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            if (historyList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, tint = TextGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Is mahine koi hazri nahi", color = TextGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList.sortedByDescending { it.date }, key = { it.id }) { att ->
                        HistoryRow(att = att, onEdit = { showEditDialog = att })
                    }
                    item { CopyrightFooter() }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    // ===== EDIT DIALOG =====
    showEditDialog?.let { att ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Change Status", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Date: ${att.date}", fontWeight = FontWeight.Bold)
                    Text("Current: ${att.status}", fontSize = 12.sp, color = TextGray)
                    Spacer(Modifier.height(12.dp))
                    Text("New Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    listOf("PRESENT" to "Hazir", "HALF_DAY" to "Half Day",
                           "LEAVE" to "Chutti", "ABSENT" to "Gair Hazir").forEach { (code, label) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = att.status == code,
                                onClick = {
                                    scope.launch {
                                        attendanceDao.insert(att.copy(status = code))
                                        showEditDialog = null
                                        selectedStaff?.let { s ->
                                            historyList = attendanceDao.getStaffMonthly(s.id, "$selectedMonth%")
                                        }
                                    }
                                }
                            )
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = TextGray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun HistoryRow(att: AttendanceEntity, onEdit: () -> Unit) {
    val (color, label) = when (att.status) {
        "PRESENT" -> BrandGreen to "Hazir"
        "HALF_DAY" -> Color(0xFF7C3AED) to "Half Day"
        "LEAVE" -> BrandOrange to "Chutti"
        "ABSENT" -> Color(0xFFDC2626) to "Gair"
        else -> TextGray to att.status
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    att.date.takeLast(2),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(att.date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                if (att.checkInTime.isNotEmpty())
                    Text("⏰ ${att.checkInTime} • ${att.markedBy}", fontSize = 10.sp, color = TextGray)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = BrandGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}
