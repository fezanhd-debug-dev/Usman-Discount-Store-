package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.Lang
import com.usmandiscountstore.app.util.SecurityPreferences
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateStaff: () -> Unit,
    onNavigateAttendance: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateAdvance: () -> Unit,
    onNavigateSalary: () -> Unit,
    onNavigateSalarySheet: () -> Unit,
    onNavigateLeave: () -> Unit,
    onNavigateSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs = SecurityPreferences(context)
    val userName = prefs.getUserName().ifEmpty { "Admin" }
    val role = prefs.getRole().ifEmpty { "ADMIN" }
    val isAdmin = role == "ADMIN"
    val todayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US).format(Date())

    var pendingLeaves by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        AppDatabase.get(context).leaveDao().getPendingCount()
            .collectLatest { pendingLeaves = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogoSmall(size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(Lang.t("app_name"), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(Lang.t("app_address"), fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                },
                actions = {
                    LanguagePickerInTopBar(tint = Color.White)
                    IconButton(onClick = { prefs.clearSession(); onLogout() }) {
                        Icon(Icons.Default.Logout, Lang.t("logout"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(BrandGreenLight),
                        contentAlignment = Alignment.Center) {
                        Text(userName.firstOrNull()?.toString() ?: "U",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Lang.t("welcome"), fontSize = 12.sp, color = TextGray)
                        Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = if (isAdmin) BrandGreen else BrandOrange) {
                            Text(
                                if (isAdmin) Lang.t("admin_access") else Lang.t("moderator_access"),
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            Text(todayDate, fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = 4.dp))

            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(Lang.t("geofence_active"), fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 13.sp)
                        Text(Lang.t("geofence_sub"), fontSize = 11.sp, color = BrandGreenDark)
                    }
                }
            }

            Text(Lang.t("store_modules"), fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp))

            ModuleCard(Lang.t("mod_hazri"), Lang.t("mod_hazri_sub"),
                Icons.Default.CheckCircle, BrandGreen, onNavigateAttendance)

            if (isAdmin) {
                ModuleCard(Lang.t("mod_staff_admin"), Lang.t("mod_staff_admin_sub"),
                    Icons.Default.People, Color(0xFF2563EB), onNavigateStaff)
            } else {
                ModuleCard(Lang.t("mod_staff_mod"), Lang.t("mod_staff_mod_sub"),
                    Icons.Default.People, Color(0xFF2563EB), onNavigateStaff)
            }

            ModuleCard(Lang.t("mod_history"), Lang.t("mod_history_sub"),
                Icons.Default.EventNote, Color(0xFF0EA5E9), onNavigateHistory)

            ModuleCard(Lang.t("mod_advance"), Lang.t("mod_advance_sub"),
                Icons.Default.AccountBalanceWallet, BrandOrange, onNavigateAdvance)

            ModuleCardBadged(
                Lang.t("mod_leave"),
                if (pendingLeaves > 0) "$pendingLeaves ${Lang.t("mod_leave_pending")}"
                else Lang.t("mod_leave_none"),
                Icons.Default.EventAvailable, Color(0xFFF59E0B),
                badge = if (pendingLeaves > 0) pendingLeaves else null,
                onClick = onNavigateLeave)

            if (isAdmin) {
                ModuleCard(Lang.t("mod_sheet"), Lang.t("mod_sheet_sub"),
                    Icons.Default.TableChart, Color(0xFF16A34A), onNavigateSalarySheet)
                ModuleCard(Lang.t("mod_slip"), Lang.t("mod_slip_sub"),
                    Icons.Default.ReceiptLong, Color(0xFF7C3AED), onNavigateSalary)
                ModuleCard(Lang.t("mod_settings"), Lang.t("mod_settings_sub"),
                    Icons.Default.Tune, Color(0xFF4B5563), onNavigateSettings)
            }

            CopyrightFooter()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ModuleCard(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextGray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ModuleCardBadged(
    title: String, subtitle: String, icon: ImageVector, tint: Color,
    badge: Int?, onClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                    if (badge != null && badge > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFDC2626)) {
                            Text("$badge",
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextGray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}
