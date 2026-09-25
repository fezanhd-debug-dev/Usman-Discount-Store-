package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CopyrightFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "© 2026 Mr.DHooM 4K — All Rights Reserved",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.ui.graphics.Color(0xFF9CA3AF)
        )
    }
}
