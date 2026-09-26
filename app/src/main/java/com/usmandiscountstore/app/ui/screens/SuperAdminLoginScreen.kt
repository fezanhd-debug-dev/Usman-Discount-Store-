package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.*
import com.usmandiscountstore.app.util.SuperAdminHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminLoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))
            .imePadding().verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock icon in circular badge
            Box(
                modifier = Modifier.size(96.dp).background(
                    Color(0xFF1E293B),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text("SUPER ADMIN", fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = Color.White, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            Text("Restricted Access — Mr.DHooM 4K",
                fontSize = 11.sp, color = Color(0xFF94A3B8), letterSpacing = 1.sp)

            Spacer(Modifier.height(36.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Master Password", color = Color(0xFFCBD5E1)) },
                leadingIcon = {
                    Icon(Icons.Default.Key, null, tint = Color(0xFFFBBF24))
                },
                trailingIcon = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(
                            if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFBBF24),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFBBF24)
                )
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = Color(0xFFEF4444), fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password == SuperAdminHelper.getPassword(context)) {
                        onSuccess()
                    } else {
                        error = "❌ Invalid master password"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24))
            ) {
                Icon(Icons.Default.LockOpen, null, tint = Color(0xFF0F172A))
                Spacer(Modifier.width(8.dp))
                Text("UNLOCK", color = Color(0xFF0F172A), fontWeight = FontWeight.Black,
                    fontSize = 15.sp, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to Login", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    }
}
