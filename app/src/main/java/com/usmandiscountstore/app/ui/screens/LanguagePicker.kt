package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.BrandGreen
import com.usmandiscountstore.app.util.Lang

/**
 * Globe icon button that opens a dropdown with 3 language options.
 * Use it inside TopAppBar actions.
 */
@Composable
fun LanguagePickerInTopBar(tint: Color = Color.White) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Language, "Language", tint = tint)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
        ) {
            Text(
                text = Lang.t("select_language"),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Divider()
            listOf(Lang.EN, Lang.UR, Lang.PA).forEach { code ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = BrandGreen
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(Lang.displayName(code), fontSize = 14.sp)
                        }
                    },
                    trailingIcon = {
                        if (Lang.current == code) {
                            Icon(Icons.Default.Check, null, tint = BrandGreen)
                        }
                    },
                    onClick = {
                        Lang.set(context, code)
                        expanded = false
                    }
                )
            }
        }
    }
}
