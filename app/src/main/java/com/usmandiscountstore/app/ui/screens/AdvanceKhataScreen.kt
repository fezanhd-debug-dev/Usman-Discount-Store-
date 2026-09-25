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
import com.usmandiscountstore.app.data.local.entity.AdvanceEntity
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.SecurityPreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceKhataScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.get(context)
    val staffDao = db.staffDao()
    val advanceDao = db.advanceDao()

    val prefs = SecurityPreferences(context)
    val addedBy = prefs.getUserName().ifEmpty { "Admin" }
    val role = prefs.getRole().ifEmpty { "ADMIN" }
    val isAdmin = role == "ADMIN"

    var staffList by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var advanceList by remember { mutableStateOf<List<AdvanceEntity>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedStaff by remember { mutableStateOf<StaffEntity?>(null) }

    LaunchedEffect(Unit) {
        staffDao.getAllActive().collectLatest { staffList = it }
        advanceDao.getAll().collectLatest { advanceList = it }
    }

    val totalAdvance = advanceList.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Advance & Peshgi Khata", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${advanceList.size} entries", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { selectedStaff = null; showDialog = true },
                containerColor = BrandGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nayi Peshgi", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundLight
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Total card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kul Advance", fontSize = 12.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Rs. ${totalAdvance.toInt()}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("Salary se kat jayega", fontSize = 11.sp, color = Color(0xFFB91C1C))
                    }
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFDC2626), modifier = Modifier.size(44.dp))
                }
            }

            if (advanceList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = TextGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Koi advance nahi", color = TextGray)
                        Text("Neeche + dabao", fontSize = 12.sp, color = TextGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(advanceList, key = { it.id }) { adv ->
                        AdvanceCard(advance = adv, isAdmin = isAdmin, onDelete = {
                            scope.launch { advanceDao.delete(adv.id) }
                        })
                    }
                    item { CopyrightFooter() }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        AddAdvanceDialog(
            staffList = staffList,
            onDismiss = { showDialog = false },
            onSave = { staff, amount, reason, mode ->
                scope.launch {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    advanceDao.insert(
                        AdvanceEntity(
                            staffId = staff.id,
                            staffName = staff.name,
                            amount = amount,
                            date = today,
                            reason = reason,
                            mode = mode,
                            addedBy = addedBy
                        )
                    )
                    showDialog = false
                }
            }
        )
    }
}

@Composable
private fun AdvanceCard(advance: AdvanceEntity, isAdmin: Boolean, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    advance.staffName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandOrange
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(advance.staffName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("${advance.date} • ${advance.mode}", fontSize = 11.sp, color = TextGray)
                if (advance.reason.isNotEmpty())
                    Text(advance.reason, fontSize = 11.sp, color = TextGray)
            }
            Text("Rs. ${advance.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            if (isAdmin) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAdvanceDialog(
    staffList: List<StaffEntity>,
    onDismiss: () -> Unit,
    onSave: (StaffEntity, Double, String, String) -> Unit
) {
    var selected by remember { mutableStateOf<StaffEntity?>(null) }
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("CASH") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nayi Peshgi / Advance", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Staff dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selected?.name ?: "Staff select karo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Staff") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        staffList.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { selected = s; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Raqam (Rs.)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Wajah (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == "CASH", onClick = { mode = "CASH" }, label = { Text("Cash") })
                    FilterChip(selected = mode == "EASYPAISA", onClick = { mode = "EASYPAISA" }, label = { Text("Easypaisa") })
                    FilterChip(selected = mode == "RATION", onClick = { mode = "RATION" }, label = { Text("Ration") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (selected != null && amt > 0) {
                        onSave(selected!!, amt, reason.trim(), mode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
