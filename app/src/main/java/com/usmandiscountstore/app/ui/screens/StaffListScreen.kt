package com.usmandiscountstore.app.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.data.local.entity.StaffEntity
import com.usmandiscountstore.app.data.repository.StaffRepository
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.FaceEmbeddingHelper
import com.usmandiscountstore.app.util.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
                        Text(Lang.t("staff_record"), fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${staffList.size} ${Lang.t("active_staff")}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Lang.t("back"), tint = Color.White)
                    }
                },
                actions = { LanguagePickerInTopBar(tint = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showDialog = true },
                containerColor = BrandGreen, contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, null) },
                text = { Text(Lang.t("new_staff"), fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundLight
    ) { pad ->
        if (staffList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PeopleOutline, null, tint = TextGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(Lang.t("no_staff"), fontSize = 16.sp, color = TextGray)
                    Text(Lang.t("tap_to_add"), fontSize = 13.sp, color = TextGray)
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
                item { CopyrightFooter() }
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
            title = { Text(Lang.t("remove_staff")) },
            text = { Text("${staff.name} — ${Lang.t("remove_staff_msg")}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deactivate(staff.id); confirmDelete = null }
                }) { Text(Lang.t("yes"), color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(Lang.t("no")) } }
        )
    }
}

@Composable
private fun StaffCard(staff: StaffEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val roleColor = when (staff.role) {
        "MODERATOR" -> BrandOrange; "ADMIN" -> BrandGreen; else -> Color(0xFF2563EB)
    }
    val hasFace = staff.faceEmbedding.isNotBlank()

    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                if (staff.photoPath.isNotEmpty() && File(staff.photoPath).exists()) {
                    AsyncImage(model = File(staff.photoPath), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Text(staff.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = roleColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(staff.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = roleColor) {
                        Text(staff.role, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (staff.designation.isNotEmpty())
                    Text(staff.designation, fontSize = 11.sp, color = TextGray)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${Lang.t("wage_label")}: Rs. ${staff.dailyWage.toInt()}/din",
                        fontSize = 11.sp, color = BrandGreen, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (hasFace) Icons.Default.CheckCircle else Icons.Default.Warning,
                        null,
                        tint = if (hasFace) BrandGreen else BrandOrange,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        if (hasFace) Lang.t("face_registered") else Lang.t("face_required"),
                        fontSize = 10.sp,
                        color = if (hasFace) BrandGreen else BrandOrange
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, Lang.t("edit"), tint = BrandGreen, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, Lang.t("delete"), tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var designation by remember { mutableStateOf(initial?.designation ?: "") }
    var role by remember { mutableStateOf(initial?.role ?: "STAFF") }
    var wageText by remember { mutableStateOf(initial?.dailyWage?.toInt()?.toString() ?: "") }
    var photoPath by remember { mutableStateOf(initial?.photoPath ?: "") }
    var faceEmbedding by remember { mutableStateOf(initial?.faceEmbedding ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    if (showCamera) {
        CameraScreen(
            title = "${Lang.t("face_capture")} — ${name.ifBlank { "New" }}",
            onPhotoCaptured = { file ->
                scope.launch {
                    processing = true
                    try {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        val emb = withContext(Dispatchers.Default) { FaceEmbeddingHelper.getEmbedding(bmp) }
                        if (emb != null) {
                            photoPath = file.absolutePath
                            faceEmbedding = FaceEmbeddingHelper.embedToString(emb)
                        } else error = Lang.t("face_error")
                    } catch (e: Exception) { error = e.message }
                    finally { processing = false; showCamera = false }
                }
            },
            onBack = { showCamera = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) Lang.t("add_staff") else Lang.t("edit_staff"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (processing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(Lang.t("processing_face"), fontSize = 12.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(BrandGreenLight),
                        contentAlignment = Alignment.Center) {
                        if (photoPath.isNotEmpty() && File(photoPath).exists()) {
                            AsyncImage(model = File(photoPath), contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Icon(Icons.Default.Person, null, tint = BrandGreen, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Button(onClick = { showCamera = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (photoPath.isEmpty()) Lang.t("face_capture") else Lang.t("change_face"), fontSize = 12.sp)
                        }
                        if (faceEmbedding.isNotBlank()) {
                            Text("✅ ${Lang.t("face_registered")}", fontSize = 10.sp, color = BrandGreen)
                        } else {
                            Text("⚠️ ${Lang.t("face_required")}", fontSize = 10.sp, color = BrandOrange)
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(Lang.t("staff_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text(Lang.t("mobile_number")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = designation, onValueChange = { designation = it },
                    label = { Text(Lang.t("designation")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wageText, onValueChange = { wageText = it.filter { c -> c.isDigit() } },
                    label = { Text(Lang.t("daily_wage")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Text("${Lang.t("role")}:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = role == "STAFF", onClick = { role = "STAFF" }, label = { Text(Lang.t("staff")) })
                    FilterChip(selected = role == "MODERATOR", onClick = { role = "MODERATOR" }, label = { Text(Lang.t("moderator")) })
                }
                if (error != null) Text(error!!, color = Color.Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) { error = Lang.t("staff_name"); return@Button }
                    if (faceEmbedding.isBlank()) { error = Lang.t("face_required"); return@Button }
                    val wage = wageText.toDoubleOrNull() ?: 0.0
                    val entity = (initial ?: StaffEntity(name = name, joinDate = today)).copy(
                        name = name.trim(), phone = phone.trim(),
                        designation = designation.trim(), role = role,
                        dailyWage = wage, photoPath = photoPath, faceEmbedding = faceEmbedding
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                enabled = !processing
            ) { Text(Lang.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Lang.t("cancel")) } }
    )
}
