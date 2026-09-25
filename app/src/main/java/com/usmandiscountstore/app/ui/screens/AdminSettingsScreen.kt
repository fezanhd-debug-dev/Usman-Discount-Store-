package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.data.local.entity.StoreSettingsEntity
import com.usmandiscountstore.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = AppDatabase.get(context).storeSettingsDao()

    var storeName by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var radiusText by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var alertEnabled by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val s = dao.get() ?: StoreSettingsEntity()
        storeName = s.storeName
        storeAddress = s.storeAddress
        latText = s.latitude.toString()
        lonText = s.longitude.toString()
        radiusText = s.geofenceRadiusMeters.toInt().toString()
        whatsapp = s.adminWhatsapp
        alertEnabled = s.alertEnabled
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Settings", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Store aur alerts config", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
        if (!loaded) return@Scaffold

        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (savedMsg != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(savedMsg!!, Modifier.padding(12.dp), color = Color(0xFF166534), fontSize = 13.sp)
                }
            }

            SectionTitle("🏪 Store Information")
            OutlinedTextField(
                value = storeName, onValueChange = { storeName = it },
                label = { Text("Store Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = storeAddress, onValueChange = { storeAddress = it },
                label = { Text("Address") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("📍 Geofence Location")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = latText, onValueChange = { latText = it },
                    label = { Text("Latitude") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lonText, onValueChange = { lonText = it },
                    label = { Text("Longitude") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = radiusText, onValueChange = { radiusText = it.filter { c -> c.isDigit() } },
                label = { Text("Geofence Radius (meters)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Hasilpur counter se itne meter ke andar hazri lagegi") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("📱 WhatsApp Alerts")
            OutlinedTextField(
                value = whatsapp, onValueChange = { whatsapp = it.filter { c -> c.isDigit() } },
                label = { Text("Admin WhatsApp Number") },
                placeholder = { Text("923001234567") },
                supportingText = { Text("Format: 92XXXXXXXXXX (bina + ya 0 ke)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = BrandGreen) },
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hazri Alerts ON karein", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text("Har hazri pe WhatsApp alert bhejega", fontSize = 11.sp, color = TextGray)
                    }
                    Switch(
                        checked = alertEnabled, onCheckedChange = { alertEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen, checkedTrackColor = BrandGreen.copy(alpha = 0.4f))
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val s = StoreSettingsEntity(
                            id = 1,
                            storeName = storeName.trim(),
                            storeAddress = storeAddress.trim(),
                            latitude = latText.toDoubleOrNull() ?: 29.6974,
                            longitude = lonText.toDoubleOrNull() ?: 72.5518,
                            geofenceRadiusMeters = radiusText.toFloatOrNull() ?: 30f,
                            adminWhatsapp = whatsapp.trim(),
                            alertEnabled = alertEnabled
                        )
                        dao.save(s)
                        savedMsg = "✅ Settings save ho gayin"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark,
        modifier = Modifier.padding(top = 4.dp))
}
