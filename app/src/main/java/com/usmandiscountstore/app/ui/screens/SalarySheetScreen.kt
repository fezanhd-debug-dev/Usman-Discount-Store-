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
import com.usmandiscountstore.app.data.local.entity.BonusEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.CsvExporter
import com.usmandiscountstore.app.util.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySheetScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.get(context)
    val staffDao = db.staffDao()
    val attendanceDao = db.attendanceDao()
    val advanceDao = db.advanceDao()
    val bonusDao = db.bonusDao()

    val prefs = SecurityPreferences(context)
    val adminName = prefs.getUserName().ifEmpty { "Admin" }

    var staffList by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var rows by remember { mutableStateOf<List<CsvExporter.SalaryRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    var selectedMonth by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
    }
    var monthExpanded by remember { mutableStateOf(false) }
    var showBonusDialog by remember { mutableStateOf<StaffEntity?>(null) }

    val months = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        repeat(12) {
            list.add(SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    suspend fun loadRows() {
        isLoading = true
        val result = mutableListOf<CsvExporter.SalaryRow>()
        for (s in staffList) {
            val atts = attendanceDao.getStaffMonthly(s.id, "$selectedMonth%")
            val present = atts.count { it.status == "PRESENT" }
            val half = atts.count { it.status == "HALF_DAY" }
            val leave = atts.count { it.status == "LEAVE" }
            val absent = atts.count { it.status == "ABSENT" }

            val gross = present * s.dailyWage + half * s.dailyWage * 0.5
            val advance = advanceDao.totalForStaff(s.id)
            val bonus = bonusDao.totalForStaffMonth(s.id, selectedMonth)
            val net = (gross + bonus - advance).coerceAtLeast(0.0)

            result.add(CsvExporter.SalaryRow(
                staff = s, present = present, half = half,
                leave = leave, absent = absent,
                gross = gross, advance = advance,
                bonus = bonus, net = net
            ))
        }
        rows = result
        isLoading = false
    }

    LaunchedEffect(Unit) {
        staffDao.getAllActive().collect { list ->
            staffList = list
            loadRows()
        }
    }

    LaunchedEffect(selectedMonth) { loadRows() }

    // Totals
    val totalGross = rows.sumOf { it.gross }
    val totalAdv = rows.sumOf { it.advance }
    val totalBonus = rows.sumOf { it.bonus }
    val totalNet = rows.sumOf { it.net }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Salary Sheet", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("All staff monthly salary", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    CsvExporter.exportSalarySheet(context, selectedMonth, rows)
                                }
                                CsvExporter.shareCsv(context, file, "Salary Sheet $selectedMonth")
                                statusMsg = "✅ CSV ready — share karo"
                            } catch (e: Exception) {
                                statusMsg = "❌ ${e.message}"
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, "Export CSV", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = BackgroundLight
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {

            Spacer(Modifier.height(8.dp))

            // Month selector
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

            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(12.dp))

            // Total card
            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreen),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("TOTAL PAYROLL — $selectedMonth",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Rs. ${totalNet.toInt()}",
                        color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Gross: Rs. ${totalGross.toInt()}  |  Bonus: Rs. ${totalBonus.toInt()}  |  Advance: Rs. ${totalAdv.toInt()}",
                        color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("STAFF WISE", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            if (rows.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, null, tint = TextGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Koi staff nahi", color = TextGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = { it.staff.id }) { row ->
                        SalaryRowCard(row = row, onBonus = { showBonusDialog = row.staff })
                    }
                    item { CopyrightFooter() }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    // Bonus dialog
    showBonusDialog?.let { staff ->
        AddBonusDialog(
            staff = staff,
            monthYear = selectedMonth,
            onDismiss = { showBonusDialog = null },
            onSave = { amount, reason ->
                scope.launch {
                    bonusDao.insert(
                        BonusEntity(
                            staffId = staff.id,
                            staffName = staff.name,
                            monthYear = selectedMonth,
                            amount = amount,
                            reason = reason,
                            addedBy = adminName
                        )
                    )
                    showBonusDialog = null
                    loadRows()
                    statusMsg = "✅ Bonus added for ${staff.name}"
                }
            }
        )
    }
}

@Composable
private fun SalaryRowCard(row: CsvExporter.SalaryRow, onBonus: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandGreenLight),
                    contentAlignment = Alignment.Center) {
                    Text(row.staff.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.staff.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Rs. ${row.staff.dailyWage.toInt()}/day",
                        fontSize = 11.sp, color = TextGray)
                }
                IconButton(onClick = onBonus) {
                    Icon(Icons.Default.AddCircle, "Add Bonus", tint = BrandOrange, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniStat("Hazir", "${row.present}", BrandGreen)
                MiniStat("Half", "${row.half}", Color(0xFF7C3AED))
                MiniStat("Chutti", "${row.leave}", BrandOrange)
                MiniStat("Gair", "${row.absent}", Color(0xFFDC2626))
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    LineItem("Present × Wage", "Rs. ${(row.present * row.staff.dailyWage).toInt()}")
                    LineItem("Half × Wage/2", "Rs. ${(row.half * row.staff.dailyWage * 0.5).toInt()}")
                    LineItem("Bonus", "+ Rs. ${row.bonus.toInt()}", BrandOrange)
                    LineItem("Advance", "- Rs. ${row.advance.toInt()}", Color(0xFFDC2626))
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider(color = Color(0xFFF3F4F6))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Net Payable", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("Rs. ${row.net.toInt()}", fontWeight = FontWeight.Bold,
                    color = BrandGreen, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(label, fontSize = 9.sp, color = TextGray)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun LineItem(label: String, value: String, color: Color = TextDark) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = TextGray)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBonusDialog(
    staff: StaffEntity,
    monthYear: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Overtime") }
    var reasonExpanded by remember { mutableStateOf(false) }
    val reasons = listOf("Overtime", "Eid Bonus", "Festival Bonus", "Performance", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bonus — ${staff.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Month: $monthYear", fontSize = 12.sp, color = TextGray)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = reasonExpanded,
                    onExpandedChange = { reasonExpanded = !reasonExpanded }
                ) {
                    OutlinedTextField(
                        value = reason, onValueChange = {}, readOnly = true,
                        label = { Text("Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reasonExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                        reasons.forEach { r ->
                            DropdownMenuItem(text = { Text(r) },
                                onClick = { reason = r; reasonExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onSave(amt, reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
