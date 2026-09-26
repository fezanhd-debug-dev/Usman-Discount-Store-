package com.usmandiscountstore.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.Lang
import com.usmandiscountstore.app.util.PasswordHelper
import com.usmandiscountstore.app.util.SecurityPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (role: String, name: String) -> Unit,
    onSuperAdminUnlock: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("ADMIN") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Secret 11-tap counter
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.align(Alignment.TopEnd)) {
                    LanguagePickerInTopBar(tint = BrandGreen)
                }
            }

            AppLogo(size = 100.dp)

            Spacer(Modifier.height(18.dp))
            Text(Lang.t("app_name"), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(Lang.t("app_address"), fontSize = 14.sp, color = TextGray)
            Text(Lang.t("app_tagline"), fontSize = 12.sp, color = BrandGreen, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(32.dp))

            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EEF5)),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(4.dp)) {
                    Button(
                        onClick = { selectedRole = "ADMIN"; errorMsg = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "ADMIN") BrandGreen else Color.Transparent,
                            contentColor = if (selectedRole == "ADMIN") Color.White else Color(0xFF455A64)
                        ),
                        elevation = null
                    ) { Text(Lang.t("admin"), fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { selectedRole = "MODERATOR"; errorMsg = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "MODERATOR") BrandGreen else Color.Transparent,
                            contentColor = if (selectedRole == "MODERATOR") Color.White else Color(0xFF455A64)
                        ),
                        elevation = null
                    ) { Text(Lang.t("moderator"), fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it; errorMsg = null },
                label = { Text(Lang.t("username")) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = BrandGreen) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; errorMsg = null },
                label = { Text(Lang.t("password")) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = BrandGreen) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) Lang.t("hide") else Lang.t("show"),
                            tint = BrandGreen
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMsg!!, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val correct = if (selectedRole == "ADMIN")
                        PasswordHelper.getAdminPassword(context)
                    else
                        PasswordHelper.getModeratorPassword(context)

                    if (username.isBlank()) {
                        errorMsg = Lang.t("username_empty")
                    } else if (password == correct) {
                        val name = if (selectedRole == "ADMIN")
                            "Muhammad Usman Nawaz"
                        else
                            username.trim().replaceFirstChar { it.uppercase() }
                        SecurityPreferences(context).saveSession(name, selectedRole)
                        onLoginSuccess(selectedRole, name)
                    } else {
                        errorMsg = Lang.t("wrong_password")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text(Lang.t("login_button"), fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(16.dp))

            // Secret tap on copyright
            CopyrightFooter(onSecretTap = {
                val now = System.currentTimeMillis()
                // Reset counter if more than 3 seconds gap
                if (now - lastTapTime > 3000L) tapCount = 0
                lastTapTime = now
                tapCount++

                // Subtle feedback every 5 taps
                if (tapCount in listOf(3, 6, 9)) {
                    Toast.makeText(context, "${11 - tapCount} taps to unlock...", Toast.LENGTH_SHORT).show()
                }

                if (tapCount >= 11) {
                    tapCount = 0
                    Toast.makeText(context, "🔓 Super Admin unlocked", Toast.LENGTH_SHORT).show()
                    onSuperAdminUnlock()
                }
            })
        }
    }
}
