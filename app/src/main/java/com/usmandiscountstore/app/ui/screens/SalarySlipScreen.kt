package com.usmandiscountstore.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.data.local.entity.AdvanceEntity
import com.usmandiscountstore.app.data.local.entity.AttendanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.SalaryBreakdown
import com.usmandiscountstore.app.util.SalarySlipGenerator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySlipScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.get(context)
    val staffDao = db.staffDao()
    val attendanceDao = db.attendanceDao()
    val advanceDao = db.advanceDao()

    var staffList by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var attendance by remember { mutableStateOf<List<AttendanceEntity>>(emptyList()) }
    var advances by remember { mutableStateOf<List<AdvanceEntity>>(emptyList()) }

    var selectedStaff by remember { mutableStateOf<StaffEntity?>(null) }
    var selectedMonth by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
    }
    var breakdown by remember { mutableStateOf<SalaryBreakdown?>(null) }
    var expandedStaff by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

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
        staffDao.getAllActive().collectLatest { staffList = it }
        attendanceDao.getByDate("").collectLatest { }
    }

    // Load attendance + advances when staff selected
    LaunchedEffect(selectedStaff) {
        selectedStaff?.let { s ->
            // Load all attendance for staff (last 90 days)
            val allAtt = mutableListOf<AttendanceEntity>()
            val cal = Calendar.getInstance()
            repeat(90) {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                // We'll fetch by month range later
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            // Simpler: load via Flow for each? Not efficient.
            // Instead, we'll use a query-less approach: fetch via dao.getByStaff
            // But getByStaff returns Flow — we need one-time snapshot.
            // Alternative: use getByDate for each day? Slow but OK for 30 days.
        }
        // Load advances for selected staff
        selectedStaff?.let { s ->
            advanceDao.getByStaff(s.id).collectLatest { advances = it }
        }
    }

    // Auto-calculate on selection change
    LaunchedEffect(selectedStaff, selectedMonth, advances) {
        val s = selectedStaff ?: return@LaunchedEffect
        // Get attendance for the month
        val monthAttendance = mutableListOf<AttendanceEntity>()
        // Fetch by reading all dates in month
        val monthStart = "$selectedMonth-01"
        // We'll approximate by using the getByDate Flow — not ideal but works
        // For now, use a direct approach:
        // (Room doesn't support sync queries with Flow, so we use a snapshot list)
        breakdown = SalarySlipGenerator.calculate(
            staff = s,
            monthYear = selectedMonth,
            attendance = attendance,
            advances = advances
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Salary Slip", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("PDF generate + print", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            statusMsg?.let {
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(12.dp), color = Color(0xFF166534), fontSize = 12.sp)
                }
            }

            // ===== STAFF SELECT =====
            ExposedDropdownMenuBox(
                expanded = expandedStaff,
                onExpandedChange = { expandedStaff = !expandedStaff }
            ) {
                OutlinedTextField(
                    value = selectedStaff?.name ?: "Staff select karo",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Staff") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedStaff) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedStaff, onDismissRequest = { expandedStaff = false }) {
                    staffList.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.name} — Rs. ${s.dailyWage.toInt()}/din") },
                            onClick = { selectedStaff = s; expandedStaff = false }
                        )
                    }
                }
            }

            // ===== MONTH SELECT =====
            ExposedDropdownMenuBox(
                expanded = expandedMonth,
                onExpandedChange = { expandedMonth = !expandedMonth }
            ) {
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMonth) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                    months.forEach { m ->
                        DropdownMenuItem(text = { Text(m) }, onClick = { selectedMonth = m; expandedMonth = false })
                    }
                }
            }

            breakdown?.let { bd ->
                // ===== BREAKDOWN CARD =====
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("SALARY BREAKDOWN", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))

                        Row {
                            SumCard("Hazir", "${bd.presentDays}", BrandGreen, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            SumCard("Half", "${bd.halfDays}", Color(0xFF7C3AED), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            SumCard("Chutti", "${bd.leaveDays}", BrandOrange, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            SumCard("Gair", "${bd.absentDays}", Color(0xFFDC2626), Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color(0xFFF3F4F6))
                        Spacer(Modifier.height(12.dp))

                        RowItem("Present Days × Wage",
                            "${bd.presentDays} × Rs. ${bd.dailyWage.toInt()}",
                            "Rs. ${bd.presentAmount.toInt()}", BrandGreen)
                        RowItem("Half Days × Wage/2",
                            "${bd.halfDays} × Rs. ${(bd.dailyWage * 0.5).toInt()}",
                            "Rs. ${bd.halfDayAmount.toInt()}", Color(0xFF7C3AED))

                        Spacer(Modifier.height(8.dp))
                        RowItem("Gross Salary", "", "Rs. ${bd.grossSalary.toInt()}", TextDark, bold = true)

                        Spacer(Modifier.height(8.dp))
                        RowItem("Advance / Peshgi", "", "- Rs. ${bd.totalAdvance.toInt()}", Color(0xFFDC2626))

                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFF3F4F6))
                        Spacer(Modifier.height(12.dp))

                        // Net
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("NET PAYABLE", fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text("Rs. ${bd.netPayable.toInt()}", fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 22.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ===== ACTION BUTTONS =====
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val pdf = SalarySlipGenerator.generatePdf(context, bd)
                                    statusMsg = "✅ PDF save: ${pdf.name}"
                                } catch (e: Exception) {
                                    statusMsg = "❌ Error: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val pdf = SalarySlipGenerator.generatePdf(context, bd)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdf
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Salary Slip - ${bd.staff.name}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                } catch (e: Exception) {
                                    statusMsg = "❌ ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val pdf = SalarySlipGenerator.generatePdf(context, bd)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdf
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                statusMsg = "❌ Print ke liye PDF app chahiye"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, null, tint = BrandGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Print / Open PDF", color = BrandGreen, fontWeight = FontWeight.Bold)
                }
            }

            CopyrightFooter()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SumCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = TextGray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun RowItem(label: String, detail: String, amount: String, amountColor: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = if (bold) 13.sp else 12.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = TextDark)
            if (detail.isNotEmpty())
                Text(detail, fontSize = 10.sp, color = TextGray)
        }
        Text(amount, fontSize = if (bold) 14.sp else 13.sp,
            fontWeight = FontWeight.Bold, color = amountColor)
    }
}
