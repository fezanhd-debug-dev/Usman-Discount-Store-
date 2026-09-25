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
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.data.repository.StaffRepository
import com.usmandiscountstore.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { StaffRepository(AppDatabase.get(context).staffDao()) }

    val staffList by repo.getAllActive().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StaffEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<StaffEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mulazimeen Record", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${staffList.size} active staff", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
                onClick = { editing = null; showDialog = true },
                containerColor = BrandGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, null) },
                text = { Text("Naya Staff", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundLight
    ) { pad ->
        if (staffList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PeopleOutline, null, tint = TextGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Koi staff nahi hai", fontSize = 16.sp, color = TextGray)
                    Text("Neeche + button dabao", fontSize = 13.sp, color = TextGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(staffList, key = { it.id }) { staff ->
                    StaffCard(
                        staff = staff,
                        onEdit = { editing = staff; showDialog = true },
                        onDelete = { confirmDelete = staff }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog) {
        AddEditStaffDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { entity ->
                scope.launch {
                    if (entity.id == 0L) repo.add(entity)
                    else repo.update(entity)
                    showDialog = false
                }
            }
        )
    }

    confirmDelete?.let { staff ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Staff Remove Karein?") },
            text = { Text("${staff.name} ko remove karna chahte hain?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deactivate(staff.id); confirmDelete = null }
                }) { Text("Haan", color = Color(0xFFDC2626)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Nahi") }
            }
        )
    }
}

@Composable
private fun StaffCard(staff: StaffEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val roleColor = when (staff.role) {
        "MODERATOR" -> BrandOrange
        "ADMIN" -> BrandGreen
        else -> Color(0xFF2563EB)
    }
    val roleText = when (staff.role) {
        "MODERATOR" -> "MODERATOR"
        "ADMIN" -> "ADMIN"
        else -> "STAFF"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    staff.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = roleColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(staff.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = roleColor) {
                        Text(
                            roleText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (staff.designation.isNotEmpty())
                    Text(staff.designation, fontSize = 11.sp, color = TextGray)
                Spacer(Modifier.height(2.dp))
                Text("Ujrat: Rs. ${staff.dailyWage.toInt()}/din", fontSize = 11.sp, color = BrandGreen, fontWeight = FontWeight.SemiBold)
                if (staff.phone.isNotEmpty())
                    Text(staff.phone, fontSize = 11.sp, color = TextGray)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = BrandGreen, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditStaffDialog(
    initial: StaffEntity?,
    onDismiss: () -> Unit,
    onSave: (StaffEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var designation by remember { mutableStateOf(initial?.designation ?: "") }
    var role by remember { mutableStateOf(initial?.role ?: "STAFF") }
    var wageText by remember { mutableStateOf(initial?.dailyWage?.toInt()?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Naya Staff Add Karein" else "Staff Edit Karein", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Naam *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Mobile Number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation, onValueChange = { designation = it },
                    label = { Text("Designation (Cashier etc.)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wageText, onValueChange = { wageText = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily Wage (Rs.)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Role:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == "STAFF",
                        onClick = { role = "STAFF" },
                        label = { Text("Staff") }
                    )
                    FilterChip(
                        selected = role == "MODERATOR",
                        onClick = { role = "MODERATOR" },
                        label = { Text("Moderator") }
                    )
                }
                if (error != null) Text(error!!, color = Color.Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) { error = "Naam zaroori hai"; return@Button }
                    val wage = wageText.toDoubleOrNull() ?: 0.0
                    val entity = (initial ?: StaffEntity(name = name, joinDate = today)).copy(
                        name = name.trim(),
                        phone = phone.trim(),
                        designation = designation.trim(),
                        role = role,
                        dailyWage = wage
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
