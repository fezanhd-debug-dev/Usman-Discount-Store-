package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.PasswordHelper
import com.usmandiscountstore.app.util.SecurityPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (role: String, name: String) -> Unit) {

    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("ADMIN") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundLight).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(20.dp), color = BrandGreenLight) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Store, null, tint = BrandGreen, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Usman Discount Store", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("Vehari Road, Old Hasilpur", fontSize = 14.sp, color = TextGray)
            Text("Staff Hazri & Khata Management", fontSize = 12.sp, color = BrandGreen, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(32.dp))

            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EEF5)),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(4.dp)) {
                    Button(
                        onClick = { selectedRole = "ADMIN"; username = "admin" },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "ADMIN") BrandGreen else Color.Transparent,
                            contentColor = if (selectedRole == "ADMIN") Color.White else Color(0xFF455A64)
                        ),
                        elevation = null
                    ) { Text("Admin", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { selectedRole = "MODERATOR"; username = "moderator" },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "MODERATOR") BrandGreen else Color.Transparent,
                            contentColor = if (selectedRole == "MODERATOR") Color.White else Color(0xFF455A64)
                        ),
                        elevation = null
                    ) { Text("Moderator", fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = BrandGreen) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = BrandGreen) },
                visualTransformation = PasswordVisualTransformation(),
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

                    if (password == correct) {
                        val name = if (selectedRole == "ADMIN") "Muhammad Usman Nawaz" else "Muzammal Shah"
                        SecurityPreferences(context).saveSession(name, selectedRole)
                        onLoginSuccess(selectedRole, name)
                    } else {
                        errorMsg = "Ghalat password"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text("Login Karein", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(12.dp))
            Text("Default: admin=admin123 | moderator=mod123",
                fontSize = 11.sp, color = TextGray)

            Spacer(Modifier.height(12.dp))
            CopyrightFooter()
        }
    }
}
