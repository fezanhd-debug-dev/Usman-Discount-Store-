package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.ui.theme.BrandGreen
import com.usmandiscountstore.app.ui.theme.BrandOrange

/**
 * Store name se initials nikalta hai.
 *
 * Examples:
 *   "Usman Discount Store"   -> "UDS"
 *   "Mera Store"             -> "MS"
 *   "Almadina Discount Store"-> "ADS"
 *   "Khan Kiryana Shop"      -> "KKS"
 *   "Almadina"               -> "ALM"
 *   "Shop"                   -> "SHO"
 */
fun generateStoreInitials(storeName: String): String {
    val cleaned = storeName.trim()
    if (cleaned.isEmpty()) return "UDS"

    val words = cleaned.split("\\s+".toRegex()).filter { it.isNotEmpty() }

    return when {
        // Sirf 1 word: pehle 3 letters
        words.size == 1 -> {
            val word = words[0]
            if (word.length >= 3) word.substring(0, 3).uppercase()
            else word.uppercase()
        }
        // 2 words: pehle word ka pehla letter + dusre word ke pehle 2
        words.size == 2 -> {
            val first = words[0].firstOrNull()?.uppercaseChar()?.toString() ?: ""
            val second = words[1].take(2).uppercase()
            "$first$second"
        }
        // 3+ words: pehle 3 words ka pehla letter
        else -> {
            words.take(3)
                .map { it.firstOrNull()?.uppercaseChar()?.toString() ?: "" }
                .joinToString("")
        }
    }
}

/**
 * Main App Logo — Store initials + Shopping Cart
 * Store name automatically Room DB se aata hai.
 */
@Composable
fun AppLogo(size: Dp = 100.dp) {
    val context = LocalContext.current
    val storeSettingsFlow = remember {
        AppDatabase.get(context).storeSettingsDao().getFlow()
    }
    val settings by storeSettingsFlow.collectAsState(initial = null)
    val storeName = settings?.storeName?.takeIf { it.isNotBlank() }
        ?: "Usman Discount Store"
    val initials = generateStoreInitials(storeName)

    // Initials ki length ke hisaab se font size adjust karein
    val fontSize = when {
        initials.length <= 2 -> (size.value / 3.5f).sp
        initials.length == 3 -> (size.value / 4f).sp
        else -> (size.value / 5f).sp
    }

    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = BrandGreen
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = initials,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Store",
                tint = BrandOrange,
                modifier = Modifier.size(size / 3)
            )
        }
    }
}

/**
 * Compact logo for TopBar (small) — sirf initials
 */
@Composable
fun AppLogoSmall(size: Dp = 40.dp) {
    val context = LocalContext.current
    val storeSettingsFlow = remember {
        AppDatabase.get(context).storeSettingsDao().getFlow()
    }
    val settings by storeSettingsFlow.collectAsState(initial = null)
    val storeName = settings?.storeName?.takeIf { it.isNotBlank() }
        ?: "Usman Discount Store"
    val initials = generateStoreInitials(storeName)

    val fontSize = when {
        initials.length <= 2 -> (size.value / 2.5f).sp
        initials.length == 3 -> (size.value / 3f).sp
        else -> (size.value / 3.5f).sp
    }

    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = initials,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = BrandGreen,
                letterSpacing = 0.5.sp
            )
        }
    }
}
