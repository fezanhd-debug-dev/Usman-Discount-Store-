package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.util.Lang

/**
 * Copyright footer.
 * @param onSecretTap optional callback — fired on tap (used for 11-tap Super Admin unlock)
 */
@Composable
fun CopyrightFooter(onSecretTap: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(
                if (onSecretTap != null) Modifier.clickable { onSecretTap() }
                else Modifier
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            Lang.t("copyright"),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.ui.graphics.Color(0xFF9CA3AF)
        )
    }
}
